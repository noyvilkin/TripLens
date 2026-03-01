package com.colman.triplens.base

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.colman.triplens.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var toolbar: MaterialToolbar
    private var feedChip: Chip? = null
    private var profileChip: Chip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        toolbar = findViewById(R.id.toolbar)
        setupNavigation()
        setupToolbarMenu()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Declare feedFragment as a top-level destination so no back arrow is shown
        val appBarConfig = AppBarConfiguration(
            setOf(R.id.loginFragment, R.id.feedFragment)
        )
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfig)

        // Show/hide toolbar based on current destination
        navController.addOnDestinationChangedListener { _, dest, _ ->
            when (dest.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    toolbar.visibility = View.GONE
                }
                else -> {
                    toolbar.visibility = View.VISIBLE
                    updateNavigationState(dest.id)
                }
            }
        }
    }

    private fun setupToolbarMenu() {
        // Get the action view of the feed menu item
        val feedItem = toolbar.menu.findItem(R.id.action_feed)
        feedChip = feedItem?.actionView?.findViewById(R.id.chipFeed)

        // Set click listener on the feed chip to navigate to feed
        feedChip?.setOnClickListener {
            val currentDest = navController.currentDestination?.id
            if (currentDest != R.id.feedFragment) {
                navController.navigate(R.id.action_global_feedFragment)
            }
        }

        // Get the action view of the profile menu item
        val profileItem = toolbar.menu.findItem(R.id.action_profile)
        profileChip = profileItem?.actionView?.findViewById(R.id.chipProfile)

        // Set click listener on the profile chip to navigate to profile
        profileChip?.setOnClickListener {
            val currentDest = navController.currentDestination?.id
            if (currentDest != R.id.profileFragment) {
                navController.navigate(R.id.action_global_profileFragment)
            }
        }
    }

    private fun updateNavigationState(currentDestinationId: Int) {
        val darkColor = ContextCompat.getColor(this, R.color.neutral_dark)
        val whiteColor = ContextCompat.getColor(this, R.color.white)
        val transparentColor = ContextCompat.getColor(this, android.R.color.transparent)
        val defaultTextColor = ContextCompat.getColor(this, R.color.neutral_medium)

        when (currentDestinationId) {
            R.id.feedFragment -> {
                // Feed is active - black background
                feedChip?.chipBackgroundColor = android.content.res.ColorStateList.valueOf(darkColor)
                feedChip?.setTextColor(whiteColor)
                feedChip?.chipIconTint = android.content.res.ColorStateList.valueOf(whiteColor)

                // Profile is inactive - transparent background
                profileChip?.chipBackgroundColor = android.content.res.ColorStateList.valueOf(transparentColor)
                profileChip?.setTextColor(defaultTextColor)
                profileChip?.chipIconTint = android.content.res.ColorStateList.valueOf(defaultTextColor)
            }
            R.id.profileFragment -> {
                // Profile is active - black background
                profileChip?.chipBackgroundColor = android.content.res.ColorStateList.valueOf(darkColor)
                profileChip?.setTextColor(whiteColor)
                profileChip?.chipIconTint = android.content.res.ColorStateList.valueOf(whiteColor)

                // Feed is inactive - transparent background
                feedChip?.chipBackgroundColor = android.content.res.ColorStateList.valueOf(transparentColor)
                feedChip?.setTextColor(defaultTextColor)
                feedChip?.chipIconTint = android.content.res.ColorStateList.valueOf(defaultTextColor)
            }
            else -> {
                // Both inactive for other screens
                feedChip?.chipBackgroundColor = android.content.res.ColorStateList.valueOf(transparentColor)
                feedChip?.setTextColor(defaultTextColor)
                feedChip?.chipIconTint = android.content.res.ColorStateList.valueOf(defaultTextColor)

                profileChip?.chipBackgroundColor = android.content.res.ColorStateList.valueOf(transparentColor)
                profileChip?.setTextColor(defaultTextColor)
                profileChip?.chipIconTint = android.content.res.ColorStateList.valueOf(defaultTextColor)
            }
        }
    }
}
