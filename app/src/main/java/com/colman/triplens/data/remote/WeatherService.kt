package com.colman.triplens.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

data class WeatherResponse(
    val main: WeatherMain,
    val weather: List<WeatherCondition>,
    val wind: Wind?
)

data class WeatherMain(
    val temp: Double,
    val humidity: Int?
)

data class WeatherCondition(
    val main: String,
    val icon: String
)

data class Wind(
    val speed: Double
)
