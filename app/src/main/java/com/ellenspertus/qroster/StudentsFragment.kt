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
import com.google.android.material.snackbar.Snackbar

class StudentsFragment : Fragment() {
    private var _binding: FragmentStudentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var studentAdapter: StudentPagerAdapter
    private var crn: String? = null

    private val viewModel: StudentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            crn = StudentsFragmentArgs.fromBundle(it).crn
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupObservers()
        refreshData()

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
    }

    private fun setupViewPager() {
        studentAdapter = StudentPagerAdapter(requireContext(), viewModel, this)
        binding.studentViewPager.apply {
            adapter = studentAdapter
            setPageTransformer(MarginPageTransformer(40))
        }
    }

    private fun setupObservers() {
        viewModel.students.observe(viewLifecycleOwner) { students ->
            // Hide loading indicators
            binding.progressBar.visibility = View.GONE
            binding.progressTextView.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false

            if (students.isEmpty()) {
                binding.studentViewPager.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.studentViewPager.visibility = View.VISIBLE
                binding.emptyStateLayout.visibility = View.GONE
                studentAdapter.submitList(students)
            }
        }
    }

    private fun refreshData() {
        initializeUi()
        loadStudents()
    }

    private fun loadStudents() {
        crn?.let {
            viewModel.loadStudentsForCourse(it)
        } ?: run {
            Log.e(TAG, "crn was null")
        }
    }

    private fun initializeUi() {
        // Indicate that data is loading.
        binding.apply {
            progressBar.visibility = View.VISIBLE
            progressTextView.visibility = View.VISIBLE
            studentViewPager.visibility = View.GONE
            emptyStateLayout.visibility = View.GONE
        }
    }

    fun showButtons() {
        binding.controlsLayout.visibility = View.VISIBLE
    }

    fun hideButtons() {
        binding.controlsLayout.visibility = View.INVISIBLE
    }

    fun startOver() {
        view?.let { v ->
            Snackbar.make(v, "Starting over with first student", Snackbar.LENGTH_SHORT).show()
        }
        binding.studentViewPager.setCurrentItem(0, true)

        // Optional: Reshuffle the students if you want variety
        // viewModel.reshuffleStudents()
    }

    companion object {
        const val TAG = "StudentsFragment"
    }
}