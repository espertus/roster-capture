package com.ellenspertus.qroster

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ellenspertus.qroster.model.Student
import com.google.firebase.firestore.FirebaseFirestore

class StudentViewModel : ViewModel() {
    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students

    private val firestore = FirebaseFirestore.getInstance()

    fun loadStudentsForCourse(crn: Long) {
        firestore.collection(ENROLLMENTS_COLLECTION)
            .whereEqualTo("crn", crn)
            .get()
            .addOnSuccessListener { enrollmentDocuments ->
                // Extract all student IDs from the enrollments
                val studentIds = enrollmentDocuments.documents.mapNotNull { doc ->
                    doc.getLong("nuid")
                }
                if (studentIds.isEmpty()) {
                    _students.value = emptyList()
                    return@addOnSuccessListener
                }

                // Split student IDs into chunks of 10 (Firestore limit for whereIn)
                val chunks = studentIds.chunked(RETRIEVAL_CHUNK_SIZE)
                val allStudents = mutableListOf<Student>()
                var remainingQueries = chunks.size

                fun completeChunk(newStudents: List<Student> = emptyList()) {
                    synchronized(allStudents) {
                        allStudents.addAll(newStudents)
                        if (--remainingQueries == 0) {
                            _students.value = allStudents.shuffled()
                        }
                    }
                }

                // For each chunk, run a separate query
                chunks.forEach { chunk ->
                    firestore.collection(STUDENTS_COLLECTION)
                        .whereIn("nuid", chunk)
                        .get()
                        .addOnSuccessListener { studentDocuments ->
                            val chunkStudents = studentDocuments.documents.mapNotNull { doc ->
                                try {
                                    val temp = doc.toObject(Student::class.java)
                                    temp?.docId = doc.id
                                    temp
                                } catch (e: Exception) {
                                    Log.e("StudentViewModel", "Error converting doc ${doc.id}", e)
                                    null
                                }
                            }
                            completeChunk(chunkStudents)
                        }
                        .addOnFailureListener { e ->
                            Log.e("StudentViewModel", "Error loading students chunk", e)
                            completeChunk()
                        }
                }
            }
    }

    companion object {
        // whereIn queries can have up to 10 values
        private const val RETRIEVAL_CHUNK_SIZE = 10
    }

//    // Track learning progress
//    fun markStudentAsLearned(position: Int) {
//        val studentList = _students.value ?: return
//        if (position < 0 || position >= studentList.size) return
//
//        val student = studentList[position]
//
//        // Update Firestore with learning progress
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//        firestore.collection("userProgress")
//            .document(userId)
//            .collection("learnedStudents")
//            .document(student.nuid)
//            .set(mapOf(
//                "learnedAt" to FieldValue.serverTimestamp(),
//                "studentId" to student.nuid,
//                "crn" to student.crn
//            ))
//    }
}