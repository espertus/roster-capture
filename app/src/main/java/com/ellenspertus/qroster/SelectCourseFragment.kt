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

    private val courseCards = mutableListOf<ItemCourseCardBinding>()
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

        binding.fabAddCourse.setOnClickListener {
            // TODO: Handle case with duplicate CRN.
            AddCourseDialogFragment().show(childFragmentManager, AddCourseDialogFragment.TAG)
        }

        // Observe courses from DataStore
        viewLifecycleOwner.lifecycleScope.launch {
            coursesViewModel.courses.collect { courses ->
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
                    findNavController().navigate(SelectCourseFragmentDirections.actionSelectCourseFragmentToAddStudentFragment(course.crn))
                }

                root.setOnLongClickListener {
                    // TODO: Require confirmation
                    coursesViewModel.removeCourse(course.crn)
                    true
                }
            }
            courseCards.add(this)
            binding.coursesContainer.addView(this.root)
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