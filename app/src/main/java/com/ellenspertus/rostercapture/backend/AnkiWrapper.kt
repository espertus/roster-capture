package com.ellenspertus.rostercapture.backend

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.ichi2.anki.FlashCardsContract.READ_WRITE_PERMISSION
import com.ichi2.anki.api.AddContentApi

/**
 * The interface between the rest of this app and [AddContentApi].
 *
 * @see [AnkiBackend]
 */
class AnkiWrapper(
    private val context: Context,
    private val api: AddContentApi = AddContentApi(context)
) {
    enum class PermissionStatus {
        GRANTED,
        DENIED_CAN_ASK_AGAIN,
        UNKNOWN_TRY_REQUEST,
        PERMANENTLY_DENIED
    }

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
        context.grantUriPermission(PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun revokeReadPermission(uri: Uri) {
        context.revokeUriPermission(PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun addMediaToAnki(uri: Uri, mimeType: String): String? {
        val fileName = System.currentTimeMillis().toString()

        val shareableUri: Uri = uri

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
        private const val ANKI_WEB_URL =
            "https://play.google.com/store/apps/details?id=com.ichi2.anki"
        private const val ANKI_MARKET_URL = "market://details?id=com.ichi2.anki"
        const val REQUIRED_PERMISSION = READ_WRITE_PERMISSION

        /**
         * Tests whether the API is available (i.e., if AnkiDroid is installed).
         */
        fun isApiAvailable(context: Context) = AddContentApi.getAnkiDroidPackageName(context) != null

        /**
         * Opens the Google Play Store or a web page where the user can choose to
         * install AnkiDroid.
         */
        fun offerToInstallAnkiDroid(activity: Activity) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = ANKI_MARKET_URL.toUri()
                    setPackage("com.android.vending")
                }
                activity.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(ANKI_WEB_URL)
                }
                activity.startActivity(intent)
            }
        }

        fun checkPermissionStatus(fragment: Fragment): PermissionStatus {
            return when {
                fragment.requireContext().checkSelfPermission(READ_WRITE_PERMISSION) == PackageManager.PERMISSION_GRANTED -> {
                    PermissionStatus.GRANTED
                }

                fragment.shouldShowRequestPermissionRationale(READ_WRITE_PERMISSION) -> {
                    PermissionStatus.DENIED_CAN_ASK_AGAIN
                }

                else -> {
                    // Either first time or permanently denied
                    PermissionStatus.UNKNOWN_TRY_REQUEST
                }
            }
        }
    }
}