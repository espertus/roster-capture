package com.ellenspertus.rostercapture

import android.app.Application
import com.ellenspertus.rostercapture.usertest.Analytics
import com.ellenspertus.rostercapture.usertest.Metadata
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MyApplication : Application() {
    private val metadata by lazy { Metadata(this) }

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        Analytics.init(this, true) // TODO: !BuildConfig.DEBUG
        FirebaseCrashlytics.getInstance().apply {
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            metadata.initializeCrashlytics(this)
        }
    }
}