package com.ellenspertus.qroster

import android.content.Context
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
    private val enclosingFragment: StudentsFragment,
) : RecyclerView.Adapter<StudentPagerAdapter.StudentViewHolder>() {

    private val storageRef = FirebaseStorage.getInstance().reference

    inner class StudentViewHolder(val binding: ItemStudentCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        with (holder.binding) {
            nameTextView.text = student.displayName
            pronounsTextView.text = student.pronouns

            showInfoButton.visibility = View.VISIBLE
            showInfoButton.let {
                it.visibility = View.VISIBLE
                it.setOnClickListener {
                    it.visibility = View.GONE
                    studentInfoContainer.visibility = View.VISIBLE
                    enclosingFragment.showButtons()
                }
            }
            if (student.selfieFile == null) {
                studentImageView.setImageResource(R.drawable.placeholder_profile)
                imageProgressBar.visibility = View.GONE
            } else {
                val storagePath = getStoragePath(student.selfieFile)
                storageRef.child(storagePath).downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(context)
                        .load(uri)
                        .placeholder(R.drawable.placeholder_profile)
                        .error(R.drawable.missing_profile)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(studentImageView)
                    imageProgressBar.visibility = View.GONE
            }.addOnFailureListener { e ->
                    // Add logging to see what went wrong
                    Log.e(TAG, "Failed to load image for ${student.displayName}", e)

                    // Handle image loading failure
                    studentImageView.setImageResource(R.drawable.missing_profile)
                    imageProgressBar.visibility = View.GONE
                }
            }
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