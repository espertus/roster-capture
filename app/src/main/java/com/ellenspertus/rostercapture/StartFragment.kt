package com.ellenspertus.rostercapture

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ellenspertus.rostercapture.databinding.FragmentStartBinding
import kotlin.system.exitProcess
import com.ellenspertus.rostercapture.anki.AnkiBackend
import com.ellenspertus.rostercapture.anki.AnkiBackend.PermissionStatus
import com.ellenspertus.rostercapture.anki.AnkiConfigViewModel
import com.ellenspertus.rostercapture.configuration.FieldConfigViewModel
import com.ellenspertus.rostercapture.extensions.navigateSafe
import com.ellenspertus.rostercapture.extensions.navigateToFailure
import com.ellenspertus.rostercapture.instrumentation.Analytics

/**
 * An invisible fragment that verifies that the API is accessible
 * and that required permissions are granted. Depending on the result,
 * it navigates to [com.ellenspertus.rostercapture.courses.SelectCourseFragment] or [FailureFragment] or
 * exits.
 */
class StartFragment : Fragment() {
    private var _binding: FragmentStartBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainActivity: MainActivity
    private val fieldConfigViewModel: FieldConfigViewModel by activityViewModels()
    private val ankiConfigViewModel: AnkiConfigViewModel by activityViewModels()

