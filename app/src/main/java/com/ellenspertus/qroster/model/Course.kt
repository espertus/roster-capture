package com.ellenspertus.qroster.model

import com.google.firebase.firestore.Exclude

data class Course(
    @Exclude
    var crn: String = "", // copied from docid
    val shortName: String = "",
    val longName: String = "",
    val semester: String = "",
    var enrollmentsCount: Long = 0,
    var studentsCount: Long = 0,
)
