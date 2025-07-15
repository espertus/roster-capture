package com.ellenspertus.rostercapture

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.ellenspertus.rostercapture.anki.AnkiBackend
import com.ellenspertus.rostercapture.app.UpdateChecker

class MainActivity : FragmentActivity() {
    var backend: AnkiBackend? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        UpdateChecker.checkUpdate(this, lifecycleScope)
    }
}