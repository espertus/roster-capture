package com.ellenspertus.qroster

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
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
    private val students: List<Student>,
    private val enclosingFragment: StudentsFragment,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val storageRef = FirebaseStorage.getInstance().reference
    private var mediaPlayer = MediaPlayer()
    private var currentPlayingPosition = 0

    // Map to store preloaded audio URLs
    private val audioUrls = mutableMapOf<Int, String>()

    inner class StudentViewHolder(val binding: ItemStudentCardBinding) :
        RecyclerView.ViewHolder(binding.root)

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
                val student = students[position]
                with(holder.binding) {
                    nameTextView.text = student.displayName
                    pronounsTextView.text = student.pronouns

                    // Set the position as a tag so we can access it in click listeners
                    addEditNoteButton.tag = position
                    deleteNoteButton.tag = position

                    setupNoteDisplay(student, this)
                    addSelfieIfPresent(student, this)
                    addAudioIfPresent(student, this, position)
                }
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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        initializeMediaPlayer()
    }

    private fun initializeMediaPlayer() {
        try {
            // Release any existing instance first
            mediaPlayer.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
            // This can happen if the MediaPlayer is in an invalid state
        }

        // Create a fresh instance
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: $what, $extra")
                resetMediaPlayer()
                // Return true to indicate we handled the error
                true
            }
        }
    }


    // Add this method to reset the player to a known state
    private fun resetMediaPlayer() {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.reset()
            currentPlayingPosition = -1
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting MediaPlayer", e)
            // In extreme cases, create a new instance
            try {
                mediaPlayer.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaPlayer", e)
            }
            initializeMediaPlayer()
        }
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

    private fun addAudioIfPresent(
        student: Student,
        binding: ItemStudentCardBinding,
        position: Int
    ) {
        if (student.audioFile == null) {
            binding.playButton.visibility = View.GONE
            return
        }

        // Show the play button, but make it disabled until the audio is loaded
        binding.playButton.visibility = View.VISIBLE
        binding.playButton.isEnabled = false // Disable until loaded

        // Get the Firebase download URL
        val storagePath = getStoragePath(student.audioFile)
        storageRef.child(storagePath).downloadUrl.addOnSuccessListener { uri ->
            // Store the URI in our map
            audioUrls[position] = uri.toString()

            // Enable the play button now that we have the URI
            binding.playButton.isEnabled = true

            // Set up the click listener
            binding.playButton.setOnClickListener {
                binding.playButton.isEnabled = false
                playPreloadedAudio(position, binding.playButton)
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to get download URL for audio", e)
            binding.playButton.visibility = View.GONE // Hide the button if loading fails
        }
    }

    private fun playPreloadedAudio(position: Int, playButton: Button) {
        // If media is already playing, ignore the click
        if (position == currentPlayingPosition && mediaPlayer.isPlaying) {
            return
        }

        // Get the preloaded URL
        val audioUrl = audioUrls[position] ?: run {
            Log.e(TAG, "No URL found for position $position")
            return
        }

        // Reset player and clear any existing playback
        try {
            mediaPlayer.reset()
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting MediaPlayer", e)
            initializeMediaPlayer() // Re-initialize if reset fails
        }

        // Set the new position as current
        currentPlayingPosition = position

        try {
            mediaPlayer.apply {
                setDataSource(audioUrl)
                setOnPreparedListener {
                    try {
                        start()
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Error starting playback after prepare", e)
                        currentPlayingPosition = -1
                    }
                }
                setOnCompletionListener {
                    currentPlayingPosition = -1
                    playButton.isEnabled = true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up playback", e)
            currentPlayingPosition = -1
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        try {
            mediaPlayer.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer on detach", e)
        }
    }

    override fun getItemCount(): Int {
        val count = students.size + 1 // start over card
        return count
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