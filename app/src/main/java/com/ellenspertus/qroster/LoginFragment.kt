package com.ellenspertus.qroster

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse

class LoginFragment : Fragment() {
    private companion object {
        private const val RC_SIGN_IN = 123
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Start Firebase UI sign-in flow directly
        startSignIn()
    }

    private fun startSignIn() {
        val providers = listOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
            // Add other providers if needed:
            // AuthUI.IdpConfig.EmailBuilder().build(),
            // AuthUI.IdpConfig.PhoneBuilder().build(),
            // etc.
        )

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setTheme(R.style.Theme_QRoster)
  //              .setLogo(R.drawable.logo) // Optional: your app logo
                .build(),
            RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                (activity as? MainActivity)?.navigateToMainContent()
            } else {
                // Sign in failed
                if (response == null) {
                    // User cancelled sign-in flow
                    Toast.makeText(context, "Sign in cancelled", Toast.LENGTH_SHORT).show()
                } else {
                    // Handle error
                    Toast.makeText(context, "Sign in error: ${response.error?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}