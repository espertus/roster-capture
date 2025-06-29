package com.ellenspertus.rostercapture.anki

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel

class AnkiConfigViewModel(private val application: Application) : AndroidViewModel(application) {
    var deckId: Long? = null
        private set
    var deckName: String? = null
        private set
    var modelId: Long? = null
        private set
    var modelName: String? = null
        private set
    val isInitialized
        get() = deckId != null && deckName != null && modelId != null && modelName != null

    init {
        loadConfiguration()
    }

    private fun loadConfiguration() {
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).apply {
            deckId = getLong(DECK_ID_KEY, 0L).takeIf { it != 0L }
            deckName = getString(DECK_NAME_KEY, null)
            modelId = getLong(MODEL_ID_KEY, 0L).takeIf { it != 0L }
            modelName = getString(MODEL_NAME_KEY, null)
        }
    }

    fun updateModel(modelId: Long, modelName: String) {
        this.modelId = modelId
        this.modelName = modelName
        saveConfiguration()
    }

    fun updateDeck(deckId: Long, deckName: String) {
        this.deckId = deckId
        this.deckName = deckName
        saveConfiguration()
    }

    private fun saveConfiguration() {
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            deckId?.let { putLong(DECK_ID_KEY, it) }
            deckName?.let { putString(DECK_NAME_KEY, it) }
            modelId?.let { putLong(MODEL_ID_KEY, it) }
            modelName?.let { putString(MODEL_NAME_KEY, it) }
        }
    }

    companion object {
        private const val PREFS_NAME = "anki_config"
        private const val DECK_ID_KEY = "deck_id"
        private const val DECK_NAME_KEY = "deck_name"
        private const val MODEL_ID_KEY = "model_id"
        private const val MODEL_NAME_KEY = "model_name"
    }
}