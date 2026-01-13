package com.colman.triplens.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.colman.triplens.data.model.Post

class FeedViewModel : ViewModel() {
    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadDummyData()
    }

    private fun loadDummyData() {
        val longList = mutableListOf<Post>()
        for (i in 1..15) {
            longList.add(
                Post(
                    id = "$i",
                    userName = "User $i",
                    title = "Trip to Destination $i",
                    destination = "Location $i",
                    description = "This is a detailed description for trip number $i. #Travel #TripLens",
                    travelImage = "https://picsum.photos/seed/${i+10}/500/300",
                    userProfileImage = "https://i.pravatar.cc/150?u=$i", // Real profile images
                    countryCapital = "Capital $i",
                    countryPopulation = "${i}M",
                    temperature = "${15 + i}",
                    weatherCondition = "Cloudy",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        _posts.value = longList
        _isLoading.value = false
    }
}