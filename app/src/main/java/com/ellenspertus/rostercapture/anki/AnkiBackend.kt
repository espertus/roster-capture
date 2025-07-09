package com.ellenspertus.rostercapture.anki

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.ellenspertus.rostercapture.AppException.AppInternalException
import com.ellenspertus.rostercapture.MainActivity
import com.ellenspertus.rostercapture.extensions.containsIgnoreCase
import com.ellenspertus.rostercapture.extensions.equalsIgnoreCase
import com.ichi2.anki.FlashCardsContract.READ_WRITE_PERMISSION
import com.ichi2.anki.api.AddContentApi
import timber.log.Timber

/**
 * An encapsulation of Anki integration.
 *
 * @throws [AppInternalException] if the model and deck can't be retrieved or created
 */
class AnkiBackend(
    private val mainActivity: MainActivity
) {
    private val api: AddContentApi = AddContentApi(mainActivity)

    val existingModelNames = api.modelList.values.toMutableList()
    val existingDeckNames = api.deckList.values.toMutableList()

    fun hasModel(modelName: String) = existingModelNames.containsIgnoreCase(modelName)

    fun hasDeck(deckName: String) = existingDeckNames.containsIgnoreCase(deckName)

    fun findModelIdByName(modelName: String, createIfAbsent: Boolean): Long? =
        when {
            hasModel(modelName) -> api.modelList.entries.find {
                it.value.equalsIgnoreCase(
                    modelName
                )
            }?.key

            createIfAbsent -> createModel(modelName)
            else -> null
        }

    fun createModel(modelName: String): Long {
        existingModelNames.add(modelName)
        return api.addNewCustomModel(
            modelName,
            FIELDS,
            CARD_NAMES,
            QUESTION_FORMATS,
            ANSWER_FORMATS,
            CSS,
            null, // default deckId (not needed)
            SORT_FIELD
        )
    }

    fun findDeckIdByName(deckName: String, createIfAbsent: Boolean): Long? =
        when {
            hasDeck(deckName) -> api.deckList.entries.find {
                it.value.toString().equalsIgnoreCase(deckName)
            }?.key

            createIfAbsent -> createDeck(deckName)
            else -> null
        }

    fun createDeck(deckName: String): Long =
        api.addNewDeck(deckName).also {
            existingDeckNames.add(deckName)
            it
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
        modelId: Long,
        deckId: Long,
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
                    throw AppInternalException("Illegal field name ${FIELDS[i]}")
                }
            }
        }

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

    private fun addMediaToAnki(uri: Uri, mimeType: String): String? {
        val fileName = System.currentTimeMillis().toString()

        val shareableUri: Uri = uri

        // Grant AnkiDroid temporary read permission.
        grantReadPermission(shareableUri)
        try {
            api.addMediaFromUri(shareableUri, fileName, mimeType)?.let {
                return it
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to add media")
            return null
        } finally {
            // Revoke read permission.
            revokeReadPermission(shareableUri)
        }
        Timber.e("addMediaFromUri() returned null")
        return null
    }

    companion object {
        internal const val DEFAULT_DECK_NAME = "Roster"
        internal const val DEFAULT_MODEL_NAME = "com.ellenspertus.rostercapture"
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

        const val PACKAGE = "com.ichi2.anki"
        private const val ANKI_WEB_URL =
            "https://play.google.com/store/apps/details?id=com.ichi2.anki"
        private const val ANKI_MARKET_URL = "market://details?id=com.ichi2.anki"
        const val REQUIRED_PERMISSION = READ_WRITE_PERMISSION

        /**
         * Tests whether the API is available (i.e., if AnkiDroid is installed).
         */
        fun isApiAvailable(context: Context) =
            AddContentApi.getAnkiDroidPackageName(context) != null

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
                fragment.requireContext()
                    .checkSelfPermission(READ_WRITE_PERMISSION) == PackageManager.PERMISSION_GRANTED -> {
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

        fun hasPermission(fragment: Fragment) =
            checkPermissionStatus(fragment) ==
                    PermissionStatus.GRANTED
    }

    enum class PermissionStatus {
        GRANTED,
        DENIED_CAN_ASK_AGAIN,
        UNKNOWN_TRY_REQUEST,
        PERMANENTLY_DENIED
    }
}