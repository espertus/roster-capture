package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.MarginPageTransformer
import com.ellenspertus.qroster.databinding.FragmentQuizBinding
import com.ellenspertus.qroster.model.Student
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class QuizFragment : Fragment() {
    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private val quizButtons = mutableListOf<MaterialButton>()
    private lateinit var studentAdapter: StudentPagerAdapter
    private lateinit var crn: String

    private val viewModel: StudentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            crn = QuizFragmentArgs.fromBundle(it).crn
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeQuizButtons()
        disableSwiping()
        setupViewPager()
        setupObservers()
        setupData()
    }

    private fun initializeQuizButtons() {
        binding.apply {
            quizButtons.addAll(
                listOf(quizButton1, quizButton2, quizButton3, quizButton4)
            )
        }
        quizButtons.forEach {
            it.setOnClickListener(::incorporateFeedback)
        }
    }

    private fun disableSwiping() {
        binding.studentViewPager.isUserInputEnabled = false
    }

    private fun setGuessButtonsEnabled(enabled: Boolean) {
        quizButtons.forEach { it.isEnabled = enabled}
    }

    private fun setupViewPager() {
        val host = object : StudentPagerAdapter.Host {
            override val showInfoAtStart = false
            override val showInfoButtonAtStart = true
            override val context = requireContext()
            override fun startOver() {
                view?.let { v ->
                    Snackbar.make(v, "Starting over with first student", Snackbar.LENGTH_SHORT)
                        .show()
                }
                binding.studentViewPager.setCurrentItem(0, true)
            }

            override fun onInfoVisibilityChanged(isVisible: Boolean) {
                setGuessButtonsEnabled(isVisible)
            }
        }

        studentAdapter = StudentPagerAdapter(requireContext(), viewModel, this, host)
        binding.studentViewPager.apply {
            adapter = studentAdapter
            setPageTransformer(MarginPageTransformer(40))
        }
    }

    private fun setupObservers() {
        // TODO: Should this happen on the first time only?
        viewModel.students.observe(viewLifecycleOwner) { studentsList ->
            // Hide loading indicators
            binding.progressBar.visibility = View.GONE
            binding.progressTextView.visibility = View.GONE
            binding.swipeRefreshLayout.isRefreshing = false

            if (studentsList.isEmpty()) {
                binding.studentViewPager.visibility = View.GONE
                binding.emptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.studentViewPager.visibility = View.VISIBLE
                binding.emptyStateLayout.visibility = View.GONE
                createStudentQueue(studentsList)
            }
        }
    }

    private fun createStudentQueue(studentList: List<Student>) {
        val quizStudents = studentList.filter { it.score < 1 }.shuffled()
        studentAdapter.setStudents(quizStudents)
    }

    private fun setupData() {
        initializeUi()
        loadStudents()
    }

    private fun initializeUi() {
        binding.apply {
            // Indicate that data is loading.
            progressBar.visibility = View.VISIBLE
            progressTextView.visibility = View.VISIBLE
            studentViewPager.visibility = View.GONE
            emptyStateLayout.visibility = View.GONE

            // Bind buttons.
            quizButton1.setOnClickListener(::incorporateFeedback)
        }
        setupToggleButtons()
    }

    private fun setupToggleButtons() {
        binding.modeToggle.modeToggleGroup.apply {
            check(R.id.quizButton)
            addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked && checkedId == R.id.browseButton) {
                    val action = QuizFragmentDirections.actionQuizFragmentToBrowseFragment(crn)
                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun loadStudents() {
        viewModel.loadStudentsForCourse(crn)
    }

    fun incorporateFeedback(view: View) {
        val score = difficultyMap[view.id]
        if (score == null) {
            Log.e(TAG, "Illegal view in incorporateFeedback()")
        } else {
            val students = studentAdapter.getStudents()
            students.getOrNull(0)?.also {
                Log.d(TAG, "Score $score for student $it")
                val newScore = PRIOR_WEIGHT * it.score + (1.0 - PRIOR_WEIGHT) * score
                viewModel.updateStudentScore(it, newScore)
                moveStudent(it)
            }.run {
                Log.e(TAG, "Unable to retrieve student")
            }
        }
    }

    private fun moveStudent(student: Student) {
        val students = studentAdapter.getStudents()
        require(students.isNotEmpty() && students[0] == student)

        studentAdapter.notifyItemRemoved(0)
        if (student.score > QUIZ_THRESHOLD) {
            students.removeAt(0)
            studentAdapter.notifyItemRemoved(0)
        } else {
            val newPos = Math.min((students.size * student.score).toInt() + 1, students.size - 1)
            students.removeAt(0)
            students.add(newPos, student)
            studentAdapter.notifyItemInserted(newPos)
        }
    }

    companion object {
        const val TAG = "QuizFragment"
        const val PRIOR_WEIGHT = .7
        const val QUIZ_THRESHOLD = .9

        private val difficultyMap = mapOf(
            R.id.quizButton1 to 0.0,  // Again
            R.id.quizButton2 to .25,  // Hard
            R.id.quizButton3 to .75,  // Good
            R.id.quizButton4 to 1.0   // Easy
        )
    }
}