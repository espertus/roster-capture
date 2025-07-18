package com.ellenspertus.rostercapture

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.ellenspertus.rostercapture.databinding.FragmentFailureBinding
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlin.getValue
import kotlin.system.exitProcess

/**
 * A fragment displaying information about why the program cannot continue.
 */
class FailureFragment : Fragment() {
    private val args: FailureFragmentArgs by navArgs()

    private var _binding: FragmentFailureBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFailureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args.exception.let {
            Firebase.crashlytics.recordException(it)
            binding.apply {
                textMessage.text = it.message
                imageIcon.setImageResource(
                    when (it) {
                        is AppException.AppUserException -> R.drawable.outline_app_blocking_24
                        is AppException.AppInternalException -> R.drawable.outline_error_24
                    }
                )
                buttonExit.setOnClickListener {
                    endProcess()
                }
                if (it.restartable) {
                    buttonRestart.let {
                        it.visibility = View.VISIBLE
                        it.setOnClickListener {
                            scheduleRestart(requireContext())
                        }
                    }
                }
            }
        }
    }

    private fun scheduleRestart(context: Context) {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.RTC,
            System.currentTimeMillis() + 100,  // 100ms delay
            pendingIntent
        )
        endProcess()
    }

    private fun endProcess() {
        requireActivity().finishAffinity()
        exitProcess(0)
    }
}