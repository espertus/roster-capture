package com.ellenspertus.rostercapture.app

import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ellenspertus.rostercapture.NavGraphDirections
import com.ellenspertus.rostercapture.R
import com.ellenspertus.rostercapture.extensions.navigateSafe

object Menu {
    fun setupMenu(fragment: Fragment, button: ImageButton) {
        button.setOnClickListener { anchorView ->
            PopupMenu(fragment.requireContext(), anchorView).apply {
                menuInflater.inflate(R.menu.select_course_menu, menu)

                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_configure -> {
                            fragment.findNavController().navigateSafe(
                                NavGraphDirections.actionGlobalFieldConfigFragment()
                            )
                            true
                        }

                        R.id.action_about -> {
                            AboutDialog.showAboutDialog(
                                fragment.requireContext(),
                                fragment.lifecycleScope
                            )
                            true
                        }

                        else -> false
                    }
                }

                show()
            }
        }
    }
}