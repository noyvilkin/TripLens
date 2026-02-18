package com.colman.triplens.ui.feed

import android.app.Application
import androidx.lifecycle.*
import com.colman.triplens.data.local.AppDatabase
import com.colman.triplens.data.model.Post
import com.colman.triplens.data.repo.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository
    val posts: LiveData<List<Post>>

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        val postDao = AppDatabase.getDatabase(application).postDao()
        repository = PostRepository(postDao)
        posts = repository.allPosts

        // Automatically check and populate data on startup
        viewModelScope.launch {
            _isLoading.value = true
            // If database is empty, populate it with initial data
            // We use a small delay or check to ensure UI is ready
            if (posts.value.isNullOrEmpty()) {
                generateInitialData()
            }
            _isLoading.value = false
        }
    }

    private suspend fun generateInitialData() {
        withContext(Dispatchers.IO) {
            val initialPosts = (1..15).map { i ->
                Post(
                    id = "post_$i",
                    userName = "User $i",
                    title = "Adventure $i",
                    description = "Exploring the beauty of Location $i.",
                    destination = "Location $i",
                    travelImage = "https://picsum.photos/seed/${i + 50}/500/300",
                    userProfileImage = "https://i.pravatar.cc/150?u=$i",
                    timestamp = System.currentTimeMillis() - (i * 3600000) // Staggered by hours
                )
            }
            repository.addPostsToCache(initialPosts)
        }
    }

}