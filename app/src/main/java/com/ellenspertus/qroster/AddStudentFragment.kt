package com.ellenspertus.qroster

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ellenspertus.qroster.databinding.FragmentAddStudentBinding
import kotlinx.coroutines.launch
import java.io.File

class AddStudentFragment() : Fragment() {
    private lateinit var crn: String
    private var _binding: FragmentAddStudentBinding? = null
    private val binding get() = _binding!!

    // Photo capture
    private var photoUri: Uri? = null
    private val takePictureLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            displayCapturedPhoto()
        }
    }

    // Audio recording
    private var mediaRecorder: MediaRecorder? = null
    private var audioUri: Uri? = null
    private var isRecording = false
    private var recordingStartTime = 0L
    private val recordingHandler = Handler(Looper.getMainLooper())

    // Permissions
    private val cameraPermissionLauncher = registerForActivityResult(
        RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            showPermissionDeniedMessage("Camera")
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            showPermissionDeniedMessage("Microphone")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            crn = AddStudentFragmentArgs.fromBundle(it).crn
        } ?: throw IllegalStateException("No CRN passed to $TAG")
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
        setupPhotoSection()
        setupAudioSection()
        setupFormSection()
        setupActionButtons()
    }

    private fun setupPhotoSection() {
        binding.photoContainer.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        binding.btnTakePhoto.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        binding.btnRetakePhoto.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }
    }

    private fun setupAudioSection() {
        binding.btnRecord.setOnClickListener {
            recordOrStop()
        }

        binding.btnRerecord.setOnClickListener {
            recordOrStop()
        }
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

    private fun recordOrStop() {
        if (isRecording) {
            stopRecording()
        } else {
            checkAudioPermissionAndRecord()
        }
    }

    private fun setupFormSection() {
        // Add text watchers for form validation
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        }

        binding.etNuid.addTextChangedListener(textWatcher)
        binding.etFirstName.addTextChangedListener(textWatcher)
        binding.etLastName.addTextChangedListener(textWatcher)

        // Pronouns radio group
        binding.rgPronouns.setOnCheckedChangeListener { _, checkedId ->
            binding.tilOtherPronouns.visibility = if (checkedId == R.id.rbOther) {
                View.VISIBLE
            } else {
                View.GONE
            }
            validateForm()
        }

        // Other pronouns field
        binding.etOtherPronouns.addTextChangedListener(textWatcher)
    }

    // Photo capture methods
    private fun checkCameraPermissionAndLaunch() {
        deletePhotoFile()
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }

            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        deletePhotoFile()

        val photoFile =
            File(requireContext().filesDir, "student_photo_${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            putExtra("android.intent.extras.CAMERA_FACING", 1)
            putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        }
        takePictureLauncher.launch(intent)
    }

    private fun displayCapturedPhoto() {
        binding.apply {
            capturedPhoto.setImageURI(photoUri)
            capturedPhoto.visibility = View.VISIBLE
            photoPlaceholder.visibility = View.GONE
            btnTakePhoto.visibility = View.GONE
            btnRetakePhoto.visibility = View.VISIBLE
        }
    }

    private fun deletePhoto() {
        deletePhotoUI()
        deletePhotoFile()
    }

    private fun uriToFile(uri: Uri?): File? =
        uri?.lastPathSegment?.let { fileName ->
            File(requireContext().filesDir, fileName)
        }

    private fun fileToUri(file: File): Uri =
        FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

    private fun deletePhotoUI() {
        binding.apply {
            capturedPhoto.visibility = View.GONE
            photoPlaceholder.visibility = View.VISIBLE
            btnTakePhoto.visibility = View.VISIBLE
            btnRetakePhoto.visibility = View.GONE
        }
    }

    private fun deletePhotoFile() {
        uriToFile(photoUri)?.let { file ->
            if (file.exists()) {
                file.delete()
                photoUri = null
            }
        }
    }

    // Audio recording methods
    private fun checkAudioPermissionAndRecord() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }

            else -> {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startRecording() {
        try {
            deleteAudioFile()

            // Create audio file
            val audioFile =
                File(requireContext().filesDir, "student_audio_${System.currentTimeMillis()}.m4a")
            audioUri = fileToUri(audioFile)

            // Initialize MediaRecorder
            mediaRecorder = MediaRecorder(requireContext()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            recordingStartTime = System.currentTimeMillis()

            // Update UI
            binding.apply {
                capturedAudio.visibility = View.VISIBLE
                noAudio.visibility = View.GONE
                // Always use the Record button for Stop Recording
                // because it is weighted more heavily than Rerecord.
                btnRecord.text = "Stop recording"
                btnRecord.setIconResource(R.drawable.stop_circle_outline)
                btnRecord.visibility = View.VISIBLE
                btnRerecord.visibility = View.GONE
            }

            // Start duration updates
            updateRecordingDuration()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to start recording", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Recording failed", e)
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            recordingHandler.removeCallbacksAndMessages(null)

            // Update UI
            binding.apply {
                capturedAudio.text = String.format("Name recorded (%s)", makeDurationString())

                btnRecord.visibility = View.GONE
                // Restore text and icon in case button is shown again later.
                btnRecord.text = "Record name"
                btnRecord.setIconResource(R.drawable.microphone_outline)

                btnRerecord.visibility = View.VISIBLE
            }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to stop recording", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Stop recording failed", e)
        }
    }

    private fun updateRecordingDuration() {
        if (isRecording) {
            binding.capturedAudio.text = String.format("Recording %s...", makeDurationString())
            recordingHandler.postDelayed({ updateRecordingDuration() }, 100)
        }
    }

    private fun makeDurationString(): String {
        val duration = System.currentTimeMillis() - recordingStartTime
        val seconds = (duration / MILLIS_PER_SECOND) % SECONDS_PER_MINUTE
        val minutes = (duration / MILLIS_PER_SECOND) / SECONDS_PER_MINUTE
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun deleteRecording() {
        deleteAudioUI()
        deleteAudioFile()
    }

    private fun deleteAudioUI() {
        binding.apply {
            capturedAudio.visibility = View.GONE
            noAudio.visibility = View.VISIBLE
            btnRecord.visibility = View.VISIBLE
            btnRerecord.visibility = View.GONE
        }
    }

    private fun deleteAudioFile() {
        uriToFile(audioUri)?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        audioUri = null
    }

    @Suppress("unused") // currently no Play button
    private fun playRecording() {
        audioUri?.let {
            try {
                // binding.btnPlay.isEnabled = false
                MediaPlayer().apply {
                    setDataSource(uriToFile(it)?.absolutePath)
                    setOnCompletionListener {
                        // binding.btnPlay.isEnabled = true
                    }
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to play media file: $e")
                // binding.btnPlay.isEnabled = true
            }
        } ?: run {
            Log.e(TAG, "playRecording() called but no recording")
        }
    }

    // Action buttons: Clear, Exit, Save
    private fun setupActionButtons() {
        // TODO: Restrict access
        binding.btnExit.setOnClickListener {
            findNavController().navigateUp()
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
            etNuid.text?.clear()
            etFirstName.text?.clear()
            etLastName.text?.clear()
            rgPronouns.clearCheck()
            scrollView.smoothScrollTo(0, 0)
        }
        deletePhoto()
        deleteRecording()
    }

    private fun EditText.hasText() = text?.isNotEmpty() == true

    private fun requiredFieldsComplete() =
        binding.etNuid.hasText() &&
                binding.etFirstName.hasText() &&
                binding.etLastName.hasText() &&
                binding.rgPronouns.checkedRadioButtonId != -1 &&
                (binding.rgPronouns.checkedRadioButtonId != R.id.rbOther ||
                        binding.etOtherPronouns.hasText())

    // Check if all required fields have been completed.
    private fun validateForm() {
        binding.btnSave.isEnabled = requiredFieldsComplete()
    }

    private fun saveButtonHandler() {
        // We should be able to get here only if the form has been validated, but make sure.
        if (!requiredFieldsComplete()) {
            Toast.makeText(
                requireContext(),
                "Please complete required fields.",
                Toast.LENGTH_LONG
            )
                .show()
            Log.e(TAG, "saveButtonHandler() reached with invalid form")
            return
        }
        val missingFiles = mutableListOf<String>()
        if (audioUri == null) {
            missingFiles.add("name recording")
        }
        if (photoUri == null) {
            missingFiles.add("selfie")
        }
        if (missingFiles.isEmpty()) {
            saveStudentInfo()
        } else {
            val missingItems = missingFiles.joinToString(separator = " and ")
            val prompt =
                String.format("Are you sure you want to save without providing a $missingItems?")
            promptForConfirmation(prompt, ::saveStudentInfo)
        }
    }

    private fun saveStudentInfo() {
        val nuid = binding.etNuid.text.toString()
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
                photoUri = photoUri,
                audioUri = audioUri,
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
        mediaRecorder?.release()
        recordingHandler.removeCallbacksAndMessages(null)

        deletePhotoFile()
        deleteAudioFile()

        _binding = null
    }

    companion object {
        private const val TAG = "AddStudentFragment"
        private const val MILLIS_PER_SECOND = 1000
        private const val SECONDS_PER_MINUTE = 60
    }
}
