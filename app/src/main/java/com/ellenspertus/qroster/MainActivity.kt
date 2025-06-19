package com.ellenspertus.qroster

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.ellenspertus.qroster.backend.AnkiBackend
import com.ellenspertus.qroster.backend.AnkiWrapper

class MainActivity : FragmentActivity() {
    var backend: AnkiBackend? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createAnkiBackend()
    }

    // Anki
    private fun createAnkiBackend() {
        val api = AnkiWrapper(this)
        if (api.isApiAvailable(this)) {
            if (api.shouldRequestPermission()) {
                api.requestPermission(this, PERM_CODE)
            } else {
                backend = AnkiBackend(this)
            }
        } else {
            Log.e(TAG, "API is not available")
            Toast.makeText(this, "Make sure AnkiDroid is installed.", Toast.LENGTH_LONG).show()
            // TODO: Transition to failure fragment
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            backend = AnkiBackend(this)
        } else {
            Toast.makeText(this, "Cannot continue without requested permissions", Toast.LENGTH_LONG).show()
            // TODO: Transition to failure fragment
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        private const val PERM_CODE = 0
    }
}