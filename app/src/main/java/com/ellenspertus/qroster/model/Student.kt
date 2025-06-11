package com.ellenspertus.qroster.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Student(
    val firstName: String,
    val lastName: String,
    val preferredName: String? = null,
    var nuid: String,
    val pronouns: String,
    val selfiePath: String? = null,
    val audioPath: String? = null,
    @Exclude
    var audioDownloadUrl: String? = null,
    var note: String? = null,
    @Exclude
    var docId: String = "",
    var score: Double = 0.0,
) {
    constructor() : this(
        firstName = "",
        lastName = "",
        preferredName = null,
        nuid = "",
        pronouns = "",
        selfiePath = null,
        audioPath = null,
        audioDownloadUrl = null,
        note = null,
        docId = "",
        score = 0.0,
    )

    val displayName
        get() =
            if (preferredName == null)
                rosterName
            else
                "$firstName ($preferredName) $lastName"

    val rosterName
        get() =
            "$firstName $lastName"

    override fun toString() = displayName
}