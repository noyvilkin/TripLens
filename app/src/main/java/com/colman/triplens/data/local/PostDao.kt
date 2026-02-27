package com.colman.triplens.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.colman.triplens.data.model.Post

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPostsByUserId(userId: String): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    fun getPostById(postId: String): LiveData<Post?>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostByIdSync(postId: String): Post?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("UPDATE posts SET userName = :userName, userProfileImage = :profileImage WHERE userId = :userId")
    suspend fun updateUserInfoInPosts(userId: String, userName: String, profileImage: String)

    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()
}
