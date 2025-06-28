package com.ellenspertus.rostercapture.students

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ellenspertus.rostercapture.MainActivity
import com.ellenspertus.rostercapture.R
import com.ellenspertus.rostercapture.configuration.FieldConfigViewModel
import com.ellenspertus.rostercapture.configuration.FieldStatus
import com.ellenspertus.rostercapture.configuration.StudentField
import com.ellenspertus.rostercapture.databinding.FragmentAddStudentBinding
import com.ellenspertus.rostercapture.extensions.hasText
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Locale

class AddStudentFragment() : Fragment() {
    private val args: AddStudentFragmentArgs by navArgs()
    private lateinit var crn: String

    private var _binding: FragmentAddStudentBinding? = null
    private val binding get() = _binding!!

    private val fieldConfigViewModel: FieldConfigViewModel by activityViewModels()

    private var isLocked = false

    // If the fragment is not locked, we should offer to lock it only if
    // the fragment is reached through the navigation graph, not if we
    // return to the fragment from taking a picture, for example.
    private var shouldOfferLocking = true
    val navListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        if (destination.id == R.id.addStudentFragment) {
            shouldOfferLocking = true
        }
    }

    data class Requirement(val name: String, val check: () -> Boolean)

    // initialized when view is created
    private var requirements: MutableList<Requirement> = mutableListOf()

    // Media
    private var photoManager: PhotoManager? = null
    private var audioManager: AudioManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        crn = args.crn
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle locking.
        isLocked = false
        findNavController().addOnDestinationChangedListener(navListener)

        // Initialize view.
        requirements = mutableListOf()
        fieldConfigViewModel.loadConfiguration(requireContext())
        setupPhotoSection()
        setupAudioSection()
        setupFormSection()
        setupActionButtons()
    }

    override fun onResume() {
        super.onResume()

        if (!isLocked && shouldOfferLocking) {
            offerToLock()
            shouldOfferLocking = false
        }
    }

    private fun setupPhotoSection() {
        // Photos are always required.
        requirements.add(Requirement(fieldConfigViewModel.getSelfieField().displayName) { photoManager?.photoUri != null })
        binding.btnTakePhoto.text = fieldConfigViewModel.getSelfieField().displayName

        photoManager = PhotoManager(
            fragment = this,
            onPhotoCapture = ::displayCapturedPhoto,
            onPermissionDenied = ::showPermissionDeniedMessage
        )

        binding.photoContainer.setOnClickListener {
            photoManager?.checkPermissionAndLaunch()
        }

        binding.btnTakePhoto.setOnClickListener {
            photoManager?.checkPermissionAndLaunch()
        }

        // not initially shown
        binding.btnRetakePhoto.setOnClickListener {
            photoManager?.checkPermissionAndLaunch()
        }
    }

    private fun setupAudioSection() {
        val field = fieldConfigViewModel.getRecordingField()
        if (field.status == FieldStatus.NOT_SOLICITED) {
            binding.recordingSection.visibility = View.GONE
            return
        }

        audioManager = AudioManager(
            fragment = this,
            onUpdateDuration = ::updateDuration,
            onRecordingComplete = ::stopRecordingUI,
            onPermissionDenied = ::showPermissionDeniedMessage
        )
        if (field.status == FieldStatus.REQUIRED) {
            requirements.add(Requirement(field.displayName) {
                audioManager?.audioUri != null
            })
        }

        binding.btnRecord.apply {
            text = field.displayNameWithIndicator
            setOnClickListener {
                audioManager?.recordOrStop()
            }
        }

        binding.btnRerecord.setOnClickListener {
            audioManager?.recordOrStop()
        }
    }

    private fun offerToLock() {
        promptForConfirmation(
            "Would you like to pin the page so students cannot navigate away from it?"
        )
        { lockPage() }
    }

    private fun isAuthenticationAvailable(): Boolean {
        val biometricManager = BiometricManager.from(requireContext())
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun lockPage() {
        if (!isAuthenticationAvailable()) {
            Snackbar.make(
                binding.root,
                "The page cannot be pinned because authentication is not available on this device.",
                Snackbar.LENGTH_INDEFINITE
            ).apply {
                // Make action color match text color.
                val textView =
                    view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                val messageTextColor = textView.currentTextColor
                setAction(R.string.ok_button) { }
                setActionTextColor(messageTextColor)
                show()
            }
            return
        }

        // App pinning
        requireActivity().startLockTask()

        // Back button
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    authenticateToLeave()
                }
            }
        )

        // Exit button
        binding.btnExit.apply {
            setIconResource(R.drawable.lock)
        }

        isLocked = true
    }

    private fun authenticateToLeave() {
        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(requireContext()),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    requireActivity().stopLockTask()
                    findNavController().navigateUp()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authentication Required")
            .setSubtitle("Verify to leave this screen")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun promptForConfirmation(message: String, action: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> action() }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @Suppress("unused") // currently no Delete button
    private fun promptToDeleteRecording() {
        promptForConfirmation("Do you really want to delete the recording?", ::deleteRecording)
    }

    private fun setupFormSection() {
        // Add text watchers for form validation
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                checkForRequiredInputs()
            }
        }

        setupNameFields(textWatcher)
        setupIdField(textWatcher)
        setupPronounsSection(textWatcher)
    }

    private fun setupNameFields(textWatcher: TextWatcher) {
        // First and last name fields are always required but can be renamed.
        fieldConfigViewModel.getFirstNameField().also {
            val name = it.displayNameWithIndicator
            binding.tilFirstName.hint = name
            binding.etFirstName.addTextChangedListener(textWatcher)
            requirements.add(Requirement(it.displayName) {
                binding.etFirstName.text?.isNotEmpty() == true
            })
        }
        fieldConfigViewModel.getLastNameField().also {
            binding.tilLastName.hint = it.displayNameWithIndicator
            binding.etLastName.addTextChangedListener(textWatcher)
            requirements.add(Requirement(it.displayName) {
                binding.etLastName.text?.isNotEmpty() == true
            })
        }

        fieldConfigViewModel.getPreferredNameField().apply {
            when (status) {
                FieldStatus.NOT_SOLICITED -> {
                    binding.etPreferredName.visibility = View.GONE
                    return
                }

                FieldStatus.REQUIRED -> {
                    requirements.add(Requirement(displayName) {
                        binding.etPreferredName.text?.isNotEmpty() == true
                    })
                    binding.etPreferredName.addTextChangedListener(textWatcher)
                }

                FieldStatus.OPTIONAL -> {}
            }

            binding.tilPreferredName.hint = displayNameWithIndicator
        }
    }

    private fun setupIdField(textWatcher: TextWatcher) {
        val field = fieldConfigViewModel.getIdField()
        if (field.status == FieldStatus.NOT_SOLICITED) {
            binding.tilId.visibility = View.GONE
            return
        }
        if (field.status == FieldStatus.REQUIRED) {
            requirements.add(Requirement(field.displayName) {
                binding.etId.text?.isNotEmpty() == true
            })
            binding.etId.addTextChangedListener(textWatcher)
        }

        binding.tilId.hint = field.displayNameWithIndicator
    }

    private fun setupPronounsSection(textWatcher: TextWatcher) {
        val field = fieldConfigViewModel.getPronounsField()
        if (field.status == FieldStatus.NOT_SOLICITED) {
            binding.pronounsSection.visibility = View.GONE
            return
        }

        if (field.status == FieldStatus.REQUIRED) {
            requirements.add(Requirement(field.displayName) {
                binding.rgPronouns.checkedRadioButtonId != -1 &&
                        (binding.rgPronouns.checkedRadioButtonId != R.id.rbOther ||
                                binding.etOtherPronouns.hasText())
            })
            binding.etOtherPronouns.addTextChangedListener(textWatcher)
        }

        binding.pronounsLabel.text = field.displayNameWithIndicator
        binding.rgPronouns.setOnCheckedChangeListener { _, checkedId ->
            binding.tilOtherPronouns.visibility = if (checkedId == R.id.rbOther) {
                View.VISIBLE
            } else {
                View.GONE
            }
            if (field.status == FieldStatus.REQUIRED) {
                checkForRequiredInputs()
            }
        }
    }

    private fun displayCapturedPhoto(photoUri: Uri) {
        binding.apply {
            capturedPhoto.setImageURI(photoUri)
            capturedPhoto.visibility = View.VISIBLE
            photoPlaceholder.visibility = View.GONE
            btnTakePhoto.visibility = View.GONE
            btnRetakePhoto.visibility = View.VISIBLE
        }
        checkForRequiredInputs()
    }

    private fun deletePhoto() {
        deletePhotoUI()
        photoManager?.deletePhotoFile()
    }

    private fun deletePhotoUI() {
        binding.apply {
            capturedPhoto.visibility = View.GONE
            photoPlaceholder.visibility = View.VISIBLE
            btnTakePhoto.visibility = View.VISIBLE
            btnRetakePhoto.visibility = View.GONE
        }
    }

    private fun stopRecordingUI(duration: Long) {
        binding.apply {
            capturedAudio.text = String.format("Name recorded (%s)", makeDurationString(duration))

            btnRecord.visibility = View.GONE
            // Restore text and icon in case button is shown again later.
            btnRecord.text = fieldConfigViewModel.getRecordingField().displayNameWithIndicator
            btnRecord.setIconResource(R.drawable.microphone_outline)

            btnRerecord.visibility = View.VISIBLE
        }
        checkForRequiredInputs()
    }

    private fun updateDuration(duration: Long) {
        if (duration == 0L) {
            binding.apply {
                capturedAudio.visibility = View.VISIBLE
                noAudio.visibility = View.GONE
                // Always use the Record button for Stop Recording
                // because it is weighted more heavily than Rerecord.
                btnRecord.text = getString(R.string.stop_recording)
                btnRecord.setIconResource(R.drawable.stop_circle_outline)
                btnRecord.visibility = View.VISIBLE
                btnRerecord.visibility = View.GONE
            }
        }
        binding.capturedAudio.text = String.format("Recording %s...", makeDurationString(duration))
    }

    private fun makeDurationString(duration: Long): String {
        val seconds = (duration / MILLIS_PER_SECOND) % SECONDS_PER_MINUTE
        val minutes = (duration / MILLIS_PER_SECOND) / SECONDS_PER_MINUTE
        return String.Companion.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    private fun deleteRecording() {
        deleteAudioUI()
        audioManager?.deleteAudioFile()
    }

    private fun deleteAudioUI() {
        binding.apply {
            capturedAudio.visibility = View.GONE
            noAudio.visibility = View.VISIBLE
            btnRecord.visibility = View.VISIBLE
            btnRerecord.visibility = View.GONE
        }
    }

    private fun setupActionButtons() {
        binding.btnExit.setOnClickListener {
            if (isLocked) {
                authenticateToLeave()
            } else {
                findNavController().navigateUp()
            }
        }

        binding.btnClear.setOnClickListener {
            promptToClearForm()
        }

        binding.btnSave.setOnClickListener {
            saveButtonHandler()
        }
    }

    private fun promptToClearForm() {
        promptForConfirmation("Do you really want to clear the form?", ::clearForm)
    }

    private fun clearForm() {
        binding.apply {
            etId.text?.clear()
            etFirstName.text?.clear()
            etLastName.text?.clear()
            etPreferredName.text?.clear()
            rgPronouns.clearCheck()
            scrollView.smoothScrollTo(0, 0)
            btnSave.isEnabled = false
        }
        deletePhoto()
        deleteRecording()
    }

    private fun requiredFieldsComplete() =
        requirements.all { it.check() }

    // Check if all required fields have been completed.
    private fun checkForRequiredInputs() {
        binding.btnSave.isEnabled = requiredFieldsComplete()
    }

    private fun saveButtonHandler() {
        // We should be able to get here only if the form has been validated, but make sure.
        if (!requiredFieldsComplete()) {
            Toast.makeText(
                requireContext(),
                "Please complete required fields (indicated with ${StudentField.Companion.REQUIRED_INDICATOR}).",
                Toast.LENGTH_LONG
            )
                .show()
            Log.e(TAG, "saveButtonHandler() reached with invalid form")
            return
        }
        saveStudentInfo()
    }

    private fun saveStudentInfo() {
        val nuid = binding.etId.text.toString()
        val firstName = binding.etFirstName.text.toString()
        val lastName = binding.etLastName.text.toString()
        val preferredName = binding.etPreferredName.text.toString().ifEmpty { null }

        val pronouns = when (binding.rgPronouns.checkedRadioButtonId) {
            R.id.rbSheHer -> "she/her/hers"
            R.id.rbHeHim -> "he/him/his"
            R.id.rbTheyThem -> "they/them/theirs"
            R.id.rbOther -> binding.etOtherPronouns.text.toString()
            else -> ""
        }

        lifecycleScope.launch {
            val backend = (requireActivity() as MainActivity).backend
            val success = backend?.writeStudent(
                crn = crn,
                nuid = nuid,
                firstName = firstName,
                lastName = lastName,
                preferredName = preferredName?.ifEmpty { null },
                pronouns = pronouns,
                photoUri = photoManager?.photoUri,
                audioUri = audioManager?.audioUri,
            )

            if (success == true) {
                Toast.makeText(
                    requireContext(),
                    "Student saved successfully!",
                    Toast.LENGTH_SHORT
                )
                    .show()
                clearForm()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to save student",
                    Toast.LENGTH_SHORT
                )
                    .show()
                binding.btnSave.isEnabled = true
            }
        }
    }

    private fun showPermissionDeniedMessage(permission: String) {
        Toast.makeText(
            requireContext(),
            "$permission permission is required for this feature",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // navigation
        findNavController().removeOnDestinationChangedListener(navListener)
        isLocked = false

        // free memory
        clearForm() // deletes media files
        _binding = null
        requirements.clear()
        photoManager = null
        audioManager = null
    }

    companion object {
        private const val TAG = "AddStudentFragment"
        private const val MILLIS_PER_SECOND = 1000
        private const val SECONDS_PER_MINUTE = 60
    }
}