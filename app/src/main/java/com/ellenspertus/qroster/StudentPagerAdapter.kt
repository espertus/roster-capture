package com.ellenspertus.qroster

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.ellenspertus.qroster.databinding.ItemStudentCardBinding
import com.ellenspertus.qroster.model.Student
import com.google.firebase.storage.FirebaseStorage

class StudentPagerAdapter(
    private val context: Context,
    private val students: List<Student>,
    private val enclosingFragment: StudentsFragment, // TODO: Create listener interface
) : RecyclerView.Adapter<StudentPagerAdapter.StudentViewHolder>() {

    private val storageRef = FirebaseStorage.getInstance().reference
    private lateinit var itemStudentCardBinding: ItemStudentCardBinding
    private var mediaPlayer = MediaPlayer()
    private var currentPlayingPosition = 0

    // Map to store preloaded audio URLs
    private val audioUrls = mutableMapOf<Int, String>()

    inner class StudentViewHolder(val binding: ItemStudentCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        itemStudentCardBinding =
            ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        with(itemStudentCardBinding) {
            showInfoButton.let {
                it.visibility = View.VISIBLE
                it.setOnClickListener {
                    it.visibility = View.GONE
                    studentInfoContainer.visibility = View.VISIBLE
                    enclosingFragment.showButtons()
                }
            }
        }
        return StudentViewHolder(itemStudentCardBinding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        with(holder.binding) {
            nameTextView.text = student.displayName
            pronounsTextView.text = student.pronouns
            enclosingFragment.hideButtons()

            addSelfieIfPresent(student, this)
            addAudioIfPresent(student, this, position)
        }
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

            setOnCompletionListener {
                currentPlayingPosition = -1
                itemStudentCardBinding.playButton.isEnabled = true
            }

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
                playPreloadedAudio(position)
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to get download URL for audio", e)
            binding.playButton.visibility = View.GONE // Hide the button if loading fails
        }
    }

    private fun playPreloadedAudio(position: Int) {
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

        // Use the preloaded URL
        try {
            // Set source and prepare
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

    override fun getItemCount(): Int = students.size

    companion object {
        const val TAG = "StudentPagerAdapter"

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