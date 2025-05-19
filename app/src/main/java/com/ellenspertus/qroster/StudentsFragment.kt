package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.MarginPageTransformer
import com.ellenspertus.qroster.databinding.FragmentStudentsBinding

class StudentsFragment() : Fragment() {
    private var _binding: FragmentStudentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var studentAdapter: StudentPagerAdapter

    private val viewModel: StudentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStudentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Add refresh listener
        binding.swipeRefreshLayout.setOnRefreshListener {
            displayStudents()
        }

        // Observe students LiveData
        viewModel.students.observe(viewLifecycleOwner) { students ->
            // Stop the refreshing animation
            binding.swipeRefreshLayout.isRefreshing = false

            if (students.isEmpty()) {
                binding.studentViewPager.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.studentViewPager.visibility = View.VISIBLE
                binding.emptyStateLayout.visibility = View.GONE

                // Update adapter
                studentAdapter = StudentPagerAdapter(requireContext(), students, this)
                binding.studentViewPager.adapter = studentAdapter
            }

            binding.progressBar.visibility = View.GONE
            binding.progressTextView.visibility = View.GONE
        }

        displayStudents()
    }

    fun showButtons() {
        binding.markAsKnownButton.visibility = View.VISIBLE
        binding.markAsNotKnownButton.visibility = View.VISIBLE
    }

    fun hideButtons() {
        binding.markAsKnownButton.visibility = View.INVISIBLE
        binding.markAsNotKnownButton.visibility = View.INVISIBLE
    }

    private fun displayStudents() {
        val progressBar = binding.progressBar
        val progressTextView = binding.progressTextView
        val studentViewPager = binding.studentViewPager
        val emptyStateLayout = binding.emptyStateLayout

        // Initial state - show loading
        progressBar.visibility = View.VISIBLE
        progressTextView.visibility = View.VISIBLE
        studentViewPager.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE

        viewModel.students.observe(viewLifecycleOwner) { students ->
            progressBar.visibility = View.GONE
            progressTextView.visibility = View.GONE

            if (students.isEmpty()) {
                studentViewPager.visibility = View.GONE
                emptyStateLayout.visibility = View.VISIBLE
            } else {
                studentViewPager.visibility = View.VISIBLE
                emptyStateLayout.visibility = View.GONE

                // Setup adapter and viewpager
                studentAdapter = StudentPagerAdapter(requireContext(), students, this)
                studentViewPager.adapter = studentAdapter

                // Optional: add page transformer for nice effects
                studentViewPager.setPageTransformer(MarginPageTransformer(40))
            }
        }

        val args = arguments?.let { StudentsFragmentArgs.fromBundle(it) }
        val crn = args?.crn
        if (crn != null) {
            viewModel.loadStudentsForCourse(crn)
        } else {
            Log.e(TAG, "crn was null")
        }
    }

    companion object {
        const val TAG = "StudentsFragment"
    }
}