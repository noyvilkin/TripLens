package com.colman.triplens.ui.feed

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.colman.triplens.data.local.AppDatabase
import com.colman.triplens.data.model.Post
import com.colman.triplens.data.repo.PostRepository
import com.colman.triplens.data.util.SeedDataManager
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
    private val seedDataManager: SeedDataManager
    val posts: LiveData<List<Post>>

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        val db = AppDatabase.getDatabase(application)
        val postDao = db.postDao()
        repository = PostRepository(
            postDao = postDao,
            commentDao = db.commentDao(),
            countryDao = db.countryDao()
        )
        seedDataManager = SeedDataManager(application)
        posts = repository.allPosts

        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Initializing FeedViewModel...")

                // Try to refresh from Firestore first
                Log.d(TAG, "Refreshing from Firestore...")
                repository.refreshPosts()

                // Check if posts exist after Firestore sync
                val currentPosts: List<Post> = withContext(Dispatchers.IO) {
                    postDao.getAllPostsSync()
                }

                val seedGenerated = seedDataManager.isSeedGenerated()
                Log.d(TAG, "Posts count: ${currentPosts.size}, Seed generated: $seedGenerated")

                // Generate seed data if:
                // 1. No posts exist after Firestore sync
                // 2. Seed flag is false (never generated)
                if (currentPosts.isEmpty() && !seedGenerated) {
                    Log.d(TAG, "Generating seed data (15 posts)...")
                    generateInitialData()
                    seedDataManager.markSeedGenerated()
                    Log.d(TAG, "Seed data generated successfully!")
                } else {
                    Log.d(TAG, "Skipping seed generation (posts exist or already generated)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Init failed: ${e.message}", e)
                // Fallback: generate seed data if not already generated
                if (!seedDataManager.isSeedGenerated()) {
                    Log.d(TAG, "Generating seed data as fallback...")
                    generateInitialData()
                    seedDataManager.markSeedGenerated()
                }
            }
            _isLoading.value = false
        }
    }

    private suspend fun generateInitialData() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Creating 15 initial posts...")
            val initialPosts = (1..15).map { i ->
                val dest = SAMPLE_DESTINATIONS.getOrElse(i - 1) { "France" }
                Post(
                    id = "seed_post_${System.currentTimeMillis()}_$i",
                    userId = "seed_user",
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

            Log.d(TAG, "Saving ${initialPosts.size} posts to Room cache...")
            // Save to Room first (immediate display)
            repository.addPostsToCache(initialPosts)

            Log.d(TAG, "Saving posts to Firebase...")
            // Save to Firebase so they persist across reinstalls
            initialPosts.forEach { post ->
                try {
                    repository.savePost(post)
                    Log.d(TAG, "Saved post ${post.id} to Firebase")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save post ${post.id} to Firebase: ${e.message}", e)
                }
            }

            Log.d(TAG, "Enriching posts with API data in background...")
            // Then enrich each post with real API data in the background
            initialPosts.forEachIndexed { index, post ->
                try {
                    Log.d(TAG, "Enriching post ${index + 1}/15: ${post.destination}")
                    val enriched = repository.enrichPostWithApiData(post)
                    if (enriched != post) {
                        repository.savePost(enriched) // Update in Firebase
                        repository.addPostsToCache(listOf(enriched)) // Update in Room
                        Log.d(TAG, "Enriched post ${post.id}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to enrich post ${post.id}: ${e.message}")
                    // Non-fatal — the post still displays with placeholders
                }
            }
            Log.d(TAG, "Seed data generation complete!")
        }
    }
}
