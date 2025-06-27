package com.ellenspertus.rostercapture.backend

import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.ellenspertus.rostercapture.AppException.AppInternalException
import com.ellenspertus.rostercapture.MainActivity

/**
 * An encapsulation of the AnkiDroid model and data communicated through it.
 *
 * @see [AnkiWrapper]
 * @throws [AppInternalException] if the model and deck can't be retrieved or created
 */
class AnkiBackend(private val mainActivity: MainActivity) {
    private val api = AnkiWrapper(mainActivity)
    private var modelId = 0L
    private var deckId = 0L

    init {
        api.findModelId(MODEL, true)?.let {
            modelId = it
        } ?: Log.e(TAG, "Unable to retrieve modelId")
        api.findDeckIdByName(DECK_NAME, true)?.let {
            deckId = it
        } ?: Log.e(TAG, "Unable to retrieve deckId")
        if (modelId == 0L || deckId == 0L) {
            throw AppInternalException("Error accessing Anki API.")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(mainActivity, message, Toast.LENGTH_LONG).show()
    }

    fun writeStudent(
        crn: String,
        nuid: String,
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
                NAME_FIELD -> if (preferredName != null)
                    "$firstName ($preferredName) $lastName"
                else
                    "$firstName $lastName"

                SELFIE_FIELD -> photoUri?.let { addImageToAnki(it) }
                AUDIO_FIELD -> audioUri?.let { addAudioToAnki(it) } ?: ""
                ID_FIELD -> nuid ?: ""
                PRONOUN_FIELD -> pronouns ?: ""
                else -> run {
                    Log.e(TAG, "Illegal field name ${FIELDS[i]}")
                    showToast("Internal Error")
                    return false
                }
            }
        }

        // TODO: Remove duplicates
        val numAdded = api.addNote(
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

    private fun addMediaToAnki(uri: Uri, mimeType: String): String? {
        api.addMediaToAnki(uri, mimeType).let {
            if (it == null) {
                showToast("Unable to add file to Anki")
            }
            return it
        }
    }

    companion object {
        private const val TAG = "AnkiWrapper"

        private const val DECK_NAME = "Roster"
        private const val MODEL_NAME = "com.ellenspertus.roster"
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

        private val MODEL = AnkiWrapper.Model(
            name = MODEL_NAME,
            fields = FIELDS,
            cardNames = CARD_NAMES,
            questionFormats = QUESTION_FORMATS,
            answerFormats = ANSWER_FORMATS,
            css = CSS,
            deckId = null,
            sortField = SORT_FIELD
        )
    }
}