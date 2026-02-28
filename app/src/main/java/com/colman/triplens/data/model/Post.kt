package com.colman.triplens.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val travelImage: String = "",
    val imageUrls: String = "",        // comma-separated URLs for multi-image
    val title: String = "",
    val description: String = "",      // Short caption shown in feed preview
    val longDescription: String = "",  // Detailed description shown in PostDetail
    val destination: String = "",

    // Country Data (RestCountries API)
    val countryFlag: String = "",
    val countryCapital: String = "",
    val countryPopulation: String = "",
    val countryCurrency: String = "",
    val countryLanguages: String = "",

    // Weather Data (OpenWeather API)
    val temperature: String = "",
    val weatherCondition: String = "",
    val weatherIcon: String = "",
    val humidity: String = "",
    val windSpeed: String = "",

    val timestamp: Long = System.currentTimeMillis()
)
