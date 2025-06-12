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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ellenspertus.qroster.databinding.FragmentAddStudentBinding
import java.io.File

private const val MILLIS_PER_SECOND = 1000
private const val SECONDS_PER_MINUTE = 60

class AddStudentFragment : Fragment() {
    private lateinit var crn: String
    private var _binding: FragmentAddStudentBinding? = null
    private val binding get() = _binding!!

    // Photo capture
    private lateinit var photoUri: Uri
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            displayCapturedPhoto()
        }
    }

    // Audio recording
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
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
        // Photo container click listener
        binding.photoContainer.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        // Take photo button
        binding.btnTakePhoto.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        // Retake photo button
        binding.btnRetakePhoto.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }
    }

    private fun setupAudioSection() {
        binding.btnRecord.setOnClickListener {
            recordOrStop()
        }
        binding.btnPlay.setOnClickListener {
            playRecording()
        }
        binding.btnDelete.setOnClickListener {
            promptToDeleteRecording()
        }
    }

    private fun promptToDeleteRecording() {
        AlertDialog.Builder(requireContext())
            .setMessage("Do you really want to delete the recording?")
            .setPositiveButton("Yes") { _, _ ->
                deleteRecording()
            }
            .setNegativeButton("No") {
                dialog, _ -> dialog.dismiss()
            }
            .show()
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

    private fun setupActionButtons() {
        // TODO: Restrict access
        binding.btnExit.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            saveStudentInfo()
        }
    }

    // Photo capture methods
    private fun checkCameraPermissionAndLaunch() {
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
        val photoFile = File(requireContext().filesDir, "student_photo_${System.currentTimeMillis()}.jpg")
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
        binding.capturedPhoto.setImageURI(photoUri)
        binding.capturedPhoto.visibility = View.VISIBLE
        binding.photoPlaceholder.visibility = View.GONE
        binding.btnRetakePhoto.visibility = View.VISIBLE
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
            // Create audio file
            val audioFile = File(requireContext().filesDir, "student_audio_${System.currentTimeMillis()}.m4a")
            audioFilePath = audioFile.absolutePath

            // Initialize MediaRecorder
            mediaRecorder = MediaRecorder(requireContext()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }

            isRecording = true
            recordingStartTime = System.currentTimeMillis()

            // Update UI
            binding.btnRecord.setImageResource(R.drawable.stop_circle_outline)
            binding.tilRecordedName.hint = "Recording..."

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
            binding.tilRecordedName.hint = "Name recording complete"
            binding.btnRecord.visibility = View.GONE
            binding.btnPlay.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.VISIBLE

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to stop recording", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Stop recording failed", e)
        }
    }

    private fun updateRecordingDuration() {
        if (isRecording) {
            val duration = System.currentTimeMillis() - recordingStartTime
            val seconds = (duration / MILLIS_PER_SECOND) % SECONDS_PER_MINUTE
            val minutes = (duration / MILLIS_PER_SECOND) / SECONDS_PER_MINUTE
            binding.tilRecordedName.hint = String.format("Recording in progress (%d:%02d)", minutes, seconds)
            recordingHandler.postDelayed({ updateRecordingDuration() }, 100)
        }
    }

    private fun deleteRecording() {
        // Delete file
        audioFilePath?.let { path ->
            File(path).delete()
        }
        audioFilePath = null

        // Reset UI
        binding.tilRecordedName.hint = getString(R.string.record_name_request)
        binding.btnRecord.setImageResource(R.drawable.microphone_outline)
        binding.btnRecord.visibility = View.VISIBLE
        binding.btnDelete.visibility = View.GONE
        binding.btnPlay.visibility = View.GONE
    }

    private fun playRecording() {
        if (audioFilePath == null) {
            Log.e(TAG, "playRecording() called but no recording")
            return
        }
        try {
            binding.btnPlay.isEnabled = false
            MediaPlayer().apply {
                setDataSource(audioFilePath)
                setOnCompletionListener {
                    binding.btnPlay.isEnabled = true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to play media file: $e")
            binding.btnPlay.isEnabled = true
        }
    }

    // Form validation
    private fun validateForm() {
        val isValid = binding.etNuid.text?.isNotEmpty() == true &&
                binding.etFirstName.text?.isNotEmpty() == true &&
                binding.etLastName.text?.isNotEmpty() == true &&
                binding.rgPronouns.checkedRadioButtonId != -1 &&
                (binding.rgPronouns.checkedRadioButtonId != R.id.rbOther ||
                        binding.etOtherPronouns.text?.isNotEmpty() == true)

        binding.btnSave.isEnabled = isValid
    }

    // Save student info
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

        // TODO: Save to your database/repository
        // For now, just show success message
        Toast.makeText(requireContext(), "Student information saved!", Toast.LENGTH_SHORT).show()

        // Navigate back
        findNavController().navigateUp()
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
        _binding = null
    }

    companion object {
        const val TAG = "AddStudentFragment"
    }
}
