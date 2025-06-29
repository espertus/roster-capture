package com.ellenspertus.rostercapture.backend

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.ellenspertus.rostercapture.AppException.AppInternalException
import com.ellenspertus.rostercapture.MainActivity
import com.ichi2.anki.FlashCardsContract.READ_WRITE_PERMISSION
import com.ichi2.anki.api.AddContentApi

/**
 * An encapsulation of Anki integration.
 *
 * @throws [AppInternalException] if the model and deck can't be retrieved or created
 */
class AnkiBackend(private val mainActivity: MainActivity) {
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

    private val api: AddContentApi = AddContentApi(mainActivity)
    private var modelId = 0L
    private var deckId = 0L

    init {
        findModelId(MODEL, true)?.let {
            modelId = it
        } ?: Log.e(TAG, "Unable to retrieve modelId")
        findDeckIdByName(DECK_NAME, true)?.let {
            deckId = it
        } ?: Log.e(TAG, "Unable to retrieve deckId")
        if (modelId == 0L || deckId == 0L) {
            throw AppInternalException("Error accessing Anki API.")
        }
    }


    fun findModelId(model: Model, createIfAbsent: Boolean): Long? =
        api.modelList.entries.firstOrNull {
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


    fun hasModel(name: String) =
        api.modelList.entries.any { it.value == name }

    fun hasDeck(name: String) =
        api.deckList.entries.any { it.value.equals(name, ignoreCase = true) }


    private fun showToast(message: String) {
        Toast.makeText(mainActivity, message, Toast.LENGTH_LONG).show()
    }


    fun addNote(
        modelId: Long,
        deckId: Long,
        fields: Array<String?>,
        tags: Set<String>
    ) =
        api.addNotes(modelId, deckId, listOf(fields), listOf(tags))

    fun grantReadPermission(uri: Uri) {
        mainActivity.grantUriPermission(PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun revokeReadPermission(uri: Uri) {
        mainActivity.revokeUriPermission(PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun writeStudent(
        crn: String,
        studentId: String?,
        firstName: String,
        lastName: String,
        preferredName: String?,
        pronouns: String?,
        photoUri: Uri?,
        audioUri: Uri?
    ): Boolean {
        val fields: Array<String?> = Array(FIELDS.size) { "" }

        for (i in fields.indices) {
            fields[i] = when (FIELDS[i]) {
                NAME_FIELD -> if (preferredName != null && preferredName != firstName)
                    "$firstName ($preferredName) $lastName"
                else
                    "$firstName $lastName"

                SELFIE_FIELD -> photoUri?.let { addImageToAnki(it) }
                AUDIO_FIELD -> audioUri?.let { addAudioToAnki(it) } ?: ""
                ID_FIELD -> studentId ?: ""
                PRONOUN_FIELD -> pronouns ?: ""
                else -> run {
                    Log.e(TAG, "Illegal field name ${FIELDS[i]}")
                    showToast("Internal Error")
                    return false
                }
            }
        }

        // TODO: Remove duplicates
        val numAdded = addNote(
            modelId,
            deckId,
            fields,
            tags = setOf(crn)
        )
        return numAdded == 1
    }

    fun addImageToAnki(uri: Uri): String? {
        return addMediaToAnki(uri, "image")
    }

    fun addAudioToAnki(uri: Uri): String? {
        return addMediaToAnki(uri, "audio")
    }

    fun addMediaToAnki(uri: Uri, mimeType: String): String? {
        val fileName = System.currentTimeMillis().toString()

        val shareableUri: Uri = uri

        // Grant AnkiDroid temporary read permission
        // TODO: Show toast on error
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
        private const val TAG = "AnkiBackend"

        private const val DECK_NAME = "Roster"
        private const val MODEL_NAME = "rostercapture"
        private const val NAME_FIELD = "name"
        private const val SELFIE_FIELD = "selfiePath"
        private const val AUDIO_FIELD = "audioPath"
        private const val ID_FIELD = "id"
        private const val PRONOUN_FIELD = "pronouns"
        private const val SORT_FIELD = 0 // name
        private const val CARD_NAME = "Photo->Name"
        private val CSS: String? = null
        private const val QUESTION_FORMAT = "{{$SELFIE_FIELD}}"
        private const val ANSWER_FORMAT =
            "{{$NAME_FIELD}} {{$AUDIO_FIELD}}<br/>{{$PRONOUN_FIELD}}<br/>{{$ID_FIELD}}"
        private val FIELDS = arrayOf(NAME_FIELD, SELFIE_FIELD, AUDIO_FIELD, ID_FIELD, PRONOUN_FIELD)
        private val CARD_NAMES = arrayOf(CARD_NAME)
        private val QUESTION_FORMATS = arrayOf(QUESTION_FORMAT)
        private val ANSWER_FORMATS = arrayOf(ANSWER_FORMAT)

        private val MODEL = Model(
            name = MODEL_NAME,
            fields = FIELDS,
            cardNames = CARD_NAMES,
            questionFormats = QUESTION_FORMATS,
            answerFormats = ANSWER_FORMATS,
            css = CSS,
            deckId = null,
            sortField = SORT_FIELD
        )

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
            } catch (_: ActivityNotFoundException) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = ANKI_WEB_URL.toUri()
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