package com.colman.triplens.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

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
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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

    // ── Display name uniqueness ──────────────────────────────────

    /**
     * Check whether a display name is already taken in the Firestore `users` collection.
     * @param excludeUserId If non-null, ignores the document belonging to this user
     *                      (used when editing your own profile).
     */
    fun isDisplayNameTaken(
        displayName: String,
        excludeUserId: String? = null,
        onResult: (Boolean) -> Unit
    ) {
        firestore.collection("users")
            .whereEqualTo("displayName", displayName)
            .get()
            .addOnSuccessListener { snapshot ->
                val taken = snapshot.documents.any { doc ->
                    excludeUserId == null || doc.id != excludeUserId
                }
                onResult(taken)
            }
            .addOnFailureListener {
                // On network error, don't block the user
                onResult(false)
            }
    }

    /**
     * Save (or update) the user document in Firestore `users` collection.
     */
    fun saveUserToFirestore(uid: String, displayName: String, email: String) {
        val userData = mapOf(
            "displayName" to displayName,
            "email" to email
        )
        firestore.collection("users").document(uid).set(userData)
    }

    // ── Auth operations ──────────────────────────────────────────

    fun register(email: String, password: String, displayName: String, onResult: (Boolean, String?) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    // Set display name on the newly created user
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener {
                            // Save display name to Firestore users collection
                            user.let { u ->
                                saveUserToFirestore(u.uid, displayName, email)
                            }
                            onResult(true, null)
                        }
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
