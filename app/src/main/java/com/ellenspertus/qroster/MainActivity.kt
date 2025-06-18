package com.ellenspertus.qroster

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.ellenspertus.qroster.backend.Backend
import com.ellenspertus.qroster.backend.anki.AnkiBackend
import com.ellenspertus.qroster.backend.anki.AnkiWrapper
import com.ellenspertus.qroster.backend.firebase.FirebaseBackend
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

const val COURSES_COLLECTION = "courses"
const val STUDENTS_COLLECTION = "students"
const val STUDENTS_RAW_COLLECTION = "studentsRaw"
const val ENROLLMENTS_COLLECTION = "enrollments"

class MainActivity : FragmentActivity() {
    var backend: Backend? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        when {
            BuildConfig.USE_FIREBASE -> createFirebaseBackend()
            BuildConfig.USE_ANKI -> createAnkiBackend()
            else -> throw IllegalStateException()
        }
    }

    // Firebase
    private fun createFirebaseBackend() {
        FirebaseApp.initializeApp(this)

        Firebase.firestore.clearPersistence()
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            if (onAVD()) {
                useEmulator()
            }
        }
        backend = FirebaseBackend()
    }

    private fun onAVD() =
        Build.SUPPORTED_ABIS.any { it.startsWith( "x86") }

    private fun useEmulator() {
        try {
            Firebase.auth.useEmulator("10.0.2.2", 9099)
            Firebase.firestore.useEmulator("10.0.2.2", 8080)
            Firebase.storage.useEmulator("10.0.2.2", 9199)
            Log.d("Firebase", "Using Firebase Emulator Suite")
        } catch (e: Exception) {
            Log.e("Firebase", "Error setting up Firebase emulators: ${e.message}")
        }
    }

    fun verifyAuthentication() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated")
            finishAndRemoveTask()
        } else {
            Log.d(TAG, "User authenticated: ${currentUser.uid}")
        }
    }

    // Anki
    private fun createAnkiBackend() {
        val api = AnkiWrapper(this)
        if (api.isApiAvailable(this)) {
            if (api.shouldRequestPermission()) {
                api.requestPermission(this, PERM_CODE)
            } else {
                backend = AnkiBackend(this)
            }
        } else {
            Log.e(TAG, "API is not available")
            Toast.makeText(this, "Make sure AnkiDroid is installed.", Toast.LENGTH_LONG).show()
            // TODO: Transition to failure fragment
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            backend = AnkiBackend(this)
        } else {
            Toast.makeText(this, "Cannot continue without requested permissions", Toast.LENGTH_LONG).show()
            // TODO: Transition to failure fragment
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        private const val PERM_CODE = 0
    }
}