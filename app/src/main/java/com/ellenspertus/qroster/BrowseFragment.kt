package com.ellenspertus.qroster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.ellenspertus.qroster.databinding.FragmentBrowseBinding
import com.google.android.material.snackbar.Snackbar

class BrowseFragment : AbstractStudentFragment() {
    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!

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

        // Call methods defined in superclass.
        enableSwiping()

        // Call local methods.
        setupToggleButtons()
    }

    override fun createAdapter(): StudentPagerAdapter {
        val host = object : StudentPagerAdapter.Host {
            override val showInfoAtStart = true
            override val showInfoButtonAtStart = false
            override val showQuizButtons = false
            override val context = requireContext()
            override fun startOver() {
                view?.let { v ->
                    Snackbar.make(v, "Starting over with first student", Snackbar.LENGTH_SHORT)
                        .show()
                }
                binding.studentViewPager.setCurrentItem(0, true)
            }

            override fun onQuizButtonPressed(id: Int) {
                throw IllegalStateException("onQuizButtonPressed() should never be called in BrowseFragment")
            }
        }

        return StudentPagerAdapter(requireContext(), viewModel, this, host)
    }

    override fun provideBindings() =
        Bindings(
            binding.studentViewPager,
            binding.progressBar,
            binding.progressTextView,
            binding.swipeRefreshLayout,
            binding.emptyStateLayout,
        )

    private fun setupToggleButtons() {
        binding.modeToggle.modeToggleGroup.apply {
            check(R.id.browseButton)
            addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked && checkedId == R.id.quizButton) {
                    val action = BrowseFragmentDirections.actionBrowseFragmentToQuizFragment(crn)
                    findNavController().navigate(action)
                }
            }
        }
    }

    companion object {
        const val TAG = "BrowseFragment"
    }
}