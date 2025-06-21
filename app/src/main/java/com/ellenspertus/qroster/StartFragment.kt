package com.ellenspertus.qroster

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.ellenspertus.qroster.backend.AnkiWrapper
import com.ellenspertus.qroster.databinding.FragmentStartBinding
import kotlin.system.exitProcess
import androidx.core.net.toUri
import com.ellenspertus.qroster.backend.AnkiBackend
import com.ichi2.anki.FlashCardsContract.READ_WRITE_PERMISSION

/**
 * An invisible fragment that verifies that the API is accessible
 * and that required permissions are granted. Depending on the result,
 * it navigates to [SelectCourseFragment] or [FailureFragment] or
 * exits.
 */
class StartFragment : Fragment() {
    private var _binding: FragmentStartBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainActivity: MainActivity

    private val requestPermissionLauncher: ActivityResultLauncher<String?> =
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainActivity = (requireActivity() as MainActivity)
        if (!isAnkiDroidInstalled()) {
            requestAnkiDroid()
            return
        }

        if (mainActivity.backend == null) {
            // This will instantiate a backend if permissions are granted.
            checkAnkiPermissions()
        } else {
            navigateToSelectCourseFragment()
        }
    }

    private fun isAnkiDroidInstalled(): Boolean =
        AnkiWrapper(mainActivity).isApiAvailable(mainActivity)

    private fun navigateToSelectCourseFragment() {
        findNavController().navigate(
            StartFragmentDirections.actionStartFragmentToSelectCourseFragment()
        )
    }

    private fun requestAnkiDroid() {
        binding.apply {
            tvAnki.visibility = View.VISIBLE
            buttonProceed.let {
                it.visibility = View.VISIBLE
                it.text = "Install AnkiDroid"
                it.setOnClickListener {
                    openPlayStore()
                }
            }
            showExitButton()
        }
    }

    // Permissions handling

    private fun handlePermissionDenied() {
        // At this point, we don't know whether we are allowed to ask again.
        fail(
            AppException.AppUserException(
                getString(R.string.permissions_rejection)
            )
        )
    }

    private fun showExitButton() {
        binding.buttonExit.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                exitProcess(0)
            }
        }
    }

    private fun openPlayStore() {
        try {
            // Try to open in Play Store app (preferred)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = ANKI_MARKET_URL.toUri()
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to web browser
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(ANKI_WEB_URL)
            }
            startActivity(intent)
        }
        exitProcess(0)
    }

    private fun createBackend() {
        try {
            mainActivity.backend = AnkiBackend(mainActivity)
            navigateToSelectCourseFragment()
        } catch (e: AppException) {
            fail(e)
        }
    }



    private fun checkAnkiPermissions() {
        when (checkPermissionStatus(READ_WRITE_PERMISSION)) {
            PermissionStatus.GRANTED -> createBackend()
            PermissionStatus.DENIED_CAN_ASK_AGAIN -> solicitPermission()
            PermissionStatus.UNKNOWN_TRY_REQUEST -> solicitPermission()
            PermissionStatus.PERMANENTLY_DENIED -> handlePermissionDenied()
        }
    }

    fun checkPermissionStatus(permission: String): PermissionStatus {
        return when {
            requireContext().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED -> {
                PermissionStatus.GRANTED
            }

            shouldShowRequestPermissionRationale(permission) -> {
                PermissionStatus.DENIED_CAN_ASK_AGAIN
            }

            else -> {
                // Either first time or permanently denied
                PermissionStatus.UNKNOWN_TRY_REQUEST
            }
        }
    }

    enum class PermissionStatus {
        GRANTED,
        DENIED_CAN_ASK_AGAIN,
        UNKNOWN_TRY_REQUEST,
        PERMANENTLY_DENIED
    }

    private fun solicitPermission() {
        binding.apply {
            tvPermissions.visibility = View.VISIBLE
            buttonProceed.visibility = View.VISIBLE
            buttonProceed.text = "Grant Permission"
            buttonProceed.setOnClickListener {
                requestPermissionLauncher.launch(READ_WRITE_PERMISSION)
                buttonProceed.visibility = View.GONE
            }
        }
    }

    private fun fail(exception: AppException) {
        Log.e(TAG, exception.toString())
        findNavController().navigate(
            StartFragmentDirections.actionStartFragmentToFailureFragment(
                exception
            )
        )
    }

    companion object {
        private const val TAG = "StartFragment"

        private const val ANKI_WEB_URL =
            "https://play.google.com/store/apps/details?id=com.ichi2.anki"
        private const val ANKI_MARKET_URL = "market://details?id=com.ichi2.anki"
    }
}