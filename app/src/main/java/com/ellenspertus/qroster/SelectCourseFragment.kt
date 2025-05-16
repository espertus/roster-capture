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
import com.google.firebase.firestore.toObject

class SelectCourseFragment : Fragment() {
    val courses = mutableListOf<Course>()

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
        val db = Firebase.firestore
        db.collection(COURSES_COLLECTION)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    courses.add(document.toObject<Course>())
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }

    companion object {
        const val COURSES_COLLECTION = "courses"
        const val TAG = "SelectCourseFragment"
    }
}