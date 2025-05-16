package com.ellenspertus.qroster.model

data class Course(
    val shortName: String? = null,
    val longName: String? = null,
    val semester: String? = null,
    var count: Int? = null,
)
