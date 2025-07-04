package com.ellenspertus.rostercapture.usertest

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object Analytics {
    private const val PREFS_NAME = "analytics"
    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    private const val LAST_LOGGED_SUFFIX = "_last"

    private lateinit var prefs: SharedPreferences
    private var isEnabled: Boolean = false

    fun init(context: Context, isEnabled: Boolean) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        this.isEnabled = true
    }

    fun logFirstTime(event: String, bundle: Bundle = Bundle()) {
        if (prefs.getInt(event, 0) == 0) {
            Firebase.analytics.logEvent(event, bundle)
            prefs.edit { putInt(event, 1) }
        }
    }

    fun logEveryNthTime(n: Int, event: String, bundle: Bundle = Bundle()) {
        val count = prefs.getInt(event, 0)
        if (count % n == 0) {
            bundle.putInt("time", count)
            bundle.putInt("every_nth_time", n)
            Firebase.analytics.logEvent(event, bundle)
        }
        prefs.edit { putInt(event, count + 1) }
    }

    fun logFirstNTimes(n: Int, event: String, bundle: Bundle = Bundle()) {
        val count = prefs.getInt(event, 0)
        if (count < n) {
            bundle.putInt("time", count)
            bundle.putInt("first_n_times", n)
            Firebase.analytics.logEvent(event, bundle)
        }
        prefs.edit { putInt(event, count + 1) }
    }

    fun logEveryNDays(days: Int, event: String, bundle: Bundle = Bundle()) {
        val key = "${event}${LAST_LOGGED_SUFFIX}"
        val lastLogged = prefs.getLong(key, 0)
        val now = System.currentTimeMillis()

        if (now - lastLogged >= days * MILLIS_PER_DAY) {
            bundle.putInt("every_n_days", days)
            Firebase.analytics.logEvent(event, bundle)
            prefs.edit { putLong(key, now) }
        }
    }

    fun log(event: String, bundle: Bundle = Bundle()) {
        if (isEnabled) {
            Firebase.analytics.logEvent(event, bundle)
        }
    }
}