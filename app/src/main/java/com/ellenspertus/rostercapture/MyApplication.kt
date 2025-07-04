package com.ellenspertus.rostercapture

import android.app.Application
import com.ellenspertus.rostercapture.usertest.TestMetadata
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MyApplication: Application() {
    private val testMetadata by lazy { TestMetadata(this) }

    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().apply {
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            testMetadata.initializeCrashlytics(this)
        }
    }
}