package com.ellenspertus.rostercapture.extensions

import android.widget.EditText

fun EditText.hasText() = text?.isNotEmpty() == true
