package com.ellenspertus.qroster

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.MarginPageTransformer
import com.ellenspertus.qroster.databinding.FragmentBrowseBinding
import com.google.android.material.snackbar.Snackbar

class BrowseFragment : Fragment() {
    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!
    private lateinit var studentAdapter: StudentPagerAdapter
    private lateinit var crn: String

    private val viewModel: StudentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            crn = BrowseFragmentArgs.fromBundle(it).crn
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        enableSwiping()
        setupViewPager()
        setupButtons()
        setupObservers()
        refreshData()

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
    }

    private fun enableSwiping() {
        binding.studentViewPager.isUserInputEnabled = true
    }

    private fun setupViewPager() {
        val host = object : StudentPagerAdapter.Host {
            override val showInfoAtStart = true
            override val showInfoButtonAtStart = false
            override val context = requireContext()
            override fun startOver() {
                view?.let { v ->
                    Snackbar.make(v, "Starting over with first student", Snackbar.LENGTH_SHORT).show()
                }
                binding.studentViewPager.setCurrentItem(0, true)
            }

            override fun onInfoVisibilityChanged(isVisible: Boolean) {
                // do nothing
            }
        }

        studentAdapter = StudentPagerAdapter(requireContext(), viewModel, this, host)
        binding.studentViewPager.apply {
            adapter = studentAdapter
            setPageTransformer(MarginPageTransformer(40))
        }
    }

    private fun setupButtons() {
        binding.quizButton.setOnClickListener { _ ->
            val action = BrowseFragmentDirections.actionBrowseFragmentToQuizFragment(crn)
            findNavController().navigate(action)
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
                studentAdapter.setStudents(students)
            }
        }

        viewModel.uiMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                when (it) {
                    is UiMessage.Success -> Toast.makeText(context, it.text, Toast.LENGTH_SHORT).show()
                    is UiMessage.Failure -> Toast.makeText(context, it.text, Toast.LENGTH_LONG).show()
                }
                viewModel.clearMessage()
            }
        }
    }

    private fun refreshData() {
        initializeUi()
        loadStudents()
    }

    private fun loadStudents() {
        viewModel.loadStudentsForCourse(crn)
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

    companion object {
        const val TAG = "BrowseFragment"
    }
}