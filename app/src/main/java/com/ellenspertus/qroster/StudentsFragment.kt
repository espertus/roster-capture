package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.MarginPageTransformer
import com.ellenspertus.qroster.databinding.FragmentStudentsBinding
import com.ellenspertus.qroster.model.Student
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StudentsFragment() : Fragment() {
    private var _binding: FragmentStudentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentViewModel by viewModels()
    private var adapter: StudentPagerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       _binding = FragmentStudentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        val studentViewPager = binding.studentViewPager
        val progressBar = binding.progressBar
        val emptyStateLayout = binding.emptyStateLayout

        // Setup initial state
        progressBar.visibility = View.VISIBLE
        studentViewPager.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE

        // Observe student data from ViewModel
        viewModel.students.observe(viewLifecycleOwner) { students ->
            progressBar.visibility = View.GONE

            if (students.isEmpty()) {
                // Show empty state
                emptyStateLayout.visibility = View.VISIBLE
                studentViewPager.visibility = View.GONE
            } else {
                // Show students
                emptyStateLayout.visibility = View.GONE
                studentViewPager.visibility = View.VISIBLE

                // Create adapter with students
                adapter = StudentPagerAdapter(requireContext(), students)
                studentViewPager.adapter = adapter

                // Add nice page transitions
                studentViewPager.setPageTransformer(MarginPageTransformer(40))

            }
        }

        // Load students for the course
        val args = arguments?.let { StudentsFragmentArgs.fromBundle(it) }
        val crn = args?.crn
        if (crn != null) {
            Log.d(tag, "About to load students with crn $crn")
            viewModel.loadStudentsForCourse(crn)
        }
    }

//    private suspend fun retrieveStudents():  = coroutineScope {
//        try {
//            val snapshot = db.collection(STUDENTS_COLLECTION)
//                .get()
//                .await() // This suspends until the task completes and returns the QuerySnapshot
//
//            // Process the result after await() completes
//            students = snapshot.documents.mapNotNull { document ->
//                try {
//                    document.toObject(Student::class.java)
//                } catch (e: Exception) {
//                    Log.e(SelectCourseFragment.TAG, "Error converting document ${document.id}", e)
//                    null
//                }
//            }
//        } catch (exception: Exception) {
//            Log.e(SelectCourseFragment.TAG, "Error getting documents", exception)
//        }
//    }

    companion object {
        const val TAG = "StudentsFragment"
        const val STUDENTS_COLLECTION = "students"
    }
}