package com.ellenspertus.qroster

import android.app.Application
import com.bumptech.glide.Glide
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.StorageReference
import java.io.InputStream

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
    }
}