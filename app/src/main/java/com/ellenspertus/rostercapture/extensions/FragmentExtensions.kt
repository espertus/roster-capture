package com.ellenspertus.rostercapture.extensions

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ellenspertus.rostercapture.AppException
import com.ellenspertus.rostercapture.FailureFragment
import com.ellenspertus.rostercapture.NavGraphDirections
import timber.log.Timber

/**
 * Navigate from the current fragment to [FailureFragment]
 * with the specified [exception].
 */
fun Fragment.navigateToFailure(exception: AppException) {
    Timber.e(exception)
    findNavController().navigateSafe(
        NavGraphDirections.actionGlobalFailureFragment(exception)
    )
}

fun Fragment.navigateToFailure(message: String) {
    Timber.e(message)
    findNavController().navigateSafe(
        NavGraphDirections.actionGlobalFailureFragment(AppException.AppInternalException(message))
    )
}