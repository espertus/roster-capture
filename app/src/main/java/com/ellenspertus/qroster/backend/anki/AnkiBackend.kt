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
        api.findModelId(ROSTER_MODEL, true)?.let {
            modelId = it
        } ?: Log.e(TAG, "Unable to retrieve modelId")
        api.findDeckIdByName(ROSTER_DECK_NAME, true)?.let {
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
        val fields: Array<String?> = Array(ROSTER_FIELDS.size) { "" }

        require(ROSTER_FIELDS[0] == ROSTER_NAME_FIELD)
        fields[0] = if (preferredName != null)
            "$preferredName ($firstName) $lastName"
        else
            "$firstName $lastName"

        require(ROSTER_FIELDS[1] == ROSTER_SELFIE_FIELD)
        fields[1] = photoUri?.let {
            addImageToAnki(it)
        }

        require(ROSTER_FIELDS[2] == ROSTER_AUDIO_FIELD)
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

        private const val ROSTER_DECK_NAME = "Roster"
        private const val ROSTER_MODEL_NAME = "com.ellenspertus.roster"
        private const val ROSTER_NAME_FIELD = "name"
        private const val ROSTER_SELFIE_FIELD = "selfiePath"
        private const val ROSTER_AUDIO_FIELD = "audioPath"
        private const val ROSTER_SORT_FIELD = 0 // name
        private const val ROSTER_CARD_NAME = "Photo->Name"
        private val CSS: String? = null
        private const val ROSTER_QUESTION_FORMAT = "{{selfiePath}}"
        private const val ROSTER_ANSWER_FORMAT = "{{name}}"
        private val ROSTER_FIELDS = arrayOf(ROSTER_NAME_FIELD, ROSTER_SELFIE_FIELD, ROSTER_AUDIO_FIELD)
        private val ROSTER_CARD_NAMES = arrayOf(ROSTER_CARD_NAME)
        private val ROSTER_QUESTION_FORMATS = arrayOf(ROSTER_QUESTION_FORMAT)
        private val ROSTER_ANSWER_FORMATS = arrayOf(ROSTER_ANSWER_FORMAT)

        private val ROSTER_MODEL = AnkiWrapper.Model(
            name = ROSTER_MODEL_NAME,
            fields = ROSTER_FIELDS,
            cardNames = ROSTER_CARD_NAMES,
            questionFormats = ROSTER_QUESTION_FORMATS,
            answerFormats = ROSTER_ANSWER_FORMATS,
            css = CSS,
            deckId = null,
            sortField = ROSTER_SORT_FIELD
        )
    }
}