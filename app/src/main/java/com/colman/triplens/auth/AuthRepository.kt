package com.colman.triplens.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * Data-layer repository responsible for Firebase Authentication
 * and persisting the "Stay Logged In" preference.
 *
 * This class does NOT hold UI-related LiveData — that responsibility
 * belongs to [AuthViewModel] (MVVM separation).
 */
class AuthRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_STAY_LOGGED_IN = "stay_logged_in"
    }

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // If user previously chose NOT to stay logged in, sign them out immediately
        if (!isStayLoggedIn() && firebaseAuth.currentUser != null) {
            firebaseAuth.signOut()
        }
    }

    // ── Query helpers ────────────────────────────────────────────

    /** Returns the currently signed-in Firebase user, or null. */
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    /** Returns true if a Firebase user session exists. */
    fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null

    /** Returns the persisted "Stay Logged In" preference. */
    fun isStayLoggedIn(): Boolean = prefs.getBoolean(KEY_STAY_LOGGED_IN, false)

    /** Saves the "Stay Logged In" preference. */
    fun setStayLoggedIn(stay: Boolean) {
        prefs.edit().putBoolean(KEY_STAY_LOGGED_IN, stay).apply()
    }

    // ── Auth operations ──────────────────────────────────────────

    fun register(email: String, password: String, displayName: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Set display name on the newly created user
                    val user = firebaseAuth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { onResult(true, null) }
                        ?: onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun logout() {
        firebaseAuth.signOut()
        setStayLoggedIn(false)
    }
}
