package com.ellenspertus.qroster

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.ellenspertus.qroster.databinding.ItemStartOverCardBinding
import com.ellenspertus.qroster.databinding.ItemStudentCardBinding
import com.ellenspertus.qroster.model.Student
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class StudentPagerAdapter(
    private val context: Context,
    private val viewModel: StudentViewModel,
    private val enclosingFragment: StudentsFragment,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val storageRef = FirebaseStorage.getInstance().reference
    private var students = emptyList<Student>()

    inner class StudentViewHolder(
        val binding: ItemStudentCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.apply {
                val student = students[position]
                nameTextView.text = student.displayName
                pronounsTextView.text = student.pronouns

                addEditNoteButton.tag = position
                deleteNoteButton.tag = position

                setupNoteDisplay(student, this)
                addSelfieIfPresent(student, this)

//                // For API 34+, you can use Coil with better performance
//                // implementation("io.coil-kt:coil:2.6.0")
//                studentPhoto.load(student.photoUrl) {
//                    crossfade(true)
//                    error(R.drawable.ic_person_placeholder)
//                }

                student.audioFile?.let {
                    playButton.visibility = View.VISIBLE
                    playButton.setOnClickListener {
                        viewModel.playAudio(root.context, student)
                    }
//                        // Haptic feedback for API 34+
//                        it.performHapticFeedback(
//                            HapticFeedbackConstants.CONFIRM,
//                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
//                        )
                } ?: run {
                    playButton.visibility = View.GONE
                }
            }
        }
    }

    inner class StartOverViewHolder(binding: ItemStartOverCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (position < students.size) {
            VIEW_TYPE_STUDENT
        } else {
            VIEW_TYPE_START_OVER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_STUDENT -> {
                val binding = ItemStudentCardBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                setupStudentCard(binding)
                StudentViewHolder(binding)
            }

            VIEW_TYPE_START_OVER -> {
                val binding = ItemStartOverCardBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                setupStartOverCard(binding)
                StartOverViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    private fun setupStudentCard(binding: ItemStudentCardBinding) {
        with(binding) {
            showInfoButton.let {
                it.visibility = View.VISIBLE
                it.setOnClickListener { _ ->
                    it.visibility = View.GONE
                    studentInfoContainer.visibility = View.VISIBLE
                    enclosingFragment.showButtons()
                }
            }

            addEditNoteButton.setOnClickListener {
                val position = it.tag as? Int ?: return@setOnClickListener
                showAddNoteDialog(students[position], position)
            }

            noteIndicator.setOnClickListener {
                showInfoButton.visibility = View.GONE
                studentInfoContainer.visibility = View.VISIBLE
                enclosingFragment.showButtons()
            }
        }
    }

    // Update onBindViewHolder to set up note display
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is StudentViewHolder -> {
                holder.bind(position)

                    // Set the position as a tag so we can access it in click listeners
                    holder.binding.addEditNoteButton.tag = position
                    holder.binding.deleteNoteButton.tag = position
                    val student = students[position]
                    setupNoteDisplay(student, holder.binding)
                    addSelfieIfPresent(student, holder.binding)
//                    addAudioIfPresent(student, holder.binding, position)
            }
            is StartOverViewHolder -> {
            }
        }
    }

    private fun setupNoteDisplay(student: Student, binding: ItemStudentCardBinding) {
        with(binding) {
            if (student.note.isNullOrEmpty()) {
                // No note exists
                noteIndicator.visibility = View.GONE
                notePreview.text = context.getString(R.string.no_notes_yet)
                addEditNoteButton.text = context.getString(R.string.add_note)
                deleteNoteButton.visibility = View.GONE
            } else {
                // Note exists
                noteIndicator.visibility = View.VISIBLE
                notePreview.text = student.note
                addEditNoteButton.text = context.getString(R.string.edit_note)
                deleteNoteButton.visibility = View.VISIBLE
                deleteNoteButton.setOnClickListener { _ ->
                    deleteNoteForStudent(student)
                    student.note = null
                    notifyItemChanged(deleteNoteButton.tag as Int)
                }
            }
        }
    }

    private fun showAddNoteDialog(student: Student, position: Int) {
        // Inflate the dialog layout
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
                    student.note = noteText
                    if (noteText.isNotEmpty()) {
                        saveNoteForStudent(student)
                    } else {
                        deleteNoteForStudent(student)
                    }
                    notifyItemChanged(position)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        noteEditText.requestFocus()
    }

    private fun saveNoteForStudent(student: Student) {
        FirebaseFirestore.getInstance()
            .collection(STUDENTS_COLLECTION)
            .document(student.docId)
            .update("note", student.note)
            .addOnSuccessListener {
                Log.d(TAG, "Saved note for ${student.displayName}: ${student.note}")
                Toast.makeText(context, "Note saved for ${student.displayName}", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Unable to save note: $e")
            }
    }

    private fun deleteNoteForStudent(student: Student) {
        FirebaseFirestore.getInstance()
            .collection(STUDENTS_COLLECTION)
            .document(student.docId)
            .update("note", FieldValue.delete())
            .addOnSuccessListener {
                Log.d(TAG, "Deleted note for ${student.displayName}")
                Toast.makeText(context, "Note deleted for ${student.displayName}", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Unable to delete note: $e")
            }
    }

    private fun setupStartOverCard(binding: ItemStartOverCardBinding) {
        binding.startOverButton.setOnClickListener {
            enclosingFragment.startOver()
        }
        binding.doneButton.setOnClickListener {
            enclosingFragment.findNavController()
                .navigate(R.id.action_studentsFragment_to_selectCourseFragment)
        }
    }

    // When we show a view, whether for the first time or later times,
    // we should not display the info or the mark buttons.
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)

        if (holder is StudentViewHolder) {
            with(holder.binding) {
                showInfoButton.visibility = View.VISIBLE
                studentInfoContainer.visibility = View.GONE
            }
        }
        enclosingFragment.hideButtons()
    }

    private fun addSelfieIfPresent(student: Student, binding: ItemStudentCardBinding) {
        if (student.selfieFile == null) {
            binding.studentImageView.setImageResource(R.drawable.missing_profile)
            binding.imageProgressBar.visibility = View.GONE
        } else {
            val storagePath = getStoragePath(student.selfieFile)
            storageRef.child(storagePath).downloadUrl.addOnSuccessListener { uri ->
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
        val count = students.size + 1 // start over card
        return count
    }

    fun submitList(newStudents: List<Student>) {
        students = newStudents
        notifyDataSetChanged()
    }

    companion object {
        const val TAG = "StudentPagerAdapter"
        const val VIEW_TYPE_STUDENT = 0
        const val VIEW_TYPE_START_OVER = 1

        fun getStoragePath(file: String) =
            when {
                file.startsWith("gs://") -> {
                    // Extract the path after the bucket name
                    // Format: gs://bucket-name/path/to/file.jpg
                    val uri = file.removePrefix("gs://")
                    val parts = uri.split("/", limit = 2)
                    if (parts.size > 1) parts[1] else file
                }

                else -> file
            }
    }
}