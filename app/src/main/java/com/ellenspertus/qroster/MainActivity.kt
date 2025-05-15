package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

private const val TAG = "MainActivity"

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (BuildConfig.DEBUG) {
            try {
                Firebase.auth.useEmulator("10.0.2.2", 9099)
                Firebase.database.useEmulator("10.0.2.2", 9000)
                Firebase.storage.useEmulator("10.0.2.2", 9199)
                Log.d("Firebase", "Using Firebase Emulator Suite")
            } catch (e: Exception) {
                Log.e("Firebase", "Error setting up Firebase emulators: ${e.message}")
            }
        }
    }
}