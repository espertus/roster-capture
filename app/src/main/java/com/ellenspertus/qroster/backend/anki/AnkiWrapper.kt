package com.ellenspertus.qroster.backend.anki

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.ichi2.anki.FlashCardsContract.READ_WRITE_PERMISSION
import com.ichi2.anki.api.AddContentApi

class AnkiWrapper(
    context: Context,
    private val appContext: Context = context.applicationContext,
    private val api: AddContentApi = AddContentApi(appContext)
) {
    data class Model(
        val name: String,
        val fields: Array<String>,
        val cardNames: Array<String>,
        val questionFormats: Array<String>,
        val answerFormats: Array<String>,
        val css: String?,   // null for no css
        var deckId: Long?,  // null for default deck
        val sortField: Int?,
    )

    fun isApiAvailable(context: Context) = AddContentApi.getAnkiDroidPackageName(context) != null

    fun shouldRequestPermission() =
        appContext.checkSelfPermission(READ_WRITE_PERMISSION) != PackageManager.PERMISSION_GRANTED

    fun requestPermission(callbackActivity: Activity, callbackCode: Int) {
        callbackActivity.requestPermissions(
            arrayOf(AddContentApi.READ_WRITE_PERMISSION),
            callbackCode
        )
    }

    fun findModelId(model: Model, createIfAbsent: Boolean): Long? =
        api.getModelList().entries.firstOrNull {
            it.value == model.name
        }?.key ?: if (createIfAbsent) {
            api.addNewCustomModel(
                model.name,
                model.fields,
                model.cardNames,
                model.questionFormats,
                model.answerFormats,
                model.css,
                model.deckId,
                model.sortField
            )
        } else {
            null
        }

    fun findDeckIdByName(deckName: String, createIfAbsent: Boolean): Long? =
        api.deckList?.let {
            it.entries.firstOrNull {
                it.value.equals(deckName, ignoreCase = true)
            }?.key
        } ?: if (createIfAbsent) {
            api.addNewDeck(deckName)
        } else {
            null
        }

    fun addNote(
        modelId: Long,
        deckId: Long,
        fields: Array<String?>,
        tags: Set<String>
    ) =
        api.addNotes(modelId, deckId, listOf(fields), listOf(tags))

    fun grantReadPermission(uri: Uri) {
        appContext.grantUriPermission(PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun revokeReadPermission(uri: Uri) {
        appContext.revokeUriPermission(PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun addMediaToAnki(uri: Uri, mimeType: String): String? {
        val fileName = System.currentTimeMillis().toString() + ".jpg"

        // See if this works
        val shareableUri: Uri = uri //prepareUriForSharing(imageUri)

        // Grant AnkiDroid temporary read permission
        grantReadPermission(shareableUri)
        try {
            api.addMediaFromUri(shareableUri, fileName, mimeType)?.let {
                Log.d(TAG, "Successfully added media: $it")
                return it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add media", e)
            return null
        } finally {
            revokeReadPermission(shareableUri)
        }
        Log.e(TAG, "addMediaFromUri() returned null")
        return null
    }

    companion object {
        const val TAG = "AnkiWrapper"

        const val PACKAGE = "com.ichi2.anki"
    }
}