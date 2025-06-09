package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.ellenspertus.qroster.databinding.FragmentQuizBinding
import com.ellenspertus.qroster.databinding.ItemQuizEndCardBinding
import com.ellenspertus.qroster.model.Student
import kotlin.math.min

class QuizFragment : AbstractStudentFragment() {
    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        disableSwiping(binding.studentViewPager)
        setupToggleButtons()
    }

    override fun createAdapter(): StudentPagerAdapter {
        val host = object : StudentPagerAdapter.Host {
            override val showInfoAtStart = false
            override val showInfoButtonAtStart = true
            override val showQuizButtons = true

            override fun provideEndViewBinding(parent: ViewGroup) =
                ItemQuizEndCardBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ).also {
                    it.selectCourseButton.setOnClickListener {
                        findNavController()
                            .navigate(R.id.action_quizFragment_to_selectCourseFragment)

                    }
                }

            override fun onQuizChoiceButtonPressed(id: Int) {
                incorporateFeedback(id)
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

    override fun processStudents(studentList: List<Student>) {
        val quizStudents = studentList.filter { it.score < QUIZ_THRESHOLD }.shuffled()
        studentPagerAdapter.setStudents(quizStudents)
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

    fun incorporateFeedback(id: Int) {
        val score = difficultyMap[id]
        if (score == null) {
            Log.e(TAG, "Illegal view in incorporateFeedback()")
        } else {
            val students = studentPagerAdapter.getStudents()
            students.getOrNull(0)?.also {
                Log.d(TAG, "Score $score for student $it")
                val newScore = PRIOR_WEIGHT * it.score + (1.0 - PRIOR_WEIGHT) * score
                viewModel.updateStudentScore(it, newScore)
                moveStudent(it)
            } ?: {
                Log.e(TAG, "Unable to retrieve student")
            }
        }
    }

    private fun moveStudent(student: Student) {
        val students = studentPagerAdapter.getStudents()
        require(students.isNotEmpty() && students[0] == student)

        studentPagerAdapter.notifyItemRemoved(0)
        if (student.score > QUIZ_THRESHOLD) {
            students.removeAt(0)
            studentPagerAdapter.notifyItemRemoved(0)
        } else {
            val newPos = min((students.size * student.score).toInt() + 1, students.size - 1)
            students.removeAt(0)
            students.add(newPos, student)
            studentPagerAdapter.notifyItemInserted(newPos)
        }
    }

    companion object {
        const val TAG = "QuizFragment"
        const val PRIOR_WEIGHT = .6
        const val QUIZ_THRESHOLD = .9

        private val difficultyMap = mapOf(
            R.id.quizButton1 to 0.0,  // Again
            R.id.quizButton2 to .25,  // Hard
            R.id.quizButton3 to .75,  // Good
            R.id.quizButton4 to 1.0   // Easy
        )
    }
}