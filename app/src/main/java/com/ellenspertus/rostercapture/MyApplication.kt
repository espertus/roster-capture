package com.ellenspertus.rostercapture

import android.app.Application
import com.ellenspertus.rostercapture.instrumentation.Analytics
import com.ellenspertus.rostercapture.instrumentation.Metadata
import com.ellenspertus.rostercapture.instrumentation.TimberIntegration
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MyApplication : Application() {
    private val metadata by lazy { Metadata(this) }

    override fun onCreate() {
        super.onCreate()

        initializeAnalytics()
        initializeCrashlytics()
        TimberIntegration.initialize()
    }

    private fun initializeAnalytics() {
        FirebaseApp.initializeApp(this)
        Analytics.init(this, !BuildConfig.DEBUG)
    }

    private fun initializeCrashlytics() {
        FirebaseCrashlytics.getInstance().apply {
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            metadata.initializeCrashlytics(this)
        }
    }
}

