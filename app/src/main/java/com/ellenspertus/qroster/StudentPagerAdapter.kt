package com.ellenspertus.qroster

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.ellenspertus.qroster.model.Student
import com.google.firebase.storage.FirebaseStorage

class StudentPagerAdapter(
    private val context: Context,
    private val students: List<Student>
) : RecyclerView.Adapter<StudentPagerAdapter.StudentViewHolder>() {

    private val storageRef = FirebaseStorage.getInstance().reference

    inner class StudentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // UI elements
        val studentImageView: ImageView = view.findViewById(R.id.studentImageView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val pronounsTextView: TextView = view.findViewById(R.id.pronounsTextView)
        val imageProgressBar: ProgressBar = view.findViewById(R.id.imageProgressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_card, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.nameTextView.text = student.displayName
        holder.pronounsTextView.text = student.pronouns

        if (student.selfieFile == null) {
            holder.studentImageView.setImageResource(R.drawable.placeholder_profile)
            holder.imageProgressBar.visibility = View.GONE
        } else {
            val storagePath = when {
                student.selfieFile.startsWith("gs://") -> {
                    // Extract the path after the bucket name
                    // Format: gs://bucket-name/path/to/file.jpg
                    val uri = student.selfieFile.removePrefix("gs://")
                    val parts = uri.split("/", limit = 2)
                    if (parts.size > 1) parts[1] else student.selfieFile
                }

                else -> student.selfieFile // Assume it's already just the path
            }

            storageRef.child(storagePath).downloadUrl.addOnSuccessListener { uri ->
                // Use Glide to load and display the image
                Glide.with(context)
                    .load(uri)
                    .placeholder(R.drawable.placeholder_profile)
                    .error(R.drawable.missing_profile)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.studentImageView)

                holder.imageProgressBar.visibility = View.GONE
            }.addOnFailureListener { e ->
                // Add logging to see what went wrong
                Log.e(TAG, "Failed to load image for ${student.displayName}", e)

                // Handle image loading failure
                holder.studentImageView.setImageResource(R.drawable.missing_profile)
                holder.imageProgressBar.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = students.size

    companion object {
        const val TAG = "StudentPagerAdapter"
    }
}