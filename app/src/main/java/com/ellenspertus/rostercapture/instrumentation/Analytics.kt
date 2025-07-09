package com.ellenspertus.rostercapture.instrumentation

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
        this.isEnabled = isEnabled
    }

    /**
     * Logs [event] to Google Analytics if the [guard] is true for the prior event
     * count and if logging is enabled. This also increments the zero-based event
     * count and adds it to [bundle], which is passed to Google Analytics.
     */
    fun log(event: String, bundle: Bundle? = null, guard: (Int) -> Boolean = { true }) {
        val count = prefs.getInt(event, 0)
        if (isEnabled && guard(count)) {
            (bundle ?: Bundle()).let {
                it.putInt("count", count)
                Firebase.analytics.logEvent(event, it)
            }
        }
        prefs.edit { putInt(event, count + 1) }
    }

    fun logFirstTime(event: String, bundle: Bundle? = null) {
        log(event, bundle) { it == 0 }
    }

    fun logEveryNthTime(n: Int, event: String, bundle: Bundle? = null) {
        log(event, bundle) { (it % n) == 0 }
    }

    fun logFirstNTimes(n: Int, event: String, bundle: Bundle? = null) {
        log(event, bundle) { it < n }
    }

    fun logEveryNDays(days: Int, event: String, bundle: Bundle = Bundle()) {
        val key = "${event}${LAST_LOGGED_SUFFIX}"
        val lastLogged = prefs.getLong(key, 0)
        val now = System.currentTimeMillis()

        if (isEnabled && now - lastLogged >= days * MILLIS_PER_DAY) {
            bundle.putInt("every_n_days", days)
            Firebase.analytics.logEvent(event, bundle)
            prefs.edit { putLong(key, now) }
        }
    }

    fun logError(tag: String?, message: String) {
        val bundle = Bundle()
        if (tag != null) {
            bundle.putString("tag", tag)
        }
        log(
            "E: $message",
            bundle
        ) { it < 5 || it % 5 == 0 }
    }
}