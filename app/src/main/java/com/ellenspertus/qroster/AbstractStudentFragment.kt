package com.ellenspertus.qroster

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2

abstract class AbstractStudentFragment : Fragment() {
    // initialized in onCreate()
    protected lateinit var crn: String

    // initialized in onViewCreated()
    protected lateinit var studentPagerAdapter: StudentPagerAdapter

    protected val viewModel: StudentViewModel by viewModels()

    data class Bindings(
        val studentViewPager: ViewPager2,
        val progressBar: View,
        val progressTextView: View,
        val swipeRefreshLayout: SwipeRefreshLayout?,
        val emptyStateLayout: View,
    )

    abstract fun createAdapter(): StudentPagerAdapter
    abstract fun provideBindings(): Bindings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            crn = BrowseFragmentArgs.fromBundle(it).crn
        } ?: throw IllegalStateException("No CRN passed to AbstractStudentFragment")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        studentPagerAdapter = createAdapter()
        val bindings = provideBindings()
        setupStudentViewPager(bindings)
        observeUiMessage()
        observeStudents(bindings)
        setupBackButton()

        bindings.swipeRefreshLayout?.setOnRefreshListener {
            loadData(bindings)
        }
        loadData(bindings)
    }

    private fun setupStudentViewPager(bindings: Bindings) {
        bindings.studentViewPager.apply {
            adapter = studentPagerAdapter
            setPageTransformer(MarginPageTransformer(40))
        }
    }

    private fun observeUiMessage() {
        viewModel.uiMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                when (it) {
                    is UiMessage.Success -> Toast.makeText(context, it.text, Toast.LENGTH_SHORT)
                        .show()

                    is UiMessage.Failure -> Toast.makeText(context, it.text, Toast.LENGTH_LONG)
                        .show()
                }
                viewModel.clearMessage()
            }
        }
    }

    private fun observeStudents(bindings: Bindings) {
        viewModel.students.observe(viewLifecycleOwner) { students ->
            bindings.progressBar.visibility = View.GONE
            bindings.progressTextView.visibility = View.GONE
            bindings.swipeRefreshLayout?.isRefreshing = false

            if (students.isEmpty()) {
                bindings.studentViewPager.visibility = View.GONE
                bindings.emptyStateLayout.visibility = View.VISIBLE
            } else {
                bindings.studentViewPager.visibility = View.VISIBLE
                bindings.emptyStateLayout.visibility = View.GONE
                studentPagerAdapter.setStudents(students)
            }
        }
    }

    protected fun showDataLoading(bindings: Bindings) {
        bindings.apply {
            progressBar.visibility = View.VISIBLE
            progressTextView.visibility = View.VISIBLE
            studentViewPager.visibility = View.GONE
            emptyStateLayout.visibility = View.GONE
        }
    }

    private fun setupBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.selectCourseFragment)
                }
            }
        )
    }

    private fun loadData(bindings: Bindings) {
        showDataLoading(bindings)
        viewModel.loadStudentsForCourse(crn)
    }

    // Utility functions for subclasses
    protected fun enableSwiping(studentViewPager: ViewPager2) {
        studentViewPager.isUserInputEnabled = true
    }

    protected fun disableSwiping(studentViewPager: ViewPager2) {
        studentViewPager.isUserInputEnabled = false
    }

}