package com.ellenspertus.rostercapture.extensions

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ellenspertus.rostercapture.AppException
import com.ellenspertus.rostercapture.FailureFragment
import com.ellenspertus.rostercapture.NavGraphDirections

/**
 * Navigate from the current fragment to [FailureFragment]
 * with the specified [exception].
 */
fun Fragment.navigateToFailure(exception: AppException) {
    Log.e(this::class.simpleName, exception.toString())
    findNavController().navigateSafe(
        NavGraphDirections.actionGlobalFailureFragment(exception)
    )
}

fun Fragment.navigateToFailure(message: String) {
    Log.e(this::class.simpleName, message)
    findNavController().navigateSafe(
        NavGraphDirections.actionGlobalFailureFragment(AppException.AppInternalException(message))
    )
}