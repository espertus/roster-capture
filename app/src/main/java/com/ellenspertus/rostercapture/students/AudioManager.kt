package com.ellenspertus.rostercapture.students

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import java.io.File

/**
 * Audio capture management for [AddStudentFragment].
 */
class AudioManager(
    private val fragment: Fragment,
    private val onUpdateDuration: (Long) -> Unit,
    private val onRecordingComplete: (Long) -> Unit,
    private val onPermissionDenied: (String) -> Unit
) {
    private val context
        get() = fragment.requireContext()

    private var mediaRecorder: MediaRecorder? = null
    var audioUri: Uri? = null
        private set
    private var isRecording = false
    private var recordingStartTime = 0L
    private val recordingHandler = Handler(Looper.getMainLooper())
    private val duration
        get() = System.currentTimeMillis() - recordingStartTime

    private val audioPermissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            onPermissionDenied("Microphone")
        }
    }

    fun checkAudioPermissionAndRecord() {
        when {
            context.checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }

            else -> {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun recordOrStop() {
        if (isRecording) {
            stopRecording()
        } else {
            checkAudioPermissionAndRecord()
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
            onRecordingComplete(duration)

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to stop recording", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Stop recording failed", e)
        }
    }

    fun startRecording() {
        try {
            deleteAudioFile()

            // Create audio file
            val audioFile =
                File(context.cacheDir, "student_audio_${System.currentTimeMillis()}.m4a")
            audioUri = MediaManager.fileToUri(context, audioFile)

            // Initialize MediaRecorder
            mediaRecorder = MediaRecorder(context).apply {
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
            onUpdateDuration(0L)
            updateRecordingDuration()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Recording failed", e)
        }
    }

    fun deleteAudioFile() {
        MediaManager.uriToFile(context, audioUri)?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        audioUri = null
    }

    private fun updateRecordingDuration() {
        if (isRecording) {
            onUpdateDuration(duration)
            recordingHandler.postDelayed({ updateRecordingDuration() }, 100)
        }
    }

    companion object {
        private const val TAG = "AudioCaptureManager"
    }
}