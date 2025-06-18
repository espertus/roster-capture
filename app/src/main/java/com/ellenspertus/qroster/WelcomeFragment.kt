package com.ellenspertus.qroster

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController

/**
 * The initial fragment, which signs in the user and directs to [SelectCourseFragment].
 */
class WelcomeFragment : Fragment() {
    override fun onStart() {
        super.onStart()

        // User already signed in, navigate immediately
        findNavController().navigate(R.id.action_welcomeFragment_to_selectCourseFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    companion object {
        const val TAG = "WelcomeFragment"
    }
}