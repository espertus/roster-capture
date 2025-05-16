package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ellenspertus.qroster.model.Course
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.toObject

class SelectCourseFragment : Fragment() {
    val courses = mutableListOf<Course>()
    val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_class_select, container, false)
    }

    override fun onStart() {
        super.onStart()
        (activity as MainActivity).verifyAuthentication()
        retrieveCourses()
    }

    private fun retrieveCourses() {
        db.collection(COURSES_COLLECTION)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    courses.add(document.toObject<Course>())
                }
                retrieveCourseCounts()
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }

    private fun retrieveCourseCounts() {
        // For now, ignore semesters and long names.
        // Assume short names are unique and used in students.
        courses.forEach { course ->
            db.collection(STUDENTS_COLLECTION)
                .whereEqualTo("courseName", course.shortName)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener { snapshot ->
                    course.count = snapshot.count.toInt()
                    Log.d(TAG, "Count for ${course.shortName}: ${course.count} ")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error getting student count for ${course.shortName}")
                    course.count = 0
                }
        }
    }

    companion object {
        const val COURSES_COLLECTION = "courses"
        const val STUDENTS_COLLECTION = "students"
        const val TAG = "SelectCourseFragment"
    }
}