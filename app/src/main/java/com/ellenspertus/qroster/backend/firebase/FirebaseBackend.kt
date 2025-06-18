package com.ellenspertus.qroster.backend.firebase

import android.net.Uri
import android.util.Log
import com.ellenspertus.qroster.COURSES_COLLECTION
import com.ellenspertus.qroster.STUDENTS_RAW_COLLECTION
import com.ellenspertus.qroster.SelectCourseFragment
import com.ellenspertus.qroster.SelectCourseFragment.Companion
import com.ellenspertus.qroster.backend.Backend
import com.ellenspertus.qroster.model.Course
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class FirebaseBackend: Backend {
    override suspend fun writeStudent(
        crn: String,
        nuid: String,
        firstName: String,
        lastName: String,
        preferredName: String?,
        pronouns: String,
        photoUri: Uri?,
        audioUri: Uri?
    ) = try {
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference

        // Set required fields.
        val studentMap = mutableMapOf(
            "nuid" to nuid,
            "crn" to crn,
            "firstName" to firstName,
            "lastName" to lastName,
            "pronouns" to pronouns
        )

        // Set option fields if present.
        preferredName?.let {
            studentMap.put("preferredName", it)
        }
        if (photoUri != null) {
            val selfiePath = "userdata/$SPECIAL_NUID/selfies/$nuid.jpg"
            storageRef.child(selfiePath).putFile(photoUri).await()
            studentMap["selfiePath"] = selfiePath
        }
        if (audioUri != null) {
            val audioPath = "userdata/$SPECIAL_NUID/audio/$nuid.m4a"
            storageRef.child(audioPath).putFile(audioUri).await()
            studentMap["audioPath"] = audioPath
        }

        db.collection(STUDENTS_RAW_COLLECTION)
            .document("$SPECIAL_NUID-$nuid")
            .set(studentMap)
            .await()

        Log.d(TAG, "Student added successfully with NUID: $nuid")
        true

    } catch (e: Exception) {
        Log.e(TAG, "Error in writeToFirebase", e)
        false
    }

    override suspend fun retrieveCourses(): List<Course> = coroutineScope {
        val snapshot = Firebase.firestore.collection(COURSES_COLLECTION)
            .get()
            .await()

        val courses = snapshot.documents.mapNotNull { document ->
            try {
                document.toObject(Course::class.java)?.also {
                    it.crn = document.id
                } ?: throw Exception("documentToObject() returned null")
            } catch (e: Exception) {
                Log.e(SelectCourseFragment.TAG, "Error converting document ${document.id}", e)
                null
            }
        }

        courses
    }

    companion object {
        private const val TAG = "FirebaseBackEnd"

        private const val SPECIAL_NUID = "0"
    }
}