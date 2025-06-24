package com.ellenspertus.qroster.courses

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ellenspertus.qroster.R
import com.ellenspertus.qroster.databinding.FragmentSelectCourseBinding
import com.ellenspertus.qroster.databinding.ItemCourseCardBinding
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

        viewLifecycleOwner.lifecycleScope.launch {
            coursesViewModel.courses.collect { courses ->
                binding.coursesContainer.removeAllViews()
                courseCards.clear()

                binding.fabAddCourse.setOnClickListener {
                    AddCourseDialogFragment(courses).show(childFragmentManager, AddCourseDialogFragment.TAG)
                }
                if (courses.isEmpty()) {
                    binding.textWelcome.text = getString(R.string.no_courses_found)
                    binding.textInstruct.visibility = View.VISIBLE
                } else {
                    solicitCourse(courses.sortedBy { it.id })
                }
            }
        }
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
            binding.textInstruct.visibility = View.INVISIBLE
            course.let {
                courseIdText.text = it.id
                courseNameText.text = it.name
                courseCrn.text = it.crn

                root.setOnClickListener { view ->
                    findNavController().navigate(SelectCourseFragmentDirections.actionSelectCourseFragmentToAddStudentFragment(course.crn))
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

    private fun promptForConfirmation(message: String, action: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> action() }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun promptToRemoveCourse(course: Course) {
        promptForConfirmation("Do you really want to delete ${course.name}?") {
            coursesViewModel.removeCourse(course.crn)
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