package com.ellenspertus.qroster

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase

class ClassSelectFragment : Fragment() {
    companion object {
        const val CLASSES_COLLECTION = "classes"
        const val TAG = "ClassSelectFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_class_select, container, false)
    }

    override fun onStart() {
        super.onStart()
        (activity as MainActivity).verifyAuthentication()
        retrieveClasses()
    }

    private fun retrieveClasses() {
        val db = Firebase.firestore
        db.collection(CLASSES_COLLECTION)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d(TAG, "Retrieved " + document.data)
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }
}