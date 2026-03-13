package com.colman.triplens.data.repo

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.colman.triplens.BuildConfig
import com.colman.triplens.data.local.CommentDao
import com.colman.triplens.data.local.CommentEntity
import com.colman.triplens.data.local.CountryDao
import com.colman.triplens.data.local.CountryNameEntity
import com.colman.triplens.data.local.PostDao
import com.colman.triplens.data.model.Comment
import com.colman.triplens.data.model.Post
import com.colman.triplens.data.models.CloudinaryModel
import com.colman.triplens.data.remote.RetrofitClient
import com.colman.triplens.data.util.CountryList
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PostRepository(
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val countryDao: CountryDao
) {

    companion object {
        private const val TAG = "PostRepository"
    }

    val allPosts: LiveData<List<Post>> = postDao.getAllPosts()

    private val weatherService = RetrofitClient.weatherService
    private val countryService = RetrofitClient.countryService
    private val cloudinaryModel = CloudinaryModel()
    private val firestore = FirebaseFirestore.getInstance()

    /** In-memory cache of country names fetched from RestCountries API */
    @Volatile
    private var cachedCountryNames: List<String>? = null

    // ── Local Cache ──────────────────────────────────────────────

    suspend fun addPostsToCache(posts: List<Post>) {
        withContext(Dispatchers.IO) { postDao.insertPosts(posts) }
    }

    fun getPostById(postId: String): LiveData<Post?> = postDao.getPostById(postId)

    suspend fun getPostByIdSync(postId: String): Post? =
        withContext(Dispatchers.IO) { postDao.getPostByIdSync(postId) }

    fun getPostsByUserId(userId: String): LiveData<List<Post>> =
        postDao.getPostsByUserId(userId)

    // ── External API Calls (with error handling) ─────────────────

    /**
     * Fetch weather for a city. Returns null on failure instead of crashing.
     */
    suspend fun fetchWeatherData(city: String): WeatherResult? {
        return withContext(Dispatchers.IO) {
            try {
                val response = weatherService.getWeather(city, BuildConfig.OPENWEATHER_API_KEY)
                val temp = "%.1f".format(response.main.temp)
                val condition = response.weather.firstOrNull()?.main ?: ""
                val icon = response.weather.firstOrNull()?.icon ?: ""
                val humidity = response.main.humidity?.toString() ?: ""
                val windSpeed = response.wind?.speed?.let { speedMs ->
                    "%.1f".format(speedMs * 3.6)
                } ?: ""
                WeatherResult(temp, condition, icon, humidity, windSpeed)
            } catch (e: Exception) {
                Log.w(TAG, "Weather fetch failed for '$city': ${e.message}")
                null
            }
        }
    }

    /**
     * Fetch country info. Returns null on failure instead of crashing.
     */
    suspend fun fetchCountryData(country: String): CountryResult? {
        return withContext(Dispatchers.IO) {
            try {
                val response = countryService.getCountryByName(country).firstOrNull()
                    ?: return@withContext null
                CountryResult(
                    capital = response.capital?.firstOrNull() ?: "",
                    population = formatPopulation(response.population),
                    currency = response.currencies?.values?.firstOrNull()?.name ?: "",
                    flagUrl = response.flags?.png ?: "",
                    languages = response.languages?.values?.joinToString(", ") ?: ""
                )
            } catch (e: Exception) {
                Log.w(TAG, "Country fetch failed for '$country': ${e.message}")
                null
            }
        }
    }

    /**
     * Fetch the full list of country names from RestCountries API.
     * Returns Room-cached names first, then network. Falls back to a built-in list.
     */
    suspend fun fetchAllCountryNames(): List<String> {
        cachedCountryNames?.let { return it }

        return withContext(Dispatchers.IO) {
            val cachedDbNames = countryDao.getAllCountryNames()
            if (cachedDbNames.isNotEmpty()) {
                cachedCountryNames = cachedDbNames
                return@withContext cachedDbNames
            }

            try {
                val response = countryService.getAllCountries()
                val names = response
                    .mapNotNull { it.name?.common }
                    .distinct()
                    .sorted()

                countryDao.clearCountryNames()
                countryDao.insertCountryNames(names.map { CountryNameEntity(it) })

                cachedCountryNames = names
                Log.d(TAG, "Fetched ${names.size} country names from API")
                names
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch country list from API: ${e.message}")
                val fallback = CountryList.FALLBACK_COUNTRIES.sorted()
                if (fallback.isNotEmpty()) {
                    countryDao.clearCountryNames()
                    countryDao.insertCountryNames(fallback.map { CountryNameEntity(it) })
                }
                cachedCountryNames = fallback
                fallback
            }
        }
    }

    /**
     * Enrich a post with weather and country data from external APIs.
     * Returns the enriched post. If APIs fail, original data is preserved.
     */
    suspend fun enrichPostWithApiData(post: Post): Post {
        val destination = post.destination.trim()
        if (destination.isEmpty()) return post

        // Destination is a country name; use it directly for both APIs
        var enriched = post

        // Only fetch weather if not already populated
        if (post.temperature.isEmpty() || post.humidity.isEmpty() || post.windSpeed.isEmpty()) {
            val weather = fetchWeatherData(destination)
            if (weather != null) {
                enriched = enriched.copy(
                    temperature = weather.temp,
                    weatherCondition = weather.condition,
                    weatherIcon = weather.icon,
                    humidity = weather.humidity,
                    windSpeed = weather.windSpeed
                )
            }
        }

        // Only fetch country info if not already populated
        if (post.countryCapital.isEmpty()) {
            val countryData = fetchCountryData(destination)
            if (countryData != null) {
                enriched = enriched.copy(
                    countryCapital = countryData.capital,
                    countryPopulation = countryData.population,
                    countryCurrency = countryData.currency,
                    countryFlag = countryData.flagUrl,
                    countryLanguages = countryData.languages
                )
            }
        }

        return enriched
    }

    // ── Image Upload ─────────────────────────────────────────────

    suspend fun uploadImages(uris: List<Uri>): List<String> {
        return uris.map { uri ->
            val name = UUID.randomUUID().toString()
            cloudinaryModel.uploadImage(uri, name)
        }
    }

    suspend fun uploadImage(uri: Uri): String {
        val name = UUID.randomUUID().toString()
        return cloudinaryModel.uploadImage(uri, name)
    }

    // ── Post CRUD ────────────────────────────────────────────────

    suspend fun savePost(post: Post) {
        withContext(Dispatchers.IO) {
            saveToFirestore(post)
            postDao.insertPost(post)
        }
    }

    suspend fun deletePost(postId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Delete from Firestore
                firestore.collection("posts")
                    .document(postId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete post from Firestore: ${e.message}")
            }
            // Delete from local Room cache
            postDao.deletePostById(postId)
        }
    }

    suspend fun updateUserInfoInPosts(userId: String, userName: String, profileImage: String) {
        withContext(Dispatchers.IO) {
            // Update local Room cache
            postDao.updateUserInfoInPosts(userId, userName, profileImage)

            // Update Firestore documents
            try {
                val snapshot = firestore.collection("posts")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                for (doc in snapshot.documents) {
                    doc.reference.update(
                        mapOf(
                            "userName" to userName,
                            "userProfileImage" to profileImage
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update user info in Firestore posts: ${e.message}")
            }
        }
    }

    private suspend fun saveToFirestore(post: Post): Unit = suspendCancellableCoroutine { cont ->
        firestore.collection("posts")
            .document(post.id)
            .set(post)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    /**
     * Refresh local cache from Firestore.
     * Fetches all posts, enriches any that are missing API data,
     * then caches them in Room.
     */
    suspend fun refreshPosts() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)
                }

                if (posts.isNotEmpty()) {
                    // Enrich posts that don't have API data yet
                    val enrichedPosts = posts.map { post ->
                        if (post.destination.isNotEmpty() &&
                            (post.temperature.isEmpty() ||
                                post.humidity.isEmpty() ||
                                post.windSpeed.isEmpty() ||
                                post.countryCapital.isEmpty())
                        ) {
                            enrichPostWithApiData(post)
                        } else {
                            post
                        }
                    }
                    postDao.clearAllPosts()
                    postDao.insertPosts(enrichedPosts)

                    // Also update Firestore with enriched data
                    enrichedPosts.forEach { post ->
                        try {
                            firestore.collection("posts")
                                .document(post.id)
                                .set(post)
                        } catch (_: Exception) { /* best-effort update */ }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh posts from Firestore: ${e.message}")
            }
        }
    }

    // ── Comments (Firestore sub-collection, real-time) ───────────

    /**
     * Returns comments from Room cache while syncing Firestore updates into Room.
     */
    fun getCommentsFlow(postId: String): Flow<List<Comment>> = callbackFlow {
        val roomJob = launch {
            commentDao.getCommentsByPostId(postId)
                .map { comments -> comments.map { it.toDomain() } }
                .collectLatest { comments ->
                    trySend(comments)
                }
        }

        val ref = firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Comments listener error: ${error.message}")
                return@addSnapshotListener
            }

            val remoteComments = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Comment::class.java)
            } ?: emptyList()

            launch(Dispatchers.IO) {
                commentDao.replaceCommentsForPost(
                    postId,
                    remoteComments.map { it.toEntity() }
                )
            }
        }

        awaitClose {
            listener.remove()
            roomJob.cancel()
        }
    }

    /**
     * Add a comment to Firestore and cache it locally for offline reads.
     */
    suspend fun addComment(comment: Comment) {
        withContext(Dispatchers.IO) {
            try {
                firestore.collection("posts")
                    .document(comment.postId)
                    .collection("comments")
                    .document(comment.id)
                    .set(comment)
                    .await()

                commentDao.insertComment(comment.toEntity())
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add comment: ${e.message}")
                throw e
            }
        }
    }

    // ── Helper Models ────────────────────────────────────────────

    private fun formatPopulation(pop: Long?): String {
        if (pop == null) return "N/A"
        return when {
            pop >= 1_000_000 -> "%.1fM".format(pop / 1_000_000.0)
            pop >= 1_000 -> "%.1fK".format(pop / 1_000.0)
            else -> pop.toString()
        }
    }
}

/** Parsed weather result from OpenWeather API */
data class WeatherResult(
    val temp: String,
    val condition: String,
    val icon: String,
    val humidity: String,
    val windSpeed: String
)

/** Parsed country result from RestCountries API */
data class CountryResult(
    val capital: String,
    val population: String,
    val currency: String,
    val flagUrl: String,
    val languages: String
)

private fun Comment.toEntity(): CommentEntity = CommentEntity(
    id = id,
    postId = postId,
    userId = userId,
    userName = userName,
    userProfileImage = userProfileImage,
    text = text,
    timestamp = timestamp
)

private fun CommentEntity.toDomain(): Comment = Comment(
    id = id,
    postId = postId,
    userId = userId,
    userName = userName,
    userProfileImage = userProfileImage,
    text = text,
    timestamp = timestamp
)
