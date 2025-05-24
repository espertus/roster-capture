package com.ellenspertus.qroster.model

import com.google.firebase.firestore.Exclude

data class Student(
    val firstName: String,
    val lastName: String,
    val preferredName: String? = null,
    val nuid: Long,
    val pronouns: String,
    val selfieFile: String? = null,
    // TODO: Change field name to audioStoragePath
    val audioFile: String? = null,
    @Exclude
    var audioDownloadUrl: String? = null,
    var note: String? = null,
    @Exclude
    var docId: String = "",
) {
    constructor() : this(
        firstName = "",
        lastName = "",
        preferredName = null,
        nuid = 0L,
        pronouns = "",
        selfieFile = null,
        audioFile = null,
        audioDownloadUrl = null,
        note = null,
        docId = "",
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