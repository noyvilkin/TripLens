package com.colman.triplens.data.model

/**
 * Comment model stored in Firestore (source of truth) and cached in Room for offline reads.
 */
data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
