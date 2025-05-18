package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

private const val TAG = "MainActivity"
const val COURSES_COLLECTION = "courses"
const val STUDENTS_COLLECTION = "students"
const val ENROLLMENTS_COLLECTION = "enrollments"

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (BuildConfig.DEBUG) {
            try {
                Firebase.auth.useEmulator("10.0.2.2", 9099)
                Firebase.firestore.useEmulator("10.0.2.2", 8080)
                Firebase.storage.useEmulator("10.0.2.2", 9199)
                Log.d("Firebase", "Using Firebase Emulator Suite")
            } catch (e: Exception) {
                Log.e("Firebase", "Error setting up Firebase emulators: ${e.message}")
            }
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
}