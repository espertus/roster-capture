package com.ellenspertus.qroster.model

data class Student(
    val firstName: String,
    val lastName: String,
    val preferredName: String? = null,
    val nuid: Long,
    val pronouns: String,
    val selfieFile: String? = null,
    val audioFile: String? = null,
) {
    constructor() : this(
        firstName = "",
        lastName = "",
        preferredName = "",
        nuid = 0L,
        pronouns = "",
        selfieFile = null,
        audioFile = null
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
}