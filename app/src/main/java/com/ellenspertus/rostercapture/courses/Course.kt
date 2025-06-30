package com.ellenspertus.rostercapture.courses

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Course(
    var crn: String = "",
    val id: String = "",
    val name: String = "",
) : Parcelable