package com.ellenspertus.rostercapture.extensions

fun Collection<String>.containsIgnoreCase(s: String) =
    this.any {it.equals(s, ignoreCase = true) }