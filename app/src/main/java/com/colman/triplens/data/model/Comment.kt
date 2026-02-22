package com.colman.triplens.data.model

/**
 * Comment model stored as a Firestore sub-collection under each post.
 * Not persisted in Room — always fetched in real-time from Firestore.
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
