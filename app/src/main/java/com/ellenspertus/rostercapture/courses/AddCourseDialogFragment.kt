package com.ellenspertus.rostercapture.courses

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.ellenspertus.rostercapture.databinding.DialogAddCourseBinding

class AddCourseDialogFragment(private val courses: List<Course>) : DialogFragment() {

    private val coursesViewModel: CoursesViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogAddCourseBinding.inflate(layoutInflater)

        return AlertDialog.Builder(requireContext())
            .setTitle("Add Course")
            .setView(binding.root)
            .setPositiveButton("Add") { _, _ ->
                val crn = binding.crnEditText.text.toString().trim()
                val id = binding.courseIdEditText.text.toString().trim()
                val name = binding.courseNameEditText.text.toString().trim()

                if (crn.isNotEmpty() && id.isNotEmpty() && name.isNotEmpty()) {
                    if (courses.any { it.crn == crn }) {
                        Toast.makeText(
                            requireContext(),
                            "A course with CRN $crn already exists",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        coursesViewModel.addCourse(
                            Course(crn = crn, id = id, name = name)
                        )
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please fill in all fields",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
    }

    companion object {
        const val TAG = "AddCourseDialog"
    }
}