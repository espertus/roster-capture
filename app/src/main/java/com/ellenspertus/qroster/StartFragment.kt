package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ellenspertus.qroster.backend.AnkiWrapper
import com.ellenspertus.qroster.databinding.FragmentStartBinding
import kotlin.system.exitProcess
import com.ellenspertus.qroster.backend.AnkiBackend
import com.ellenspertus.qroster.backend.AnkiWrapper.PermissionStatus
import com.ellenspertus.qroster.configuration.FieldConfigViewModel

/**
 * An invisible fragment that verifies that the API is accessible
 * and that required permissions are granted. Depending on the result,
 * it navigates to [com.ellenspertus.qroster.courses.SelectCourseFragment] or [FailureFragment] or
 * exits.
 */
class StartFragment : Fragment() {
    private var _binding: FragmentStartBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainActivity: MainActivity
    private val fieldConfigViewModel: FieldConfigViewModel by activityViewModels()

    // This needs to be at the top level because registerForActivityResult()
    // can be called only during a Fragment's creation.
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                createBackend()
            } else {
                handlePermissionDenied()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        mainActivity = (requireActivity() as MainActivity)

        // Requirement 1: AnkiDroid is installed.
        if (!isAnkiDroidInstalled()) {
            requestAnkiDroid()
            return
        }

        // Requirement 2: Fields are configured.
        if (!fieldConfigViewModel.hasConfiguration(requireContext())) {
            requestConfiguration()
            return
        }

        // Requirement 3: Permissions are granted.
        if (mainActivity.backend == null) {
            // This will instantiate a backend if permissions are granted.
            checkAnkiPermissions()
        } else {
            navigateToSelectCourseFragment()
        }
    }

    private fun isAnkiDroidInstalled(): Boolean =
        AnkiWrapper.isApiAvailable(mainActivity)

    // Transitions to other fragments

    private fun navigateToSelectCourseFragment() {
        findNavController().navigate(
            StartFragmentDirections.actionStartFragmentToSelectCourseFragment()
        )
    }

    private fun navigateToFieldConfigFragment() {
        findNavController().navigate(
            StartFragmentDirections.actionStartFragmentToFieldConfigFragment()
        )
    }

    private fun fail(exception: AppException) {
        Log.e(TAG, exception.toString())
        findNavController().navigate(
            StartFragmentDirections.actionStartFragmentToFailureFragment(
                exception
            )
        )
    }

    // Primarily UI methods

    private fun requestAnkiDroid() {
        binding.apply {
            tvAnki.visibility = View.VISIBLE
            buttonProceed.let {
                it.visibility = View.VISIBLE
                it.text = "Install AnkiDroid"
                it.setOnClickListener {
                    AnkiWrapper.offerToInstallAnkiDroid(mainActivity)
                    exitProcess(0)
                }
            }
            showExitButton()
        }
    }

    private fun requestConfiguration() {
        binding.apply {
            tvConfigure.visibility = View.VISIBLE
            buttonProceed.let {
                it.visibility = View.VISIBLE
                it.text = "Configure RosterCapture"
                it.setOnClickListener {
                    navigateToFieldConfigFragment()
                }
                tvConfigure.visibility = View.GONE
            }
        }
    }

    private fun showExitButton() {
        binding.buttonExit.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                exitProcess(0)
            }
        }
    }

    // Permissions handling

    private fun checkAnkiPermissions() {
        when (AnkiWrapper.checkPermissionStatus(this)) {
            PermissionStatus.GRANTED -> createBackend()
            PermissionStatus.DENIED_CAN_ASK_AGAIN -> solicitPermission()
            PermissionStatus.UNKNOWN_TRY_REQUEST -> solicitPermission()
            PermissionStatus.PERMANENTLY_DENIED -> handlePermissionDenied()
        }
    }

    private fun solicitPermission() {
        binding.apply {
            tvPermissions.visibility = View.VISIBLE
            buttonProceed.visibility = View.VISIBLE
            buttonProceed.text = "Grant Permission"
            buttonProceed.setOnClickListener {
                launchPermissionRequest()
            }
        }
    }

    private fun launchPermissionRequest() {
        requestPermissionLauncher.launch(AnkiWrapper.REQUIRED_PERMISSION)
        binding.buttonProceed.visibility = View.GONE
    }

    private fun handlePermissionDenied() {
        // At this point, we don't know whether we are allowed to ask again.
        fail(
            AppException.AppUserException(
                getString(R.string.permissions_rejection)
            )
        )
    }

    // This should be called only if AnkiDroid is installed and the required
    // permissions granted.
    private fun createBackend() {
        if (!isAnkiDroidInstalled() || AnkiWrapper.checkPermissionStatus(this) != PermissionStatus.GRANTED) {
            fail(AppException.AppInternalException("Assertion failed in createBackend()"))
        }
        try {
            mainActivity.backend = AnkiBackend(mainActivity)
            navigateToSelectCourseFragment()
        } catch (e: AppException) {
            fail(e)
        }
    }

    companion object {
        private const val TAG = "StartFragment"
    }
}