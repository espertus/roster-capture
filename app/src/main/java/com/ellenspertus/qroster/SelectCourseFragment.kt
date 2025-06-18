package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ellenspertus.qroster.databinding.FragmentSelectCourseBinding
import com.ellenspertus.qroster.databinding.ItemCourseCardBinding
import com.ellenspertus.qroster.dialog.AddCourseDialogFragment
import com.ellenspertus.qroster.model.Course
import com.ellenspertus.qroster.viewmodel.CoursesViewModel
import kotlinx.coroutines.launch

class SelectCourseFragment : Fragment() {
    private var _binding: FragmentSelectCourseBinding? = null
    private val binding get() = _binding!!

    // These cards are created programmatically.
    private val courseCards = mutableListOf<ItemCourseCardBinding>()

    // ViewModel for managing courses
    private val coursesViewModel: CoursesViewModel by viewModels()

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
        binding.modeToggle.bottomControlsCard.visibility = View.INVISIBLE

        // Set up FAB click listener
        binding.fabAddCourse.setOnClickListener {
            AddCourseDialogFragment().show(childFragmentManager, AddCourseDialogFragment.TAG)
        }

        // Observe courses from DataStore
        viewLifecycleOwner.lifecycleScope.launch {
            coursesViewModel.courses.collect { courses ->
                // Clear existing UI courses
                binding.coursesContainer.removeAllViews()
                courseCards.clear()

                if (courses.isEmpty()) {
                    Log.e(TAG, "No courses retrieved")
                    binding.textWelcome.text = getString(R.string.no_courses_found)
                } else {
                    solicitCourse(courses.sortedBy { it.id })
                }
            }
        }

        // Example: Add a sample course if none exist (remove this in production)
        // viewLifecycleOwner.lifecycleScope.launch {
        //     if (coursesViewModel.courses.value.isEmpty()) {
        //         coursesViewModel.addCourse(
        //             Course(crn = "12345", id = "6.001", name = "SICP")
        //         )
        //     }
        // }
    }

    private fun solicitCourse(courses: List<Course>) {
        binding.apply {
            if (courses.isEmpty()) {
                textWelcome.text = getString(R.string.no_courses_found)
                return
            }
            textWelcome.text = getString(R.string.select_a_course)
            courses.forEach { addCourseToUI(it) }
            coursesContainer.visibility = View.VISIBLE
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

                root.setOnClickListener { view ->
                    courseCards.forEach { card ->
                        val wasClicked = card.root == view
                        card.root.isChecked = wasClicked
                        card.root.isSelected = wasClicked
                    }
                    enableToggleButtons(course)
                }

                // Add long click to delete course (optional)
                root.setOnLongClickListener {
                    // You might want to show a confirmation dialog here
                    coursesViewModel.removeCourse(course.crn)
                    true
                }
            }
            courseCards.add(this)
            binding.coursesContainer.addView(this.root)
        }
    }

    private fun enableToggleButtons(course: Course) {
        binding.modeToggle.apply {
            bottomControlsCard.visibility = View.VISIBLE
            modeToggleGroup.clearChecked()
            modeToggleGroup.clearOnButtonCheckedListeners()
            modeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    val action = when (checkedId) {

                        R.id.addStudentButton -> SelectCourseFragmentDirections.actionSelectCourseFragmentToAddStudentFragment(
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SelectCourseFragment"
    }
}