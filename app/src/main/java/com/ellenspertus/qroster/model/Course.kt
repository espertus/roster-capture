package com.ellenspertus.qroster.model

data class Course(
    val crn: Long = 0,
    val shortName: String = "",
    val longName: String = "",
    val semester: String = "",
    var count: Int? = null,
)
