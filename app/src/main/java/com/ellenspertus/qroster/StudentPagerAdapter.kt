package com.ellenspertus.qroster

import android.content.Context
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.ellenspertus.qroster.databinding.ItemStudentCardBinding
import com.ellenspertus.qroster.model.Student
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.storage.FirebaseStorage

class StudentPagerAdapter(
    private val context: Context,
    private val viewModel: StudentViewModel,
    private val enclosingFragment: Fragment,
    private val host: Host,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val storageRef = FirebaseStorage.getInstance().reference
    private val students = mutableListOf<Student>()
    private val quizButtonList = mutableListOf<MaterialButton>()

    interface Host {
        val showInfoAtStart: Boolean
        val showInfoButtonAtStart: Boolean
        // Quiz buttons are shown only when the info is displayed.
        val showQuizButtons: Boolean
        fun provideEndViewBinding(parent: ViewGroup): ViewBinding
        fun onQuizChoiceButtonPressed(id: Int)
    }

    inner class StudentViewHolder(
        val binding: ItemStudentCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.apply {
                val student = students[position]
                nameTextView.text = student.displayName
                pronounsTextView.text = student.pronouns

                setupNoteDisplay(student, this)
                addSelfieIfPresent(student, this)

                // Display the play button even if the audio file has not yet been loaded.
                student.audioPath?.let {
                    playButton.apply {
                        visibility = View.VISIBLE
                        setOnClickListener {
                            viewModel.playAudio(root.context, student)
                        }
                        performHapticFeedback(
                            HapticFeedbackConstants.CONFIRM,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                        )
                    }
                } ?: run {
                    playButton.visibility = View.GONE
                }

                if (host.showQuizButtons) {
                    quizButtonList.add(quizButton1)
                    quizButtonList.add(quizButton2)
                    quizButtonList.add(quizButton3)
                    quizButtonList.add(quizButton4)

                    quizButtonList.forEach {
                        it.setOnClickListener { view ->
                            host.onQuizChoiceButtonPressed(view.id)
                        }
                    }
                }
            }
        }
    }

    inner class EndCardViewHolder(binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (position < students.size) {
            VIEW_TYPE_STUDENT
        } else {
            VIEW_TYPE_END
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_STUDENT -> {
                val binding = ItemStudentCardBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                StudentViewHolder(binding)
            }

            VIEW_TYPE_END -> {
                EndCardViewHolder(host.provideEndViewBinding(parent))
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    fun setStudents(newStudents: List<Student>) {
        students.clear()
        students.addAll(newStudents)
        notifyDataSetChanged()
    }

    fun getStudents(): MutableList<Student> = students

    private fun setupStudentCard(student: Student, binding: ItemStudentCardBinding) {
        with(binding) {
            showInfoButton.let {
                it.visibility = View.VISIBLE
                it.setOnClickListener { _ ->
                    showInfo(this)
                }
            }

            addEditNoteButton.setOnClickListener {
                showAddNoteDialog(student)
            }

            noteIndicator.setOnClickListener {
                showInfoButton.visibility = View.GONE
                studentInfoContainer.visibility = View.VISIBLE
            }

            quizButtons.visibility = View.INVISIBLE
        }
    }

    private fun showInfo(binding: ItemStudentCardBinding) {
        binding.showInfoButton.visibility = View.GONE
        binding.studentInfoContainer.visibility = View.VISIBLE
        if (host.showQuizButtons) {
            binding.quizButtons.visibility = View.VISIBLE
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is StudentViewHolder -> {
                holder.bind(position)

                val student = students[position]
                setupStudentCard(student, holder.binding)
                setupNoteDisplay(student, holder.binding)
                addSelfieIfPresent(student, holder.binding)
            }

            is EndCardViewHolder -> {
            }
        }
    }

    private fun setupNoteDisplay(student: Student, binding: ItemStudentCardBinding) {
        with(binding) {
            if (student.note.isNullOrEmpty()) {
                // No note exists
                noteIndicator.visibility = View.GONE
                notePreview.visibility = View.GONE
                addEditNoteButton.text = context.getString(R.string.add_note)
                deleteNoteButton.visibility = View.GONE
            } else {
                // Note exists
                noteIndicator.visibility = View.VISIBLE
                notePreview.visibility = View.VISIBLE
                notePreview.text = student.note
                addEditNoteButton.text = context.getString(R.string.edit_note)
                deleteNoteButton.visibility = View.VISIBLE
                deleteNoteButton.setOnClickListener { _ ->
                    viewModel.deleteStudentNote(student)
                }
            }
        }
    }

    private fun showAddNoteDialog(student: Student) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_note, null)
        val noteEditText = dialogView.findViewById<TextInputEditText>(R.id.noteEditText)
        student.note?.let {
            noteEditText.setText(it)
            noteEditText.setSelection(it.length)
        }

        // Create and show the dialog
        MaterialAlertDialogBuilder(context)
            .setTitle("Note for ${student.displayName}")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val noteText = noteEditText.text.toString().trim()

                if (noteText != student.note) {
                    if (noteText.isNotEmpty()) {
                        viewModel.updateStudentNote(student, noteText)
                    } else {
                        viewModel.deleteStudentNote(student)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        noteEditText.requestFocus()
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)

        if (holder is StudentViewHolder) {
            initializeInfo(holder.binding)
        }
    }

    private fun initializeInfo(binding: ItemStudentCardBinding) {
        require(!(host.showInfoButtonAtStart && host.showInfoAtStart))
        if (host.showInfoAtStart) {
            showInfo(binding)
        } else {
            binding.studentInfoContainer.visibility = View.GONE
        }
    }

    private fun addSelfieIfPresent(student: Student, binding: ItemStudentCardBinding) {
        if (student.selfiePath == null) {
            binding.studentImageView.setImageResource(R.drawable.missing_profile)
            binding.imageProgressBar.visibility = View.GONE
        } else {
            storageRef.child(student.selfiePath).downloadUrl.addOnSuccessListener { uri ->
                Glide.with(context)
                    .load(uri)
                    .placeholder(R.drawable.placeholder_profile)
                    .error(R.drawable.missing_profile)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.studentImageView)
                binding.imageProgressBar.visibility = View.GONE
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to load image for ${student.displayName}", e)
                binding.studentImageView.setImageResource(R.drawable.error_face)
                binding.imageProgressBar.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        val count = students.size + 1 // end card
        return count
    }

    companion object {
        const val TAG = "StudentPagerAdapter"
        const val VIEW_TYPE_STUDENT = 0
        const val VIEW_TYPE_END = 1
    }
}