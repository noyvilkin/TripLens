package com.colman.triplens.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface CountryService {
    @GET("v3.1/name/{name}?fields=capital,population,currencies,flags,languages")
    suspend fun getCountryByName(@Path("name") name: String): List<CountryResponse>

    /**
     * Fetch all country names from the API.
     * Returns only the "name" field to keep the payload small.
     */
    @GET("v3.1/all?fields=name")
    suspend fun getAllCountries(): List<CountryNameResponse>
}

data class CountryResponse(
    val capital: List<String>?,
    val population: Long?,
    val currencies: Map<String, CurrencyInfo>?,
    val flags: FlagInfo?,
    val languages: Map<String, String>?
)

data class CurrencyInfo(val name: String?)
data class FlagInfo(val png: String?)

/** Lightweight model for the /all?fields=name endpoint */
data class CountryNameResponse(val name: CountryNameInfo?)
data class CountryNameInfo(val common: String?)
