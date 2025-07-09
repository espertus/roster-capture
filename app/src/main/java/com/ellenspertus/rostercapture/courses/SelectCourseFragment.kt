package com.ellenspertus.rostercapture.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ellenspertus.rostercapture.R
import com.ellenspertus.rostercapture.databinding.FragmentSelectCourseBinding
import com.ellenspertus.rostercapture.databinding.ItemCourseCardBinding
import com.ellenspertus.rostercapture.extensions.navigateSafe
import com.ellenspertus.rostercapture.extensions.promptForConfirmation
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

        // Whenever a first course is added, display this tip.
        childFragmentManager.setFragmentResultListener(
            COURSE_ADDED_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            // We can't get the size from the viewModel, which might not have been updated.
            if (bundle.getInt(COURSE_COUNT_KEY) == 1) {
                showDeleteTip()
            } else {
                hideDeleteTip()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            coursesViewModel.courses.collect { courses ->
                binding.coursesContainer.removeAllViews()
                courseCards.clear()

                binding.fabAddCourse.setOnClickListener {
                    AddCourseDialogFragment().show(
                        childFragmentManager,
                        AddCourseDialogFragment.TAG
                    )
                }
                if (courses.isEmpty()) {
                    binding.textWelcome.text = getString(R.string.no_courses_found)
                    showCreateTip()
                    hideDeleteTip()
                } else {
                    if (courses.size != 1) {
                        hideDeleteTip()
                    }
                    solicitCourse(courses.sortedBy { it.id })
                }
            }
        }
        binding.settingsButton.setOnClickListener {
            findNavController().navigateSafe(
                SelectCourseFragmentDirections.actionSelectCourseFragmentToFieldConfigFragment()
            )
        }
    }

    private fun showDeleteTip() {
        binding.textDeleteCourse.apply {
            visibility = View.VISIBLE
        }
    }

    private fun showCreateTip() {
        binding.textCreateCourse.apply {
            visibility = View.VISIBLE
        }
    }

    private fun hideDeleteTip() {
        binding.textDeleteCourse.visibility = View.GONE
    }

    private fun hideCreateTip() {
        binding.textCreateCourse.visibility = View.GONE
    }

    private fun solicitCourse(courses: List<Course>) {
        binding.apply {
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
            hideCreateTip()
            course.let {
                courseIdText.text = it.id
                courseNameText.text = it.name
                courseCrn.text = it.crn

                root.setOnClickListener { view ->
                    findNavController().navigateSafe(
                        SelectCourseFragmentDirections.actionSelectCourseFragmentToAddStudentFragment(
                            course
                        )
                    )
                }

                root.setOnLongClickListener {
                    promptToRemoveCourse(course)
                    true
                }
            }
            courseCards.add(this)
            binding.coursesContainer.addView(this.root)
        }
    }

    private fun promptToRemoveCourse(course: Course) {
        requireContext().promptForConfirmation(
            getString(
                R.string.delete_course_confirmation,
                course.name
            )
        ) {
            coursesViewModel.removeCourse(course.crn)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val COURSE_ADDED_KEY = "courseAdded"
        const val COURSE_COUNT_KEY = "courseCount"
    }
}