package com.ellenspertus.rostercapture.updates

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import com.ellenspertus.rostercapture.R

private const val MAX_RELEASE_NOTE_LENGTH = 500
private const val PREFS_NAME = "update_prefs"
private const val SKIPPED_VERSION_KEY = "skipped_version"
private const val GITHUB_URL = "https://api.github.com/repos/espertus/roster-capture-test/releases/latest"

object UpdateChecker {
    fun checkUpdate(context: Context, scope: CoroutineScope) {
        scope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    JSONObject(URL(GITHUB_URL).readText())
                }
                val latestVersion = json.getString("tag_name").removePrefix("v")
                if (shouldShowUpdate(context, latestVersion)) {
                    withContext(Dispatchers.Main) {
                        showDialog(context, latestVersion, json)
                    }
                }
            }.onFailure {
                Timber.e(it, "Update check failed")
            }
        }
    }

    private fun shouldShowUpdate(context: Context, latestVersion: String): Boolean {
        val skippedVersion = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SKIPPED_VERSION_KEY, null)

        return latestVersion != skippedVersion &&
                compareVersions(latestVersion, BuildConfig.VERSION_NAME) > 0
    }

    private fun compareVersions(v1: String, @Suppress("SameParameterValue") v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val part1 = parts1.getOrNull(i) ?: 0
            val part2 = parts2.getOrNull(i) ?: 0

            if (part1 != part2) {
                return part1.compareTo(part2)
            }
        }
        return 0
    }

    private fun getReleaseNotes(json: JSONObject) =
        try {
            json.getString("body")
                .replace(Regex("#{1,6}\\s*"), "")  // Removes # through ###### with optional spaces
                .replace(Regex("\\*{2,}"), "")     // Removes ** for bold
                .replace(Regex("\\n{3,}"), "\n\n") // Collapse multiple newlines
                .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "• ") // Lists
                .trim()
                .take(MAX_RELEASE_NOTE_LENGTH)
        } catch (e: Exception) {
            ""
        }

    private fun findDownloadUrl(json: JSONObject): String? =
        try {
            json.getJSONArray("assets")
                .let { assets ->
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        if (asset.getString("name").endsWith(".apk")) {
                            return@let asset.getString("browser_download_url")
                        }
                    }
                    Timber.e("Unable to extract download URL from JSON")
                    null
                }
        } catch (e: Exception) {
            Timber.e(e, "Exception extracting download URL")
            null
        }

    private fun showDialog(context: Context, latestVersion: String, json: JSONObject) {
        val releaseNotes = getReleaseNotes(json)
        findDownloadUrl(json)?.let { downloadUrl ->
            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.update_available))
                .setMessage(buildString {
                    append(context.getString(R.string.version_is_now_available, latestVersion))
                    append(context.getString(R.string.current_version, BuildConfig.VERSION_NAME))

                    if (releaseNotes.isNotEmpty()) {
                        append(context.getString(R.string.what_s_new))
                        append(releaseNotes)
                    }
                })
                .setPositiveButton(context.getString(R.string.download_button)) { _, _ ->
                    handleDownload(context, downloadUrl)
                }
                .setNeutralButton(context.getString(R.string.not_now_button)) { _, _ ->
                }
                .setNegativeButton(context.getString(R.string.skip_button)) { _, _ ->
                    skipVersion(context, latestVersion)
                }
                .show()
        }
    }

    private fun skipVersion(context: Context, version: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(SKIPPED_VERSION_KEY, version)
            }
        Toast.makeText(context,
            context.getString(R.string.skip_confirmation, version), Toast.LENGTH_SHORT).show()
    }

    private fun handleDownload(context: Context, url: String) {
        val uri = url.toUri()
        try {
            val fileName = "rostercapture-update-${System.currentTimeMillis()}.apk"

            val request = DownloadManager.Request(uri).apply {
                setTitle(context.getString(R.string.download_title))
                setDescription(context.getString(R.string.download_description))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            Toast.makeText(context, context.getString(R.string.download_started), Toast.LENGTH_LONG).show()

            trackDownload(context, downloadId)
        } catch (e: Exception) {
            Timber.e(e, "DownloadManager failed, falling back to browser")
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context,
                    context.getString(R.string.download_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun trackDownload(context: Context, downloadId: Long) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val uri = downloadManager.getUriForDownloadedFile(downloadId)

                    if (uri != null) {
                        // Automatically open installer
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }

                        try {
                            context.startActivity(installIntent)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to start installer")
                        }
                    }

                    context.unregisterReceiver(this)
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
    }
}