    // This needs to be at the top level because registerForActivityResult()
    // can be called only during a Fragment's creation.
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                handlePermissionDenied()
            }
            // If it was granted, onResume() will be called and move to the next step.
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        mainActivity = (requireActivity() as MainActivity)

        // Requirement 1: AnkiDroid is installed.
        if (!isAnkiDroidInstalled()) {
            requestAnkiDroid()
            return
        }
        Analytics.logFirstTime("AnkiDroid installed")

        // Requirement 2: Fields are configured.
        if (!fieldConfigViewModel.hasConfiguration(requireContext())) {
            requestConfiguration()
            return
        }
        Analytics.logFirstTime("AnkiDroid initially configured")

        // Requirement 3: Permissions are granted.
        if (mainActivity.backend == null) {
            if (!AnkiBackend.hasPermission(this)) {
                solicitPermission()
                // When the permission request is complete,
                // onResume() will be called, and it will re-attempt.
                return
            } else {
                // Try creating backend.
                if (!createBackend()) {
                    // It navigates to the FailureFragment.
                    return
                }
                // If it succeeds, fall through to the final requirement.
            }
        }

        // Requirement 4: Model and deck are configured.
        if (!ankiConfigViewModel.isInitialized) {
            configureModelAndDeck() // which calls navigateToSelectCourseFragment()
            return
        }

        navigateToSelectCourseFragment()
    }

    private fun isAnkiDroidInstalled(): Boolean =
        AnkiBackend.isApiAvailable(mainActivity)

    // Transitions to other fragments

    private fun navigateToSelectCourseFragment() {
        Analytics.logFirstTime("startup_complete")
        findNavController().navigateSafe(
            StartFragmentDirections.actionStartFragmentToSelectCourseFragment()
        )
    }

    private fun navigateToFieldConfigFragment() {
        Analytics.log("configuration_solicitation")
        findNavController().navigateSafe(
            StartFragmentDirections.actionStartFragmentToFieldConfigFragment()
        )
    }

    // Primarily UI methods

    private fun requestAnkiDroid() {
        Analytics.log("ankidroid_request")
        binding.apply {
            tvAnki.visibility = View.VISIBLE
            buttonProceed.let {
                it.visibility = View.VISIBLE
                it.text = getString(R.string.install_ankidroid)
                it.setOnClickListener {
                    AnkiBackend.offerToInstallAnkiDroid(mainActivity)
                    exitProcess(0)
                }
            }
        }
    }

    private fun requestConfiguration() {
        binding.apply {
            tvConfigure.visibility = View.VISIBLE
            buttonProceed.let {
                it.visibility = View.VISIBLE
                it.text = getString(R.string.configure_rostercapture)
                it.setOnClickListener {
                    navigateToFieldConfigFragment()
                }
                tvConfigure.visibility = View.GONE
            }
        }
    }

    // Permissions handling

    private fun permissionGranted() =
        AnkiBackend.checkPermissionStatus(this) == PermissionStatus.GRANTED

    private fun permissionPermanentlyDenied() =
        AnkiBackend.checkPermissionStatus(this) == PermissionStatus.PERMANENTLY_DENIED

    private fun solicitPermission() {
        require(!permissionGranted())
        if (permissionPermanentlyDenied()) {
            Analytics.logFirstTime("anki_permission_permanently_denied")
            handlePermissionDenied()
        } else {
            Analytics.log("anki_permission_solicited")
            binding.apply {
                // Explain why permission is needed.
                tvPermissions.visibility = View.VISIBLE

                // Enable button to launch permission request.
                buttonProceed.visibility = View.VISIBLE
                buttonProceed.text = getString(R.string.grant_permission)
                buttonProceed.setOnClickListener {
                    // This will call onResume() when complete.
                    launchPermissionRequest()
                }
            }
        }
    }

    private fun launchPermissionRequest() {
        requestPermissionLauncher.launch(AnkiBackend.REQUIRED_PERMISSION)
        binding.buttonProceed.visibility = View.GONE
    }

    private fun handlePermissionDenied() {
        Analytics.log("anki_permission_denied")
        // At this point, we don't know whether we are allowed to ask again.
        navigateToFailure(
                getString(R.string.permissions_rejection)
        )
    }

    // This should be called only if AnkiDroid is installed and the required
    // permissions granted.
    private fun createBackend(): Boolean {
        if (!isAnkiDroidInstalled() || AnkiBackend.checkPermissionStatus(this) != PermissionStatus.GRANTED) {
            navigateToFailure("Assertion failed in createBackend()")
            return false
        }

        try {
            mainActivity.backend = AnkiBackend(mainActivity)
            Analytics.logFirstTime("anki_backend_created")
            return true
        } catch (e: AppException) {
            navigateToFailure(e)
            return false
        }
    }

    // Model and deck configuration
    private fun configureModelAndDeck() {
        // Check if it's already configured.
        if (ankiConfigViewModel.isInitialized) {
            navigateToSelectCourseFragment()
            return
        }

        initializeModel()

        // If the deck name doesn't exist yet, create it.
        require(mainActivity.backend != null)
        if (mainActivity.backend?.hasDeck(AnkiBackend.DEFAULT_DECK_NAME) == false) {
            initializeDeck(AnkiBackend.DEFAULT_DECK_NAME)
        }

        // Finally, prompt for deck name.
        promptForDeckName()
    }

    private fun initializeModel() {
        if (ankiConfigViewModel.modelId == null || ankiConfigViewModel.modelName == null) {
            mainActivity.backend?.findModelIdByName(
                AnkiBackend.DEFAULT_MODEL_NAME,
                createIfAbsent = true
            )?.let {
                ankiConfigViewModel.updateModel(it, AnkiBackend.DEFAULT_MODEL_NAME)
                Analytics.log("model_created")
            } ?: run {
                navigateToFailure("Unable to create model")
            }
        }
    }

    private fun initializeDeck(deckName: String) {
        require(ankiConfigViewModel.deckId == null && ankiConfigViewModel.deckName == null)
        require(mainActivity.backend != null)
        mainActivity.backend?.findDeckIdByName(deckName, createIfAbsent = true)?.let {
            ankiConfigViewModel.updateDeck(it, deckName)
            Analytics.log("deck_created", Bundle().apply {
                putString("deck_name", deckName)
            })
            navigateToSelectCourseFragment()
        } ?: run {
            navigateToFailure("Unable to create deck")
        }
    }

    private fun promptForDeckName() {
        binding.apply {
            tvIntro.visibility = View.GONE
            tvPermissions.visibility = View.GONE

            tvDeckExplanation.text = HtmlCompat.fromHtml(
                getString(R.string.anki_deck_name_conflict),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            tvDeckExplanation.visibility = View.VISIBLE

            tilDeck.visibility = View.VISIBLE
            etDeck.setText(AnkiBackend.DEFAULT_DECK_NAME)
            etDeck.doAfterTextChanged {
                val deckName = etDeck.text?.toString()
                buttonProceed.isEnabled = deckName?.isNotEmpty() == true
            }

            buttonProceed.visibility = View.VISIBLE
            buttonProceed.text = getString(R.string.use_deck_button)
            buttonProceed.setOnClickListener {
                initializeDeck(etDeck.text.toString().trim())
                navigateToSelectCourseFragment()
            }
        }
    }
}