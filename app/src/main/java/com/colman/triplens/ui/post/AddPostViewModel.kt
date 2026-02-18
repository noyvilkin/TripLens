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
        const val WEATHER_API_KEY = "732ac5d8063c73f2138ed054fe94510e"
        const val MAX_IMAGES = 5
    }

    private val repository: PostRepository

    init {
        val dao = AppDatabase.getDatabase(application).postDao()
        repository = PostRepository(dao)
    }

    // Selected images (local URIs)
    private val _selectedImages = MutableLiveData<List<Uri>>(emptyList())
    val selectedImages: LiveData<List<Uri>> = _selectedImages

    // Weather data
    private val _weatherText = MutableLiveData<String?>()
    val weatherText: LiveData<String?> = _weatherText

    private val _weatherIconUrl = MutableLiveData<String?>()
    val weatherIconUrl: LiveData<String?> = _weatherIconUrl

    // Country data
    private val _countryText = MutableLiveData<String?>()
    val countryText: LiveData<String?> = _countryText

    private val _countryFlagUrl = MutableLiveData<String?>()
    val countryFlagUrl: LiveData<String?> = _countryFlagUrl

    // UI state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _postCreated = MutableLiveData(false)
    val postCreated: LiveData<Boolean> = _postCreated

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Cached API data for post creation
    private var weatherTemp = ""
    private var weatherCondition = ""
    private var weatherIcon = ""
    private var countryFlag = ""
    private var countryCapital = ""
    private var countryPopulation = ""
    private var countryCurrency = ""

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

    fun fetchDestinationData(destination: String) {
        if (destination.isBlank()) return

        val parts = destination.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val city = parts.first()
        val country = if (parts.size > 1) parts.last() else city

        viewModelScope.launch {
            // Fetch weather
            val weather = repository.fetchWeather(city, WEATHER_API_KEY)
            if (weather != null) {
                weatherTemp = "%.1f".format(weather.main.temp)
                weatherCondition = weather.weather.firstOrNull()?.main ?: ""
                weatherIcon = weather.weather.firstOrNull()?.icon ?: ""

                val iconUrl = "https://openweathermap.org/img/wn/${weatherIcon}@2x.png"
                _weatherIconUrl.value = iconUrl
                _weatherText.value = "${weatherTemp}°C, $weatherCondition"
            }

            // Fetch country info
            val countryData = repository.fetchCountryInfo(country)
            if (countryData != null) {
                countryCapital = countryData.capital?.firstOrNull() ?: ""
                countryPopulation = formatPopulation(countryData.population)
                countryCurrency = countryData.currencies?.values?.firstOrNull()?.name ?: ""
                countryFlag = countryData.flags?.png ?: ""

                _countryFlagUrl.value = countryFlag
                _countryText.value = "Capital: $countryCapital\nPopulation: $countryPopulation\nCurrency: $countryCurrency"
            }
        }
    }

    fun submitPost(title: String, description: String, destination: String) {
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
                    destination = destination,
                    countryFlag = countryFlag,
                    countryCapital = countryCapital,
                    countryPopulation = countryPopulation,
                    countryCurrency = countryCurrency,
                    temperature = weatherTemp,
                    weatherCondition = weatherCondition,
                    weatherIcon = weatherIcon,
                    timestamp = System.currentTimeMillis()
                )

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

    private fun formatPopulation(pop: Long?): String {
        if (pop == null) return "N/A"
        return when {
            pop >= 1_000_000 -> "%.1fM".format(pop / 1_000_000.0)
            pop >= 1_000 -> "%.1fK".format(pop / 1_000.0)
            else -> pop.toString()
        }
    }
}
