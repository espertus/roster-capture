package com.ellenspertus.rostercapture.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.ellenspertus.rostercapture.R
import com.ellenspertus.rostercapture.courses.SelectCourseFragment.Companion.COURSE_ADDED_KEY
import com.ellenspertus.rostercapture.courses.SelectCourseFragment.Companion.COURSE_COUNT_KEY
import com.ellenspertus.rostercapture.databinding.DialogAddCourseBinding

class AddCourseDialogFragment() : DialogFragment() {
    private val coursesViewModel: CoursesViewModel by activityViewModels()
    private var _binding: DialogAddCourseBinding? = null
    private val binding get() = _binding!!
    private val courses
        get() = coursesViewModel.courses.value

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddCourseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Enable Add button only when other fields are valid.
        binding.saveButton.setOnClickListener {
            handleAddCourse()
        }
        binding.saveButton.isEnabled = false
        setupTextChangeListeners()

        // The Cancel button is always enabled.
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setupTextChangeListeners() {
        val changeListener = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                validateInputs()
            }
        }

        binding.crnEditText.addTextChangedListener(changeListener)
        binding.courseIdEditText.addTextChangedListener(changeListener)
        binding.courseNameEditText.addTextChangedListener(changeListener)
    }

    private fun validateInputs() {
        val crn = binding.crnEditText.text.toString().trim()
        val id = binding.courseIdEditText.text.toString().trim()
        val name = binding.courseNameEditText.text.toString().trim()

        val isDuplicateCrn = courses.any { it.crn == crn }
        binding.crnInputLayout.error = if (crn.isNotEmpty() && isDuplicateCrn) {
            getString(R.string.duplicate_crn_error)
        } else {
            null
        }

        // Enable Add button only if all fields are filled and CRN is unique.
        binding.saveButton.isEnabled = crn.isNotEmpty() &&
                id.isNotEmpty() &&
                name.isNotEmpty() &&
                !isDuplicateCrn
    }

    private fun handleAddCourse() {
        val course = Course(
            crn = binding.crnEditText.text.toString().trim(),
            id = binding.courseIdEditText.text.toString().trim(),
            name = binding.courseNameEditText.text.toString().trim()
        )
        setFragmentResult(
            COURSE_ADDED_KEY,
            bundleOf(COURSE_COUNT_KEY to courses.size + 1)
        )
        coursesViewModel.addCourse(course)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddCourseDialog"
    }
}