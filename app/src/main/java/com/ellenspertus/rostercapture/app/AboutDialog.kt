package com.ellenspertus.rostercapture.app

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.ellenspertus.rostercapture.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope

object AboutDialog {
    fun showAboutDialog(context: Context, scope: CoroutineScope) {
        UpdateChecker.checkUpdate(
            context,
            scope,
            onUpdateAvailable = { latestVersion ->
                show(context, latestVersion)
            },
            onUpdateSkipped = { latestVersion ->
                show(context, latestVersion)
            },
            onNoUpdate = {
                show(context, Version.current)
            }
        )
    }

    private fun show(context: Context, latestVersion: Version) {
        val dialogBuilder = MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.about_title))
            .setIcon(R.mipmap.ic_launcher)

        val message = StringBuilder(
            context.getString(R.string.about_message, Version.current)
        )

        if (latestVersion != Version.current && latestVersion.isDownloadable) {
            message.append(context.getString(R.string.version_is_available, latestVersion))
            dialogBuilder.setPositiveButton(context.getString(R.string.see_update_info_button)) { _, _ ->
                UpdateChecker.showUpdateDialog(context, latestVersion)
            }
            dialogBuilder.setNegativeButton(context.getString(R.string.ok_button), null)
        } else {
            message.append(context.getString(R.string.you_have_the_latest_version))
            dialogBuilder.setPositiveButton(context.getString(R.string.ok_button), null)
        }
        dialogBuilder.setMessage(HtmlCompat.fromHtml(message.toString(), HtmlCompat.FROM_HTML_MODE_COMPACT))

        dialogBuilder.create().apply {
            findViewById<TextView>(android.R.id.message)?.apply {
                movementMethod = LinkMovementMethod.getInstance()
            }
            show()
        }
    }
}