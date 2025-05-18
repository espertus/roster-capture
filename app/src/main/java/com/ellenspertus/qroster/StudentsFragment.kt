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
                val adapter = StudentPagerAdapter(requireContext(), students)
                studentViewPager.adapter = adapter

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