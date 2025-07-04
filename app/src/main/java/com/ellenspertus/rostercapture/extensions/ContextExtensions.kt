package com.ellenspertus.rostercapture.extensions

import android.app.AlertDialog
import android.content.Context

fun Context.promptForConfirmation(
    message: String,
    positiveAction: () -> Unit,
    negativeAction: () -> Unit = { }
) {
    AlertDialog.Builder(this)
        .setMessage(message)
        .setPositiveButton("Yes") { _, _ -> positiveAction() }
        .setNegativeButton("No") { dialog, _ ->
            negativeAction()
            dialog.dismiss()
        }
        .show()
}

fun Context.promptForConfirmation(
    message: String,
    positiveAction: () -> Unit
) {
    promptForConfirmation(message, positiveAction) {}
}