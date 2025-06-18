package com.ellenspertus.qroster.model

data class Course(
    var crn: String = "", // copied from docid
    val id: String = "", // e.g., "CS 1000"
    val name: String = "",
)
