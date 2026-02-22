package com.colman.triplens.ui.feed

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.colman.triplens.data.local.AppDatabase
import com.colman.triplens.data.model.Post
import com.colman.triplens.data.repo.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "FeedViewModel"

        /** Sample country-only destinations for initial mock data enrichment */
        private val SAMPLE_DESTINATIONS = listOf(
            "France", "Japan", "Italy",
            "United Kingdom", "Spain",
            "United States", "Australia",
            "Thailand", "Turkey", "Germany",
            "Netherlands", "South Korea",
            "Portugal", "Czech Republic", "Greece"
        )
    }

    private val repository: PostRepository
    val posts: LiveData<List<Post>>

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        val postDao = AppDatabase.getDatabase(application).postDao()
        repository = PostRepository(postDao)
        posts = repository.allPosts

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Try to refresh from Firestore first
                repository.refreshPosts()

                // If DB is still empty after Firestore sync, generate initial data
                val currentPosts = withContext(Dispatchers.IO) {
                    repository.allPosts.value
                }
                if (currentPosts.isNullOrEmpty()) {
                    generateInitialData()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Init failed, generating local data: ${e.message}")
                generateInitialData()
            }
            _isLoading.value = false
        }
    }

    private suspend fun generateInitialData() {
        withContext(Dispatchers.IO) {
            val initialPosts = (1..15).map { i ->
                val dest = SAMPLE_DESTINATIONS.getOrElse(i - 1) { "France" }
                Post(
                    id = "post_$i",
                    userName = "Traveler $i",
                    title = "Adventure in $dest",
                    description = "Exploring the beauty of $dest.",
                    longDescription = buildString {
                        append("My incredible journey to $dest was unforgettable. ")
                        append("From the stunning architecture to the delicious local cuisine, ")
                        append("every moment was a new discovery. I spent several days wandering through ")
                        append("the streets, meeting locals, and experiencing the rich culture firsthand. ")
                        append("This destination offers something for every type of traveler — ")
                        append("whether you're looking for history, nightlife, or natural beauty.")
                    },
                    destination = dest,
                    travelImage = "https://picsum.photos/seed/${i + 50}/500/300",
                    imageUrls = (0..2).joinToString(",") {
                        "https://picsum.photos/seed/${i * 10 + it}/500/300"
                    },
                    userProfileImage = "https://i.pravatar.cc/150?u=$i",
                    timestamp = System.currentTimeMillis() - (i * 3600000L)
                )
            }

            // Save initial posts to Room first (so UI shows something)
            repository.addPostsToCache(initialPosts)

            // Then enrich each post with real API data in the background
            initialPosts.forEach { post ->
                try {
                    val enriched = repository.enrichPostWithApiData(post)
                    if (enriched != post) {
                        repository.addPostsToCache(listOf(enriched))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to enrich post ${post.id}: ${e.message}")
                    // Non-fatal — the post still displays with placeholders
                }
            }
        }
    }
}
