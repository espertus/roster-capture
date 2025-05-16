package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ellenspertus.qroster.model.Course
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SelectCourseFragment : Fragment() {
    val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_class_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).verifyAuthentication()
        viewLifecycleOwner.lifecycleScope.launch {
            val courses = retrieveCourses()
            retrieveCourseCounts(courses)
            solicitCourse(courses)
        }
    }

    private fun solicitCourse(courses: List<Course>) {
        view?.findViewById<TextView>(R.id.textWelcome)?.text = ""
        view?.findViewById<AutoCompleteTextView>(R.id.dropdown_text)?.apply {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                courses.map { "${it.shortName} (${it.count})"}
            )
            setAdapter(adapter)

            setOnItemClickListener { _, _, position, _ ->
                val selectedCourse = courses[position]
                Log.d(TAG, "User selected $selectedCourse")
            }
        }
    }

    private suspend fun retrieveCourses(): List<Course> = coroutineScope {
        try {
            val snapshot = db.collection(COURSES_COLLECTION)
                .get()
                .await() // This suspends until the task completes and returns the QuerySnapshot

            // Process the result after await() completes
            val courses = snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Course::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document ${document.id}", e)
                    null
                }
            }

            courses
        } catch (exception: Exception) {
            Log.e(TAG, "Error getting documents", exception)
            emptyList() // Return empty list on error
        }
    }

    private suspend fun retrieveCourseCounts(courses: List<Course>): List<Course> = coroutineScope {
        val deferredResults = courses.map { course ->
            async {
                try {
                    val snapshot = db.collection(STUDENTS_COLLECTION)
                        .whereEqualTo("courseName", course.shortName)
                        .count()
                        .get(AggregateSource.SERVER)
                        .await()

                    course.count = snapshot.count.toInt()
                    Log.d(TAG, "Count for ${course.shortName}: ${course.count}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting student count for ${course.shortName}", e)
                    course.count = 0
                }
            }
        }

        // Wait for all async operations to complete
        deferredResults.awaitAll()
        Log.d(TAG, "All course counts retrieved")
        courses
    }

    companion object {
        const val COURSES_COLLECTION = "courses"
        const val STUDENTS_COLLECTION = "students"
        const val TAG = "SelectCourseFragment"
    }
}