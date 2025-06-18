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
import kotlinx.coroutines.launch

class SelectCourseFragment : Fragment() {
    private var _binding: FragmentSelectCourseBinding? = null
    private val binding get() = _binding!!

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
        binding.modeToggle.bottomControlsCard.visibility = View.INVISIBLE

//        (activity as MainActivity).verifyAuthentication()
        viewLifecycleOwner.lifecycleScope.launch {
            // TODO: Manage courses
            val courses: List<Course> = listOf(
                Course(crn = "12345", id = "6.001", name = "SICP")
            )
            if (courses.isEmpty()) {
                Log.e(TAG, "No courses retrieved")
            } else {
                solicitCourse(courses.sortedBy { it.id })
            }
        }
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

    companion object {
        const val TAG = "SelectCourseFragment"
    }
}