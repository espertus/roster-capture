package com.ellenspertus.rostercapture.extensions

import android.app.AlertDialog
import android.content.Context

fun Context.promptForConfirmation(message: String, action: () -> Unit) {
    AlertDialog.Builder(this)
        .setMessage(message)
        .setPositiveButton("Yes") { _, _ -> action() }
        .setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}