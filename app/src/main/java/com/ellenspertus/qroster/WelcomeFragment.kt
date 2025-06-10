package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity.RESULT_OK
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


/**
 * The initial fragment, which signs in the user and directs to [SelectCourseFragment].
 */
class WelcomeFragment : Fragment() {
    private lateinit var user: FirebaseUser
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        this.onSignInResult(res)
    }

    override fun onStart() {
        super.onStart()
        // Only launch sign-in if we don't already have a user
        if (FirebaseAuth.getInstance().currentUser == null) {
            launchSignInIntent()
        } else {
            // User already signed in, navigate immediately
            findNavController().navigate(R.id.action_welcomeFragment_to_selectCourseFragment)
        }
    }

    private fun launchSignInIntent() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
        )
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            FirebaseAuth.getInstance().currentUser?.let {
                user = it
                Log.d(TAG, "Successfully signed in")

                // Check if we're still at WelcomeFragment before navigating
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.welcomeFragment) {
                    navController.navigate(R.id.action_welcomeFragment_to_selectCourseFragment)
                } else {
                    Log.d(TAG, "Already navigated away from WelcomeFragment")
                }
                return
            }
            Log.e(TAG, "RESULT_OK but user null?!")
        } else if (response == null) {
            Log.w(TAG, "User cancelled sign-in. Retry.")
            launchSignInIntent()
        } else {
            Log.e(TAG, response.error.toString())
            activity?.finishAndRemoveTask()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    companion object {
        const val TAG = "WelcomeFragment"
    }
}