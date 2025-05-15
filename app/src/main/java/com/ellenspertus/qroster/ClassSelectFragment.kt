package com.ellenspertus.qroster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.ellenspertus.qroster.MainActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

class ClassSelectFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var signOutButton: Button
    private lateinit var welcomeText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_class_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        welcomeText = view.findViewById(R.id.textWelcome)
        signOutButton = view.findViewById(R.id.buttonSignOut)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set welcome message with user's name
        auth.currentUser?.let { user ->
            welcomeText.text = "Welcome, ${user.displayName ?: "User"}"
        }

        // Setup sign out button
        signOutButton.setOnClickListener {
            AuthUI.getInstance().signOut(requireContext()).addOnCompleteListener {
                // Navigate back to login screen
                (activity as? MainActivity)?.navigateToLogin()
            }
        }
    }
}