package com.ellenspertus.rostercapture.app

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.URL
import androidx.core.content.edit
import com.ellenspertus.rostercapture.BuildConfig
import androidx.core.net.toUri
import com.ellenspertus.rostercapture.AppException
import com.ellenspertus.rostercapture.R
import com.ellenspertus.rostercapture.instrumentation.Analytics

private const val PREFS_NAME = "update_prefs"
private const val SKIPPED_VERSION_KEY = "skipped_version"
private const val DOWNLOAD_ID_KEY = "download_id"
private const val GITHUB_URL =
    "https://api.github.com/repos/espertus/roster-capture/releases/latest"
private const val DOWNLOAD_URL = "content://downloads/my_downloads"
private const val UPDATE_FILE_NAME = "app-update.apk"

object UpdateChecker {
    const val MAX_RELEASE_NOTE_LENGTH = 500

    private var downloadReceiver: BroadcastReceiver? = null

    fun checkUpdate(
        context: Context,
        scope: CoroutineScope,
        onUpdateAvailable: (Version) -> Unit = { showUpdateDialog(context, it) },
        onUpdateSkipped: (Version) -> Unit = {},
        onNoUpdate: () -> Unit = {},
        onFailure: (Throwable) -> Unit = { Timber.e(it, "Update check failed") }
    ) {
        scope.launch(Dispatchers.IO) {  // Launch the entire coroutine on IO dispatcher
            runCatching {
                val json = JSONObject(URL(GITHUB_URL).readText())
                val latestVersion = Version(json)

                withContext(Dispatchers.Main) {  // Only switch to Main for UI updates
                    when {
                        Version.current >= latestVersion -> onNoUpdate()
                        latestVersion == getSkippedVersion(context) -> onUpdateSkipped(latestVersion)
                        !latestVersion.isDownloadable -> onFailure(
                            AppException.AppInternalException(
                                "isDownloadable false for $latestVersion"
                            )
                        )

                        else -> onUpdateAvailable(latestVersion)
                    }
                }
            }.onFailure { exception ->
                withContext(Dispatchers.Main) {
                    onFailure(exception)
                }
            }
        }
    }

    private fun getSkippedVersion(context: Context): Version? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SKIPPED_VERSION_KEY, null)?.let { Version(it) }

    // This does nothing if !latestVersion.isDownloadable.
    fun showUpdateDialog(context: Context, latestVersion: Version) {
        val releaseNotes = latestVersion.releaseNotes.take(MAX_RELEASE_NOTE_LENGTH)

        latestVersion.downloadUrl?.let { downloadUrl ->
            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.update_available))
                .setMessage(buildString {
                    appendLine(context.getString(R.string.version_is_now_available, latestVersion))
                    appendLine(
                        context.getString(
                            R.string.current_version,
                            BuildConfig.VERSION_NAME
                        )
                    )
                    appendLine()

                    if (releaseNotes.isNotEmpty()) {
                        appendLine(context.getString(R.string.what_s_new))
                        append(releaseNotes)
                    }
                })
                .setPositiveButton("Update") { _, _ ->
                    handleDownload(context, downloadUrl)
                    Analytics.logFirstTime("Updating to $latestVersion")
                }
                .setNeutralButton(context.getString(R.string.not_now_button)) { _, _ ->
                    Analytics.logFirstTime("Decided not to update now to $latestVersion")
                }
                .setNegativeButton(context.getString(R.string.skip_button)) { _, _ ->
                    skipVersion(context, latestVersion)
                    Analytics.logFirstTime("Decided to skip update to $latestVersion")
                }
                .show()
        }
    }

    fun skipVersion(context: Context, version: Version) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(SKIPPED_VERSION_KEY, version.toString())
            }
        Toast.makeText(
            context,
            context.getString(R.string.skip_confirmation, version), Toast.LENGTH_SHORT
        ).show()
    }

    private var downloadObserver: ContentObserver? = null

    fun handleDownload(context: Context, url: String) {
        val uri = url.toUri()
        try {
            val request = DownloadManager.Request(uri).apply {
                setTitle(context.getString(R.string.download_title))
                setDescription(context.getString(R.string.download_description))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, UPDATE_FILE_NAME)
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putLong(DOWNLOAD_ID_KEY, downloadId)
            }

            // Register content observer as primary method
            registerDownloadObserver(context, downloadId)

            // Keep the broadcast receiver as fallback
            registerDownloadReceiver(context)

            Toast.makeText(
                context,
                context.getString(R.string.download_started),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Timber.e(e, "Download failed, opening in browser")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun registerDownloadObserver(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        downloadObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIndex != -1) {
                            val status = cursor.getInt(statusIndex)

                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                installApk(context, downloadId)
                                unregisterObserver(context)
                                unregisterReceiver(context)
                            }
                        }
                    }
                }
            }
        }

        context.contentResolver.registerContentObserver(
            DOWNLOAD_URL.toUri(),
            true,
            downloadObserver!!
        )

    }

    private fun unregisterObserver(context: Context) {
        downloadObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Timber.e(e, "Failed to unregister ContentObserver")
            }
            downloadObserver = null
        }
    }

    private fun registerDownloadReceiver(context: Context) {
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                val savedDownloadId = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getLong(DOWNLOAD_ID_KEY, -1)
                if (downloadId == savedDownloadId) {
                    installApk(context, downloadId)
                    unregisterReceiver(context)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.applicationContext.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            context.applicationContext.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    // Update the unregister function to also clean up the observer
    fun unregisterReceiver(context: Context) {
        downloadReceiver?.let {
            try {
                context.applicationContext.unregisterReceiver(it)
            } catch (e: Exception) {
                Timber.e(e, "Failed to unregister receiver")
            }
            downloadReceiver = null
        }

        // Also unregister the observer if it exists
        unregisterObserver(context)
    }

    private fun installApk(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        try {
            // Try to get the URI directly from DownloadManager
            val uri = downloadManager.getUriForDownloadedFile(downloadId)
            if (uri != null) {
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(installIntent)
                return
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to install using DownloadManager URI")
        }

        // Fallback: Query for the file location
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val downloadStatusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (downloadStatusIndex == -1) {
                    Timber.e("COLUMN_STATUS not found in DownloadManager query")
                    return
                }
                val status = cursor.getInt(downloadStatusIndex)
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    if (localUriIndex == -1) {
                        Timber.e("COLUMN_LOCAL_URI not found in DownloadManager query")
                        return
                    }
                    val localUri = cursor.getString(localUriIndex)
                    if (localUri != null) {
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                localUri.toUri(),
                                "application/vnd.android.package-archive"
                            )
                            flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(installIntent)
                    } else {
                        Timber.e("localUri is null for download $downloadId")
                    }
                } else {
                    Timber.e("Download not successful. Status: $status")
                }
            } else {
                Timber.e("No download found with ID: $downloadId")
            }
        }
    }
}
