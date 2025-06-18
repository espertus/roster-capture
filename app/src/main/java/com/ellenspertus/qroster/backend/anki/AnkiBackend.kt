package com.ellenspertus.qroster.backend.anki

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.ellenspertus.qroster.backend.Backend
import com.ellenspertus.qroster.model.Course

class AnkiBackend(private val context: Context): Backend {
    private val api = AnkiWrapper(context)
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
            showToast("Error accessing Anki API")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override suspend fun writeStudent(
        crn: String,
        nuid: String,
        firstName: String,
        lastName: String,
        preferredName: String?,
        pronouns: String,
        photoUri: Uri?,
        audioUri: Uri?
    ): Boolean {
        val fields: Array<String?> = Array(FIELDS.size) { "" }

        require(FIELDS[0] == NAME_FIELD)
        fields[0] = if (preferredName != null)
            "$preferredName ($firstName) $lastName"
        else
            "$firstName $lastName"

        require(FIELDS[1] == SELFIE_FIELD)
        fields[1] = photoUri?.let {
            addImageToAnki(it)
        }

        require(FIELDS[2] == AUDIO_FIELD)
        // TODO: Add audio

        // TODO: Remove duplicates

        val numAdded = api.addNote(
            modelId,
            deckId,
            fields,
            tags = setOf(crn)
        )

        if (numAdded == 0) {
            Log.e(TAG, "Failure adding notes")
            showToast("Student was NOT added")
        } else {
            Log.d(TAG, "Added student")
            showToast("Added student")
        }
        return numAdded == 1
    }

    override suspend fun retrieveCourses(): List<Course> {
        // TODO: Use real data
        val course = Course(crn = "12345", id = "6.001", name = "SICP")
        return listOf(course)
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

        private const val ADD_PERM_REQUEST = 0 // arbitrary constant

        private const val DECK_NAME = "roster" // a deck is a group of cards
        private const val MODEL_NAME = "com.ellenspertus"
        private const val NAME_FIELD = "name"
        private const val SELFIE_FIELD = "selfiePath"
        private const val AUDIO_FIELD = "audioPath"
        private const val SORT_FIELD = 0 // name
        private const val CARD_NAME = "Photo->Name"
        private val CSS: String? = null
        private const val QUESTION_FORMAT = "{{selfiePath}}"
        private const val ANSWER_FORMAT = "{{name}}"
        private val FIELDS = arrayOf(NAME_FIELD, SELFIE_FIELD, AUDIO_FIELD)
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