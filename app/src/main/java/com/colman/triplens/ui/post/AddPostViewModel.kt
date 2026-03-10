package com.colman.triplens.ui.post

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.colman.triplens.data.local.AppDatabase
import com.colman.triplens.data.model.Post
import com.colman.triplens.data.repo.PostRepository
import com.colman.triplens.util.SingleLiveEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class AddPostViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val MAX_IMAGES = 5
        private const val ENRICHMENT_TIMEOUT_MS = 15_000L
    }

    private val repository: PostRepository

    /** Country names fetched dynamically from RestCountries API */
    private val _countryNames = MutableLiveData<List<String>>()
    val countryNames: LiveData<List<String>> = _countryNames

    init {
        val dao = AppDatabase.getDatabase(application).postDao()
        repository = PostRepository(dao)

        // Fetch country names on init (runs once, cached afterwards)
        viewModelScope.launch {
            _countryNames.value = repository.fetchAllCountryNames()
        }
    }

    // ── Edit mode ────────────────────────────────────────────────
    private var editingPostId: String? = null

    private val _editingPost = MutableLiveData<Post?>(null)
    val editingPost: LiveData<Post?> = _editingPost

    /** Existing cloud image URLs (for edit mode — can be removed by user) */
    private val _existingImageUrls = MutableLiveData<List<String>>(emptyList())
    val existingImageUrls: LiveData<List<String>> = _existingImageUrls

    val isEditMode: Boolean get() = editingPostId != null

    /**
     * Load a post for editing. Call from Fragment when postId arg is non-empty.
     */
    fun loadPostForEditing(postId: String) {
        viewModelScope.launch {
            val post = repository.getPostByIdSync(postId) ?: return@launch
            editingPostId = post.id
            _editingPost.value = post
            _existingImageUrls.value = post.imageUrls
                .split(",")
                .filter { it.isNotBlank() }
        }
    }

    fun removeExistingImage(index: Int) {
        val current = _existingImageUrls.value.orEmpty().toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _existingImageUrls.value = current
        }
    }

    // ── Selected images (local URIs — new picks) ─────────────────
    private val _selectedImages = MutableLiveData<List<Uri>>(emptyList())
    val selectedImages: LiveData<List<Uri>> = _selectedImages

    // ── UI state ─────────────────────────────────────────────────

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /** One-shot event: post was saved. Prevents duplicate navigation on rotation. */
    private val _postCreated = SingleLiveEvent<Boolean>()
    val postCreated: LiveData<Boolean> = _postCreated

    /** One-shot event: error message. Prevents duplicate Snackbars on rotation. */
    private val _error = SingleLiveEvent<String?>()
    val error: LiveData<String?> = _error

    fun addImage(uri: Uri) {
        val existingCount = _existingImageUrls.value.orEmpty().size
        val newCount = _selectedImages.value.orEmpty().size
        if (existingCount + newCount < MAX_IMAGES) {
            _selectedImages.value = _selectedImages.value.orEmpty() + uri
        }
    }

    fun removeImage(index: Int) {
        val current = _selectedImages.value.orEmpty().toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _selectedImages.value = current
        }
    }

    fun submitPost(
        title: String,
        description: String,
        longDescription: String,
        destination: String
    ) {
        if (title.isBlank() || description.isBlank()) {
            _error.value = "Title and description are required"
            return
        }

        val existingUrls = _existingImageUrls.value.orEmpty()
        val newImages = _selectedImages.value.orEmpty()

        if (existingUrls.isEmpty() && newImages.isEmpty()) {
            _error.value = "Please add at least one image"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // 1. Upload only new images
                val newDownloadUrls = if (newImages.isNotEmpty()) {
                    repository.uploadImages(newImages)
                } else {
                    emptyList()
                }

                // 2. Combine existing (kept) URLs with newly uploaded URLs
                val allUrls = existingUrls + newDownloadUrls

                val user = FirebaseAuth.getInstance().currentUser
                val id = editingPostId ?: UUID.randomUUID().toString()

                val post = Post(
                    id = id,
                    userId = user?.uid ?: "",
                    userName = user?.displayName ?: user?.email ?: "Anonymous",
                    userProfileImage = user?.photoUrl?.toString() ?: "",
                    travelImage = allUrls.firstOrNull() ?: "",
                    imageUrls = allUrls.joinToString(","),
                    title = title,
                    description = description,
                    longDescription = longDescription,
                    destination = destination,
                    timestamp = if (isEditMode) {
                        _editingPost.value?.timestamp ?: System.currentTimeMillis()
                    } else {
                        System.currentTimeMillis()
                    }
                )

                // 3. Save post immediately for better UX (no waiting for API enrichment)
                repository.savePost(post)

                _isLoading.postValue(false)
                _postCreated.postValue(true)

                // 4. Enrich with Weather & Country data in background (non-blocking)
                //    This happens after navigation, improving perceived performance
                if (destination.isNotBlank()) {
                    viewModelScope.launch {
                        try {
                            val enrichedPost = withTimeoutOrNull(ENRICHMENT_TIMEOUT_MS) {
                                repository.enrichPostWithApiData(post)
                            }
                            if (enrichedPost != null && enrichedPost != post) {
                                repository.savePost(enrichedPost)  // Update with enriched data
                            }
                        } catch (e: Exception) {
                            // Silent failure - post is already saved, enrichment is bonus
                            Log.w("AddPostVM", "Background enrichment failed: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                _isLoading.postValue(false)
                _error.postValue(e.message ?: "Failed to save post")
            }
        }
    }

    fun clearError() { _error.value = null }
}
