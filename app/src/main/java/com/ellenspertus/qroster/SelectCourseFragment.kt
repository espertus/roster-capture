package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ellenspertus.qroster.databinding.FragmentSelectCourseBinding
import com.ellenspertus.qroster.databinding.ItemCourseCardBinding
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

    // These layout elements are created programmatically.
    private val courseCards = mutableListOf<ItemCourseCardBinding>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectCourseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).verifyAuthentication()
        viewLifecycleOwner.lifecycleScope.launch {
            solicitCourse(retrieveCourses().sortedBy { it.longName })
        }
    }

    private fun solicitCourse(courses: List<Course>) {
        binding.apply {
            textWelcome.text = requireContext().getString(R.string.select_a_course)
            courses.forEach { addCourseToUI(it) }
            coursesContainer.visibility = View.VISIBLE
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

    private fun addCourseToUI(course: Course) {
        ItemCourseCardBinding.inflate(
            LayoutInflater.from(requireContext()),
            binding.coursesContainer,
            false
        ).apply {
            course.let {
                courseNameText.text = it.shortName
                courseDetailsText.text =
                    "${it.longName}\n${it.studentsCount}/${it.enrollmentsCount} students"
                courseDetailsText.visibility = View.VISIBLE

                root.setOnClickListener { view ->
                    courseCards.forEach { card ->
                        val wasClicked = card.root == view
                        card.root.isChecked = wasClicked
                        card.root.isSelected = wasClicked
                    }

                    binding.browseButton.apply {
                        isEnabled = true
                        setOnClickListener {
                            val action =
                                SelectCourseFragmentDirections.actionSelectCourseFragmentToBrowseStudentsFragment(
                                    course.crn
                                )
                            findNavController().navigate(action)
                        }
                    }

                    binding.quizButton.apply {
                        isEnabled = true
                        setOnClickListener {
                            val action =
                                SelectCourseFragmentDirections.actionSelectCourseFragmentToQuizFragment(
                                    course.crn
                                )
                            findNavController().navigate(action)
                        }
                    }
                }
            }
            courseCards.add(this)
            binding.coursesContainer.addView(this.root)
        }
    }

    companion object {
        const val TAG = "SelectCourseFragment"
    }
}