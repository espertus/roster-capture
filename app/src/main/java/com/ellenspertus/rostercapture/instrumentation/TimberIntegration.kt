package com.ellenspertus.rostercapture.instrumentation

import android.util.Log
import com.ellenspertus.rostercapture.BuildConfig
import timber.log.Timber

object TimberIntegration {
    fun initialize() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }

    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority >= Log.ERROR) {
                val tagString = tag ?: "UNKNOWN"
                val messageString = when (val msg = t?.message) {
                    null -> message
                    else -> msg
                }
                Analytics.logError(tagString, messageString)
            }
        }
    }
}