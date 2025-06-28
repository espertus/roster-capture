package com.ellenspertus.rostercapture.students

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.ellenspertus.rostercapture.AppException
import java.io.File

/**
 * Photo capture management for [AddStudentFragment].
 */
class PhotoManager(
    private val fragment: Fragment,
    private val onPhotoCapture: (Uri) -> Unit,
    private val onPermissionDenied: (String) -> Unit
) {
    var photoUri: Uri? = null
        private set
    private val context: Context
        get() = fragment.requireContext()

    private val takePictureLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        photoUri?.let {
            if (result.resultCode == Activity.RESULT_OK) {
                onPhotoCapture(it)
            }
        } ?: run {
            throw AppException.AppInternalException("photoUri is null after capture")
        }
    }

    private val cameraPermissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            onPermissionDenied(Manifest.permission.CAMERA)
        }
    }

    fun checkPermissionAndLaunch() {
        deletePhotoFile()
        when {
            ContextCompat.checkSelfPermission(
                context,
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
            File(
                context.cacheDir,
                "student_photo_${System.currentTimeMillis()}.jpg"
            )
        photoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
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

    fun deletePhotoFile() {
        MediaManager.uriToFile(context, photoUri)?.let { file ->
            if (file.exists()) {
                file.delete()
                photoUri = null
            }
        }
    }
}