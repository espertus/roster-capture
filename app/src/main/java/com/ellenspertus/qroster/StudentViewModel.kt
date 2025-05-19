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
                Log.d("StudentViewModel", "Found ${enrollmentDocuments.size()} enrollment documents for CRN $crn")

                // Extract all student IDs from the enrollments
                val studentIds = enrollmentDocuments.documents.mapNotNull { doc ->
                    doc.getLong("nuid")
                }
                Log.d("StudentViewModel", "Extracted student IDs: $studentIds")

                if (studentIds.isEmpty()) {
                    Log.d("StudentViewModel", "No student IDs found, returning empty list")
                    _students.value = emptyList()
                    return@addOnSuccessListener
                }

                // TODO: Chunk the studentIds in groups of 10 for correctness.
                Log.d("StudentViewModel", "Looking up students with IDs: $studentIds")
                firestore.collection(STUDENTS_COLLECTION)
                    .whereIn("nuid", studentIds)
                    .get()
                    .addOnSuccessListener { studentDocuments ->
                        Log.d("StudentViewModel", "Found ${studentDocuments.size()} student documents")
                        val studentsList = studentDocuments.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Student::class.java)
                            } catch (e: Exception) {
                                Log.e("StudentViewModel", "Error converting doc ${doc.id}", e)
                                null
                            }
                        }
                        _students.value = studentsList
                    }
                    .addOnFailureListener { e ->
                        Log.e("StudentViewModel", "Error loading students", e)
                        _students.value = emptyList()
                    }
            }
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