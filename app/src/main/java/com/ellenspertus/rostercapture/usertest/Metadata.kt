package com.ellenspertus.rostercapture.usertest

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Build
import androidx.core.content.edit
import com.ellenspertus.rostercapture.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class Metadata(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    private val numRuns = incrementNumRuns()
    private val userId = getOrCreateUserId()

    private fun getOrCreateUserId(): String =
        prefs.getString(USER_ID_KEY, null) ?: run {
            UUID.randomUUID().toString().also {
                prefs.edit { putString(USER_ID_KEY, it) }
            }
        }

    private fun incrementNumRuns() =
        (prefs.getInt(NUM_RUNS_KEY, 0) + 1).also {
            prefs.edit { putInt(NUM_RUNS_KEY, it) }
        }

    fun initializeCrashlytics(instance: FirebaseCrashlytics) {
        instance.apply {
            setUserId(userId)
            if (numRuns == 1) {
                shareFullMetadata(instance)
            }
            setCustomKey("num_runs", numRuns)
        }
    }

    private fun shareFullMetadata(instance: FirebaseCrashlytics) {
        instance.apply {
            setCustomKey("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            setCustomKey("android_version", Build.VERSION.RELEASE)
            setCustomKey("api_level", Build.VERSION.SDK_INT)
            setCustomKey("first_launch", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()))
            setCustomKey("initial_locale", Locale.getDefault().toString())

            // Screen info
            val displayMetrics = context.resources.displayMetrics
            setCustomKey("screen_resolution", "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}")
            setCustomKey("screen_density", displayMetrics.density)

            // Version info
            setCustomKey("version_name", BuildConfig.VERSION_NAME)
            setCustomKey("version_code", BuildConfig.VERSION_CODE)
        }
    }

    companion object {
        private const val PREFS_NAME = "usertest"
        private const val USER_ID_KEY = "user_id"
        private const val NUM_RUNS_KEY = "num_runs"
    }
}