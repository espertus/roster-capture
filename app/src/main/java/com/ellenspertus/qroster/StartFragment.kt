package com.ellenspertus.qroster

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.ellenspertus.qroster.AppException.AppUserException
import com.ellenspertus.qroster.backend.AnkiBackend
import com.ellenspertus.qroster.backend.AnkiWrapper

/**
 * An invisible fragment that verifies that the API is accessible
 * and that required permissions are granted. Depending on the result,
 * it navigates to [SelectCourseFragment] or [FailureFragment].
 */
class StartFragment : Fragment() {
    private lateinit var mainActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = (requireActivity() as MainActivity)

        try {
            mainActivity.backend?.run {
                findNavController().navigate(
                    StartFragmentDirections.actionStartFragmentToSelectCourseFragment()
                )
            }
        } catch (e: AppException.AppInternalException) {
            fail(e)
        }
        createAnkiBackend()
    }

    private fun createAnkiBackend() {
        val api = AnkiWrapper(mainActivity)
        if (api.isApiAvailable(mainActivity)) {
            if (api.shouldRequestPermission()) {
                api.requestPermission(this, PERM_CODE)
            } else {
                mainActivity.backend = AnkiBackend(mainActivity)
            }
        } else {
            fail(AppUserException("The AnkiDroid app must be installed."))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mainActivity.backend = AnkiBackend(mainActivity)
            findNavController().navigate(
                StartFragmentDirections.actionStartFragmentToSelectCourseFragment())
        } else {
            fail(AppUserException("Cannot continue without required permissions. Please restart application."))
        }
    }

    private fun fail(exception: AppException) {
        Log.e(TAG, exception.toString())
        findNavController().navigate(
            StartFragmentDirections.actionStartFragmentToFailureFragment(
                exception
            ))
    }

    companion object {
        private const val TAG = "StartFragment"
        private const val PERM_CODE = 0 // arbitrary
    }
}