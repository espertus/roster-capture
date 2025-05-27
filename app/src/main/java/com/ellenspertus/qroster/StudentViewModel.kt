package com.ellenspertus.qroster

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.ellenspertus.qroster.model.Student
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StudentViewModel : ViewModel() {
    private val _students = MutableLiveData<List<Student>>()
    val students: LiveData<List<Student>> = _students

    private val _uiMessage = MutableLiveData<UiMessage?>()
    val uiMessage: LiveData<UiMessage?> = _uiMessage

    private val firestore = FirebaseFirestore.getInstance()
    private var exoPlayer: ExoPlayer? = null

    fun loadStudentsForCourse(crn: String) {
        firestore.collection(ENROLLMENTS_COLLECTION)
            .whereEqualTo("crn", crn)
            .get()
            .addOnSuccessListener { enrollmentDocuments ->
                // Extract all student IDs from the enrollments
                val studentIds = enrollmentDocuments.documents.mapNotNull { doc ->
                    doc.getString("nuid")
                }
                if (studentIds.isEmpty()) {
                    _students.value = emptyList()
                    return@addOnSuccessListener
                }

                // Split student IDs into chunks as required
                val chunks = studentIds.chunked(RETRIEVAL_CHUNK_SIZE)
                val allStudents = mutableListOf<Student>()
                var remainingQueries = chunks.size

                fun completeChunk(newStudents: List<Student> = emptyList()) {
                    synchronized(allStudents) {
                        allStudents.addAll(newStudents)
                        if (--remainingQueries == 0) {
                            _students.value = allStudents.shuffled()
                        }
                    }
                }

                chunks.forEach { chunk ->
                    firestore.collection(STUDENTS_COLLECTION)
                        .whereIn("nuid", chunk)
                        .get()
                        .addOnSuccessListener { studentDocuments ->
                            val chunkStudents = studentDocuments.documents.mapNotNull { doc ->
                                try {
                                    doc.toObject(Student::class.java)?.apply {
                                        // Save docId to facilitate updates.
                                        docId = doc.id
                                        val student = this

                                        viewModelScope.launch {
                                            prefetchAudio(student)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("StudentViewModel", "Error converting doc ${doc.id}", e)
                                    null
                                }
                            }
                            completeChunk(chunkStudents)
                        }
                        .addOnFailureListener { e ->
                            Log.e("StudentViewModel", "Error loading students chunk", e)
                            completeChunk()
                        }
                }
            }
    }

    private suspend fun prefetchAudio(student: Student) = coroutineScope {
        student.audioFile?.let {
            try {
                val url = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(it)
                    .downloadUrl
                    .await()
                    .toString()
                student.audioDownloadUrl = url
            } catch (e: Exception) {
                _uiMessage.value = UiMessage.Failure("Failed to get URL for ${student.nuid}")
            }
        }
    }

    private fun getOrCreatePlayer(context: Context): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true // handleAudioFocus
            )
            .build()
            .also { exoPlayer = it }
    }

    fun playAudio(context: Context, student: Student) {
        // If there is no audio file, this should not be reachable.
        if (student.audioFile == null) {
            Log.e(TAG, "playAudio() called even though audioFile null for $student")
            return
        }

        student.audioDownloadUrl?.let {
            // Reset player.
            val player = getOrCreatePlayer(context)
            player.stop()
            player.clearMediaItems()

            // Play item.
            val mediaItem = MediaItem.fromUri(it)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        } ?: run {
            Log.e(TAG, "playAudio() called but audioDownloadUrl not available")
            Toast.makeText(context, "Unable to play audio", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }

    fun updateStudentNote(student: Student, newNote: String?) {
        viewModelScope.launch {
            student.note = newNote
            try {
                firestore.collection(STUDENTS_COLLECTION)
                    .document(student.nuid)
                    .update("note", newNote)
                    .await()

                _uiMessage.value = UiMessage.Success("Saved note")
                _students.value = _students.value // Trigger LiveData update

            } catch (e: Exception) {
                _uiMessage.value = UiMessage.Failure("Failed to update note: $e")
            }
        }
    }

    fun deleteStudentNote(student: Student) {
        FirebaseFirestore.getInstance()
            .collection(STUDENTS_COLLECTION)
            .document(student.docId)
            .update("note", FieldValue.delete())
            .addOnSuccessListener {
                _uiMessage.value = UiMessage.Success("Deleted note for ${student.displayName}")
            }
            .addOnFailureListener { e ->
                Log.e(StudentPagerAdapter.TAG, "Unable to delete note: $e")
            }
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    companion object {
        private const val TAG = "StudentViewModel"
        // whereIn queries can have up to 10 values
        private const val RETRIEVAL_CHUNK_SIZE = 10
    }

//    // Track learning progress
//    fun markStudentAsLearned(position: Int) {
//        val studentList = _students.value ?: return
//        if (position < 0 || position >= studentList.size) return
//
//        val student = studentList[position]
//
//        // Update Firestore with learning progress
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//        firestore.collection("userProgress")
//            .document(userId)
//            .collection("learnedStudents")
//            .document(student.nuid)
//            .set(mapOf(
//                "learnedAt" to FieldValue.serverTimestamp(),
//                "studentId" to student.nuid,
//                "crn" to student.crn
//            ))
//    }
}