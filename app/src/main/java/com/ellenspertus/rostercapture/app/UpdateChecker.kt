package com.ellenspertus.rostercapture.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
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
private const val GITHUB_URL =
    "https://api.github.com/repos/espertus/roster-capture/releases/latest"

object UpdateChecker {
    const val MAX_RELEASE_NOTE_LENGTH = 500

    fun checkUpdate(
        context: Context,
        scope: CoroutineScope,
        onUpdateAvailable: (Version) -> Unit = { showUpdateDialog(context, it) },
        onUpdateSkipped: (Version) -> Unit = {},
        onNoUpdate: () -> Unit = {},
        onFailure: (Throwable) -> Unit = { Timber.e(it, "Update check failed") }
    ) {
        scope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    JSONObject(URL(GITHUB_URL).readText())
                }
                val latestVersion = Version(json)
                withContext(Dispatchers.Main) {
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
            }.onFailure {
                onFailure(it)
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

    fun handleDownload(context: Context, url: String) {
        val uri = url.toUri()
        try {
            val request = DownloadManager.Request(uri).apply {
                setTitle(context.getString(R.string.download_title))
                setDescription(context.getString(R.string.download_description))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "app-update.apk")
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

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
}
