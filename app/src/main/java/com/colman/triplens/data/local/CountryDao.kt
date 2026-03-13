package com.colman.triplens.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CountryDao {
    @Query("SELECT name FROM country_names ORDER BY name ASC")
    suspend fun getAllCountryNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCountryNames(countryNames: List<CountryNameEntity>)

    @Query("DELETE FROM country_names")
    suspend fun clearCountryNames()
}

