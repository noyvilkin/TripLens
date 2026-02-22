package com.colman.triplens.ui.post

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.colman.triplens.data.local.AppDatabase
import com.colman.triplens.data.model.Post
import com.colman.triplens.data.repo.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

class AddPostViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val MAX_IMAGES = 5
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

    // Selected images (local URIs)
    private val _selectedImages = MutableLiveData<List<Uri>>(emptyList())
    val selectedImages: LiveData<List<Uri>> = _selectedImages

    // UI state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _postCreated = MutableLiveData(false)
    val postCreated: LiveData<Boolean> = _postCreated

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun addImage(uri: Uri) {
        val current = _selectedImages.value.orEmpty()
        if (current.size < MAX_IMAGES) {
            _selectedImages.value = current + uri
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
        if (_selectedImages.value.isNullOrEmpty()) {
            _error.value = "Please add at least one image"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val imageUris = _selectedImages.value.orEmpty()
                val downloadUrls = repository.uploadImages(imageUris)

                val user = FirebaseAuth.getInstance().currentUser
                val post = Post(
                    id = UUID.randomUUID().toString(),
                    userId = user?.uid ?: "",
                    userName = user?.displayName ?: user?.email ?: "Anonymous",
                    userProfileImage = user?.photoUrl?.toString() ?: "",
                    travelImage = downloadUrls.firstOrNull() ?: "",
                    imageUrls = downloadUrls.joinToString(","),
                    title = title,
                    description = description,
                    longDescription = longDescription,
                    destination = destination,
                    timestamp = System.currentTimeMillis()
                )

                // Save post without API data — enrichment happens
                // automatically when the feed loads (via PostRepository.refreshPosts)
                repository.savePost(post)
                _isLoading.postValue(false)
                _postCreated.postValue(true)
            } catch (e: Exception) {
                _isLoading.postValue(false)
                _error.postValue(e.message ?: "Failed to create post")
            }
        }
    }

    fun clearError() { _error.value = null }
}
