package com.ellenspertus.rostercapture.extensions

fun String.equalsIgnoreCase(s: String) = this.lowercase() == s.lowercase()