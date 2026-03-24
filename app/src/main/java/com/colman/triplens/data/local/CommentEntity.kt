package com.colman.triplens.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "comments",
    indices = [Index(value = ["postId"]), Index(value = ["timestamp"])]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val userId: String,
    val userName: String,
    val userProfileImage: String,
    val text: String,
    val timestamp: Long
)

