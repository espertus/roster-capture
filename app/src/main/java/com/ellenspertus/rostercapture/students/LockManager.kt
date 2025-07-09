package com.ellenspertus.rostercapture.students

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.ellenspertus.rostercapture.R
import com.ellenspertus.rostercapture.extensions.promptForConfirmation
import com.ellenspertus.rostercapture.instrumentation.Analytics

class LockManager(
    private val fragment: Fragment,
    private val onLockAttempt: (Status) -> Unit
) {
    private val context
        get() = fragment.requireContext()

    private val activity
        get() = fragment.requireActivity()

    var isLocked = false
        private set

    enum class Status {
        AUTH_NOT_AVAILABLE,
        LOCKED
    }

    // If the fragment is not locked, we should offer to lock it only if
    // the fragment is reached through the navigation graph, not if we
    // return to the fragment from taking a picture, for example.
    private var shouldOfferLocking = true
    val navListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        if (destination.id == R.id.addStudentFragment) {
            shouldOfferLocking = true
        }
    }

    init {
        fragment.findNavController().addOnDestinationChangedListener(navListener)
    }

    fun handleExitRequest() {
        if (isLocked) {
            authenticateToLeave()
        } else {
            fragment.findNavController().navigateUp()
        }
    }

    fun resume() {
        if (!isLocked && shouldOfferLocking) {
            offerToLock()
            shouldOfferLocking = false
        }
    }

    fun destroy() {
        fragment.findNavController().removeOnDestinationChangedListener(navListener)
        isLocked = false
    }

    fun lockPage() {
        Analytics.logFirstNTimes(5, "pinning_attempted")
        if (isAuthenticationAvailable()) {
            Analytics.logFirstNTimes(5, "pinning_available")
            activity.startLockTask()
            isLocked = true
            onLockAttempt(Status.LOCKED)
        } else {
            onLockAttempt(Status.AUTH_NOT_AVAILABLE)
        }
    }

    fun isAuthenticationAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun offerToLock() {
        Analytics.logFirstNTimes(10, "pinning_offered")
        context.promptForConfirmation(
            "Would you like to pin the page so students cannot navigate away from it?",
            {
                Analytics.logFirstNTimes(10, "pinning_chosen")
                lockPage()
            },
            {  Analytics.logFirstNTimes(10, "pinning_declined") }
        )
    }

    private fun authenticateToLeave() {
        val biometricPrompt = BiometricPrompt(
            fragment,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    activity.stopLockTask()
                    fragment.findNavController().navigateUp()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authentication Required")
            .setSubtitle("Verify to leave this screen")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}