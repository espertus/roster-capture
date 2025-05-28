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
import com.ellenspertus.qroster.databinding.FragmentSelectCourseBinding
import com.ellenspertus.qroster.model.Course
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SelectCourseFragment : Fragment() {
    private var _binding: FragmentSelectCourseBinding? = null
    private val binding get() = _binding!!
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSelectCourseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).verifyAuthentication()
        viewLifecycleOwner.lifecycleScope.launch {
            solicitCourse(retrieveCourses())
        }
    }

    private fun solicitCourse(courses: List<Course>) {
        binding.apply {
            textWelcome.text = requireContext().getString(R.string.select_a_course)
            menuLayout.visibility = View.VISIBLE
            dropdownText.let {
                it.visibility = View.VISIBLE
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    courses.map { "${it.shortName} (${it.studentsCount}/${it.enrollmentsCount})" }
                )
                it.setAdapter(adapter)

                it.setOnItemClickListener { _, _, position, _ ->
                    val selectedCourse = courses[position]

                    browseButton.apply {
                        isEnabled = true
                        setOnClickListener {
                            val action = SelectCourseFragmentDirections.actionSelectCourseFragmentToBrowseStudentsFragment(
                                selectedCourse.crn
                            )
                            findNavController().navigate(action)
                        }
                    }

                    quizButton.apply {
                        isEnabled = true
                        setOnClickListener {
                            val action = SelectCourseFragmentDirections.actionSelectCourseFragmentToQuizFragment(
                                selectedCourse.crn
                            )
                            findNavController().navigate(action)
                        }
                    }
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