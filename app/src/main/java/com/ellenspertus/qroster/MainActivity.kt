package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import android.window.OnBackInvokedDispatcher
import androidx.fragment.app.FragmentActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class MainActivity : FragmentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enable predictive back
        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT
        ) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish()
            }
        }

        // Setup Firebase Emulators for DEBUG builds
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

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initial navigation based on auth state
        if (savedInstanceState == null) {
            checkAuthState()
        }
    }

    override fun onStart() {
        super.onStart()
        // Check auth state on start
        checkAuthState()
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in
            navigateToMainContent()
        } else {
            // User is signed out
            navigateToLogin()
        }
    }

    fun navigateToLogin() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    fun navigateToMainContent() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ClassSelectFragment())
            .commit()
    }

    fun signOut() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener {
            navigateToLogin()
        }
    }
}