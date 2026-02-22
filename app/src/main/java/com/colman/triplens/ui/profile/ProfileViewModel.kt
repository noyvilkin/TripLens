package com.colman.triplens.ui.profile

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.colman.triplens.data.local.AppDatabase
import com.colman.triplens.data.model.Post
import com.colman.triplens.data.repo.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val repository: PostRepository

    // ── Current user ────────────────────────────────────────────
    private val _currentUser = MutableLiveData<FirebaseUser?>(FirebaseAuth.getInstance().currentUser)
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    // ── User's posts (filtered by userId via Room query) ────────
    private val _userId = MutableLiveData<String>()
    val userPosts: LiveData<List<Post>> = _userId.switchMap { uid ->
        repository.getPostsByUserId(uid)
    }

    // ── UI state ────────────────────────────────────────────────
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isUpdatingProfile = MutableLiveData(false)
    val isUpdatingProfile: LiveData<Boolean> = _isUpdatingProfile

    private val _profileUpdateSuccess = MutableLiveData(false)
    val profileUpdateSuccess: LiveData<Boolean> = _profileUpdateSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _postDeleted = MutableLiveData(false)
    val postDeleted: LiveData<Boolean> = _postDeleted

    init {
        val dao = AppDatabase.getDatabase(application).postDao()
        repository = PostRepository(dao)

        // Set up the userId to trigger user posts query
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            _userId.value = uid
        }
    }

    /**
     * Refresh user data from Firebase Auth.
     */
    fun refreshUser() {
        val user = FirebaseAuth.getInstance().currentUser
        _currentUser.value = user
        user?.uid?.let { _userId.value = it }
    }

    /**
     * Update the user's display name and optional profile picture.
     * 1. Upload new image to Cloudinary (if provided)
     * 2. Update Firebase Auth profile
     * 3. Update all user's posts in Room and Firestore
     */
    fun updateProfile(displayName: String, newImageUri: Uri?) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            _error.value = "No user signed in"
            return
        }

        if (displayName.isBlank()) {
            _error.value = "Display name cannot be empty"
            return
        }

        _isUpdatingProfile.value = true
        viewModelScope.launch {
            try {
                // Check if the new display name is taken (skip if unchanged)
                val nameChanged = displayName != user.displayName
                if (nameChanged) {
                    val snapshot = FirebaseFirestore.getInstance()
                        .collection("users")
                        .whereEqualTo("displayName", displayName)
                        .get()
                        .await()
                    val taken = snapshot.documents.any { it.id != user.uid }
                    if (taken) {
                        _isUpdatingProfile.postValue(false)
                        _error.postValue("Display name \"$displayName\" is already taken")
                        return@launch
                    }
                }

                // Upload new profile image if provided
                val imageUrl = if (newImageUri != null) {
                    repository.uploadImage(newImageUri)
                } else {
                    user.photoUrl?.toString() ?: ""
                }

                // Update Firebase Auth profile
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .setPhotoUri(if (imageUrl.isNotEmpty()) Uri.parse(imageUrl) else null)
                    .build()

                user.updateProfile(profileUpdates).await()

                // Update Firestore users collection
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .set(mapOf(
                        "displayName" to displayName,
                        "email" to (user.email ?: "")
                    ))

                // Update all posts by this user in Room and Firestore
                repository.updateUserInfoInPosts(user.uid, displayName, imageUrl)

                _isUpdatingProfile.postValue(false)
                _profileUpdateSuccess.postValue(true)

                // Refresh user data
                _currentUser.postValue(FirebaseAuth.getInstance().currentUser)

            } catch (e: Exception) {
                Log.w(TAG, "Profile update failed: ${e.message}")
                _isUpdatingProfile.postValue(false)
                _error.postValue(e.message ?: "Failed to update profile")
            }
        }
    }

    /**
     * Delete a post from both Firebase and local Room database.
     */
    fun deletePost(postId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.deletePost(postId)
                _isLoading.postValue(false)
                _postDeleted.postValue(true)
            } catch (e: Exception) {
                Log.w(TAG, "Delete post failed: ${e.message}")
                _isLoading.postValue(false)
                _error.postValue(e.message ?: "Failed to delete post")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearProfileUpdateSuccess() {
        _profileUpdateSuccess.value = false
    }

    fun clearPostDeleted() {
        _postDeleted.value = false
    }
}
