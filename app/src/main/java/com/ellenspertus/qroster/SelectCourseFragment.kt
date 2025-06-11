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
    private val firestore by lazy { Firebase.firestore }

    // These cards are created programmatically.
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
            solicitCourse(retrieveCourses().sortedBy { it.id })
        }
        binding.modeToggle.bottomControlsCard.visibility = View.INVISIBLE
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
            val snapshot = firestore.collection(COURSES_COLLECTION)
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
            if (courses.isEmpty()) {
                Log.e(TAG, "No courses retrieved")
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
                courseIdText.text = it.id
                courseNameText.text = it.name
                courseCountText.text = String.format(
                    getString(R.string.students_ratio),
                    course.studentsCount,
                    course.enrollmentsCount
                )

                root.setOnClickListener { view ->
                    courseCards.forEach { card ->
                        val wasClicked = card.root == view
                        card.root.isChecked = wasClicked
                        card.root.isSelected = wasClicked
                    }
                    enableToggleButtons(course)
                }
            }
            courseCards.add(this)
            binding.coursesContainer.addView(this.root)
        }
    }

    private fun enableToggleButtons(course: Course) {
        binding.modeToggle.apply {
            bottomControlsCard.visibility = View.VISIBLE
            modeToggleGroup.clearOnButtonCheckedListeners()
            modeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    val action = when (checkedId) {
                        R.id.quizButton -> SelectCourseFragmentDirections.actionSelectCourseFragmentToQuizFragment(
                            course.crn
                        )

                        R.id.browseButton -> SelectCourseFragmentDirections.actionSelectCourseFragmentToBrowseFragment(
                            course.crn
                        )

                        else -> {
                            Log.e(TAG, "Unexpected case in enableToggleButtons()")
                            throw AssertionError("Unreachable code hit in enableToggleButtons()")
                        }
                    }
                    findNavController().navigate(action)
                }
            }
        }
    }

    companion object {
        const val TAG = "SelectCourseFragment"
    }
}