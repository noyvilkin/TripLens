package com.colman.triplens.data.repo

import android.net.Uri
import androidx.lifecycle.LiveData
import com.colman.triplens.data.local.PostDao
import com.colman.triplens.data.model.Post
import com.colman.triplens.data.models.CloudinaryModel
import com.colman.triplens.data.remote.CountryResponse
import com.colman.triplens.data.remote.RetrofitClient
import com.colman.triplens.data.remote.WeatherResponse
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PostRepository(private val postDao: PostDao) {

    val allPosts: LiveData<List<Post>> = postDao.getAllPosts()

    private val weatherService = RetrofitClient.weatherService
    private val countryService = RetrofitClient.countryService
    private val cloudinaryModel = CloudinaryModel()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun addPostsToCache(posts: List<Post>) {
        withContext(Dispatchers.IO) { postDao.insertPosts(posts) }
    }

    suspend fun fetchWeather(city: String, apiKey: String): WeatherResponse? {
        return withContext(Dispatchers.IO) {
            try {
                weatherService.getWeather(city, apiKey)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun fetchCountryInfo(country: String): CountryResponse? {
        return withContext(Dispatchers.IO) {
            try {
                countryService.getCountryByName(country).firstOrNull()
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun uploadImages(uris: List<Uri>): List<String> {
        return uris.map { uri ->
            val name = UUID.randomUUID().toString()
            cloudinaryModel.uploadImage(uri, name)
        }
    }

    suspend fun savePost(post: Post) {
        withContext(Dispatchers.IO) {
            saveToFirestore(post)
            postDao.insertPost(post)
        }
    }

    private suspend fun saveToFirestore(post: Post): Unit = suspendCancellableCoroutine { cont ->
        firestore.collection("posts")
            .document(post.id)
            .set(post)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    suspend fun refreshPosts() { }
}
