package com.ellenspertus.rostercapture.anki

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ellenspertus.rostercapture.AppException
import com.ellenspertus.rostercapture.MainActivity
import com.ellenspertus.rostercapture.R
import com.ellenspertus.rostercapture.databinding.FragmentAnkiConfigBinding
import com.ellenspertus.rostercapture.extensions.containsIgnoreCase
import com.ellenspertus.rostercapture.extensions.navigateSafe
import com.ellenspertus.rostercapture.extensions.navigateToFailure
import kotlin.getValue

/**
 * Fragment that ensures that AnkiDroid model and deck exist.
 */
class AnkiConfigFragment : Fragment() {
    private var _binding: FragmentAnkiConfigBinding? = null
    private val binding get() = _binding!!

    private val ankiConfigViewModel: AnkiConfigViewModel by activityViewModels()
    private var _backend: AnkiBackend? = null // initialized in onAttach()
    private val backend
        get() = _backend!!

    override fun onAttach(context: Context) {
        super.onAttach(context)

        _backend = (requireActivity() as MainActivity).backend

        // Verify that Anki has not been initialized
        if (ankiConfigViewModel.isInitialized) {
            navigateToFailure(AppException.AppInternalException("AppConfigViewModel initialized in AnkiConfigFragment"))
        }

        initializeModel()

        if (!backend.existingDeckNames.containsIgnoreCase(AnkiBackend.DEFAULT_DECK_NAME)) {
            initializeDeck(AnkiBackend.DEFAULT_DECK_NAME)
            navigateToStartFragment()
            return
        }
    }

    private fun navigateToStartFragment() {
        findNavController().navigateSafe(
            AnkiConfigFragmentDirections.actionAnkiConfigFragmentToStartFragment()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnkiConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ankiConfigViewModel.apply {
            // The model has been initialized.
            require(modelId != null && modelName != null)

            // The deck has not been initialized.
            require(deckId == null && deckName == null)
        }
        binding.instructions.text =
            HtmlCompat.fromHtml(
                getString(R.string.anki_deck_name_conflict),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

        binding.apply {
            etDeck.setText(AnkiBackend.DEFAULT_DECK_NAME)
            etDeck.doAfterTextChanged {
                updateSaveButton()
            }
            btnSave.setOnClickListener {
                initializeDeck(etDeck.text.toString().trim())
                navigateToStartFragment()
            }
        }
    }

    private fun initializeModel() {
        if (ankiConfigViewModel.modelId == null || ankiConfigViewModel.modelName == null) {
            backend.findModelIdByName(AnkiBackend.DEFAULT_MODEL_NAME, createIfAbsent = true)?.let {
                ankiConfigViewModel.updateModel(it, AnkiBackend.DEFAULT_MODEL_NAME)
            } ?: run {
                navigateToFailure(AppException.AppInternalException("Unable to create model"))
            }
        }
    }

    private fun initializeDeck(deckName: String) {
        require(ankiConfigViewModel.deckId == null && ankiConfigViewModel.deckName == null)
        backend.findDeckIdByName(deckName, createIfAbsent = true)?.let {
            ankiConfigViewModel.updateDeck(it, deckName)
        } ?: run {
            navigateToFailure("Unable to create deck")
        }
    }

    private fun updateSaveButton() {
        binding.apply {
            val deckName = etDeck.text?.toString()
            btnSave.isEnabled = deckName?.isNotEmpty() == true
        }
    }

    companion object {
        private const val TAG = "AnkiConfigFragment"
    }
}