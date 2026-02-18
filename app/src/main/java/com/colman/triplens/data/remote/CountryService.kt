package com.colman.triplens.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface CountryService {
    @GET("v3.1/name/{name}?fields=capital,population,currencies,flags")
    suspend fun getCountryByName(@Path("name") name: String): List<CountryResponse>
}

data class CountryResponse(
    val capital: List<String>?,
    val population: Long?,
    val currencies: Map<String, CurrencyInfo>?,
    val flags: FlagInfo?
)

data class CurrencyInfo(val name: String?)
data class FlagInfo(val png: String?)
