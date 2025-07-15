package com.ellenspertus.rostercapture.app

import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.ellenspertus.rostercapture.NavGraphDirections
import com.ellenspertus.rostercapture.R
import com.ellenspertus.rostercapture.extensions.navigateSafe

class RosterCaptureMenuProvider(private val fragment: Fragment): MenuProvider {
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        Log.d("MenuProvider", "Creating menu")
        menuInflater.inflate(R.menu.select_course_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem) =
        when (menuItem.itemId) {
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