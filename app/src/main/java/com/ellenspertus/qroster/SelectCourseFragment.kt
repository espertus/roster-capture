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
import androidx.navigation.fragment.findNavController
import com.ellenspertus.qroster.model.Course
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SelectCourseFragment : Fragment() {
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_course, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).verifyAuthentication()
        viewLifecycleOwner.lifecycleScope.launch {
            solicitCourse(retrieveCourses())
        }
    }

    private fun solicitCourse(courses: List<Course>) {
        view?.apply {
            findViewById<TextView>(R.id.textWelcome)?.text =
                context.getString(R.string.select_a_course)
            findViewById<TextInputLayout>(R.id.menu_layout).visibility = View.VISIBLE
            findViewById<AutoCompleteTextView>(R.id.dropdown_text)?.apply {
                visibility = View.VISIBLE
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    courses.map { "${it.shortName} (${it.studentsCount}/${it.enrollmentsCount})" }
                )
                setAdapter(adapter)

                setOnItemClickListener { _, _, position, _ ->
                    val selectedCourse = courses[position]
                    val action =
                        SelectCourseFragmentDirections.actionSelectCourseFragmentToStudentsFragment(
                            selectedCourse.crn
                        )
                    findNavController().navigate(action)
                }
            }
        }
    }

    private suspend fun retrieveCourses(): List<Course> = coroutineScope {
        try {
            val snapshot = db.collection(COURSES_COLLECTION)
                .get()
                .await()

            val courses = snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Course::class.java)?.also {
                        it.crn = document.id
                    } ?: throw Exception("documentToObject() returned null")
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

    companion object {
        const val TAG = "SelectCourseFragment"
    }
}