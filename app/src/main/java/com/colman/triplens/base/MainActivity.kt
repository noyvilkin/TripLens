package com.colman.triplens.base

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.colman.triplens.R
import com.colman.triplens.auth.AuthRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var toolbar: MaterialToolbar
    private var profileImageView: ImageView? = null

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
                    loadProfileImage()
                }
            }
        }
    }

    private fun setupToolbarMenu() {
        // Get the action view of the profile menu item
        val profileItem = toolbar.menu.findItem(R.id.action_profile)
        val actionView = profileItem?.actionView

        profileImageView = actionView?.findViewById(R.id.ivMenuProfile)

        // Set click listener on the action view to show popup
        actionView?.setOnClickListener { view ->
            showProfilePopup(view)
        }

        loadProfileImage()
    }

    private fun showProfilePopup(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, MENU_MY_PROFILE, 0, R.string.my_profile)
        popup.menu.add(0, MENU_LOGOUT, 1, R.string.logout)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                MENU_MY_PROFILE -> {
                    val currentDest = navController.currentDestination?.id
                    if (currentDest != R.id.profileFragment) {
                        navController.navigate(R.id.action_global_profileFragment)
                    }
                    true
                }
                MENU_LOGOUT -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun performLogout() {
        val authRepo = AuthRepository(this)
        authRepo.logout()
        navController.navigate(R.id.action_global_loginFragment)
    }

    /**
     * Load the current user's profile image into the toolbar menu icon.
     */
    fun loadProfileImage() {
        val user = FirebaseAuth.getInstance().currentUser
        val photoUrl = user?.photoUrl?.toString()

        profileImageView?.let { iv ->
            if (!photoUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(photoUrl)
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .fit().centerCrop()
                    .into(iv)
            } else {
                iv.setImageResource(android.R.drawable.ic_menu_myplaces)
            }
        }
    }

    companion object {
        private const val MENU_MY_PROFILE = 1
        private const val MENU_LOGOUT = 2
    }
}
