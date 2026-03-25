package com.colman.triplens.ui.feed

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.colman.triplens.data.local.AppDatabase
import com.colman.triplens.data.model.Post
import com.colman.triplens.data.repo.PostRepository
import kotlinx.coroutines.launch

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "FeedViewModel"
    }

    private val repository: PostRepository
    val posts: LiveData<List<Post>>

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PostRepository(
            postDao = db.postDao(),
            commentDao = db.commentDao(),
            countryDao = db.countryDao()
        )
        posts = repository.allPosts

        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Refreshing posts from Firestore...")
                repository.refreshPosts()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh posts: ${e.message}", e)
            }
            _isLoading.value = false
        }
    }
}
