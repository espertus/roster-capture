package com.ellenspertus.qroster.backend

import android.net.Uri
import com.ellenspertus.qroster.model.Course

interface Backend {
    suspend fun writeStudent(
        crn: String,
        nuid: String,
        firstName: String,
        lastName: String,
        preferredName: String?,
        pronouns: String,
        photoUri: Uri?,
        audioUri: Uri?
    ): Boolean

    suspend fun retrieveCourses(): List<Course>
}