package com.colman.triplens.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "country_names")
data class CountryNameEntity(
    @PrimaryKey val name: String
)

