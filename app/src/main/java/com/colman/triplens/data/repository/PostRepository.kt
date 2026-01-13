package com.colman.triplens.data.repo

import androidx.lifecycle.LiveData
import com.colman.triplens.data.local.PostDao
import com.colman.triplens.data.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostRepository(private val postDao: PostDao) {

    val allPosts: LiveData<List<Post>> = postDao.getAllPosts()

    // Add this specific function to resolve the "Unresolved reference" in ViewModel
    suspend fun addPostsToCache(posts: List<Post>) {
        withContext(Dispatchers.IO) {
            postDao.insertPosts(posts)
        }
    }

    suspend fun refreshPosts() {
        // Placeholder for future Firebase logic
    }
}