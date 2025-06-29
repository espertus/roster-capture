package com.ellenspertus.rostercapture

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.ellenspertus.rostercapture.anki.AnkiBackend

class MainActivity : FragmentActivity() {
    var backend: AnkiBackend? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}