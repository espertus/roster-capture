package com.ellenspertus.rostercapture.students

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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.ellenspertus.rostercapture.MainActivity
import com.ellenspertus.rostercapture.R
import com.ellenspertus.rostercapture.anki.AnkiConfigViewModel
import com.ellenspertus.rostercapture.configuration.FieldConfigViewModel
import com.ellenspertus.rostercapture.configuration.FieldStatus
import com.ellenspertus.rostercapture.configuration.StudentField
import com.ellenspertus.rostercapture.courses.Course
import com.ellenspertus.rostercapture.databinding.FragmentAddStudentBinding
import com.ellenspertus.rostercapture.extensions.hasText
import com.ellenspertus.rostercapture.extensions.navigateToFailure
import com.ellenspertus.rostercapture.extensions.promptForConfirmation
import com.ellenspertus.rostercapture.instrumentation.Analytics
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Locale

class AddStudentFragment() : Fragment() {
    private val args: AddStudentFragmentArgs by navArgs()
    private lateinit var course: Course

    private var _binding: FragmentAddStudentBinding? = null
    private val binding get() = _binding!!
    private lateinit var firstEditText: TextInputEditText // initializeFirstEditText()
    private val _backend
        get() = (requireActivity() as MainActivity).backend
    private val backend
        get() = _backend!!

    private val fieldConfigViewModel: FieldConfigViewModel by activityViewModels()
    private val ankiConfigViewModel: AnkiConfigViewModel by activityViewModels()
    private var modelId = 0L // initialized in onAttach()
    private var deckId = 0L // initialized in onAttach()

    data class Requirement(val name: String, val check: () -> Boolean)

    private var requirements: MutableList<Requirement> = mutableListOf()

    // Helper classes
    private var photoManager: PhotoManager? = null
    private var audioManager: AudioManager? = null
    private var lockManager: LockManager? = null

    // Lifecycle methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        course = args.course

        ankiConfigViewModel.modelId?.let {
            modelId = it
        } ?: run {
            navigateToFailure("modelId was null in AddStudentFragment")
        }

        ankiConfigViewModel.deckName?.let {
            val fullName = "$it::${course.id} (${course.crn})"
            backend.findDeckIdByName(fullName, createIfAbsent = true)?.let {
                deckId = it
            } ?: run {
                navigateToFailure("Could not create deck $fullName")
            }
        } ?: run {
            navigateToFailure("deckName was null in AddStudentFragment")
        }
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
        lockManager = LockManager(this, ::processLockAttempt)

        // Initialize view.
        requirements = mutableListOf()
        fieldConfigViewModel.loadConfiguration(requireContext())
        setupPhotoSection()
        setupAudioSection()
        setupFormSection()
        setupActionButtons()
        initializeFirstEditText()

        Analytics.logFirstNTimes(10, "add_student_opened")
    }

    override fun onDestroyView() {
        super.onDestroyView()

        lockManager?.destroy()
        lockManager = null

        // free memory
        clearForm() // deletes media files
        _binding = null
        requirements.clear()
        photoManager = null
        audioManager = null
    }

    // View configuration and requirements creation

    private fun setupPhotoSection() {
        // Photos are always required.
        requirements.add(Requirement(fieldConfigViewModel.getSelfieField().displayName) { photoManager?.photoUri != null })
        binding.btnTakePhoto.text = fieldConfigViewModel.getSelfieField().displayNameWithIndicator

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

    private fun initializeFirstEditText() {
        val editTexts = listOf(binding.etId, binding.etFirstName, binding.etLastName)
        editTexts.firstOrNull { it.isEnabled }?.let {
            firstEditText = it
        } ?: navigateToFailure("Could not find enabled edittext")
    }

    // Locking (pinning)

    override fun onResume() {
        super.onResume()
        lockManager?.resume()
    }

    private fun processLockAttempt(status: LockManager.Status) {
        if (status == LockManager.Status.AUTH_NOT_AVAILABLE) {
            reportAuthNotAvailable()
            return
        }

        // Back button
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    lockManager?.handleExitRequest()
                }
            }
        )

        // Exit button
        binding.btnExit.setIconResource(R.drawable.lock)
    }

    private fun reportAuthNotAvailable() {
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
            Analytics.logFirstNTimes(10, "pinning_unavailable")
        }
    }

    // Photo

    private fun displayCapturedPhoto(photoUri: Uri) {
        binding.apply {
            capturedPhoto.setImageURI(photoUri)
            capturedPhoto.visibility = View.VISIBLE
            photoPlaceholder.visibility = View.GONE
            btnTakePhoto.visibility = View.GONE
            btnRetakePhoto.visibility = View.VISIBLE
        }
        checkForRequiredInputs()
        Analytics.logFirstNTimes(5, "photo_taken")
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

    private fun showPermissionDeniedMessage(permission: String) {
        Analytics.log("permission_denied", Bundle().apply {
            putString("permission", permission)
        })
        Toast.makeText(
            requireContext(),
            "$permission permission is required for this feature",
            Toast.LENGTH_LONG
        ).show()
    }

    // Recording

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
        Analytics.logFirstNTimes(5, "recording_made")
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

    // Form handling

    private fun setupActionButtons() {
        binding.btnExit.setOnClickListener {
            lockManager?.handleExitRequest()
        }

        binding.btnClear.setOnClickListener {
            promptToClearForm()
        }

        binding.btnSave.setOnClickListener {
            saveButtonHandler()
        }
    }

    private fun promptToClearForm() {
        requireContext().promptForConfirmation(
            "Do you really want to clear the form?",
            ::clearForm,
        )
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
        firstEditText.requestFocus()
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
                getString(
                    R.string.please_complete_required_fields,
                    StudentField.Companion.REQUIRED_INDICATOR
                ),
                Toast.LENGTH_LONG
            )
                .show()
            Log.e(TAG, "saveButtonHandler() reached with invalid form")
            return
        }
        saveStudentInfo()
    }

    private fun saveStudentInfo() {
        val studentId = binding.etId.text.toString().ifEmpty { null }
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
            try {
                val success = backend.writeStudent(
                    modelId = modelId,
                    deckId = deckId,
                    crn = course.crn,
                    studentId = studentId,
                    firstName = firstName,
                    lastName = lastName,
                    preferredName = preferredName?.ifEmpty { null },
                    pronouns = pronouns,
                    photoUri = photoManager?.photoUri,
                    audioUri = audioManager?.audioUri,
                )

                var message = if (success == true) {
                    clearForm()
                    Analytics.logEveryNthTime(10, "student_save_succeeded")
                    getString(R.string.student_saved_successfully)
                } else {
                    binding.btnSave.isEnabled = true
                    Analytics.logFirstNTimes(10, "student_save_failed")
                    getString(R.string.failed_to_save_student)
                }
                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                navigateToFailure("Failed to save student: $e")
            }
        }
    }

    companion object {
        private const val TAG = "AddStudentFragment"
        private const val MILLIS_PER_SECOND = 1000
        private const val SECONDS_PER_MINUTE = 60
    }
}