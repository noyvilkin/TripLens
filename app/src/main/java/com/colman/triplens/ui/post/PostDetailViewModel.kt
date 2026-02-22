package com.colman.triplens.ui.post

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.colman.triplens.data.local.AppDatabase
import com.colman.triplens.data.model.Comment
import com.colman.triplens.data.model.Post
import com.colman.triplens.data.repo.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class PostDetailViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PostDetailVM"
    }

    private val repository: PostRepository

    init {
        val dao = AppDatabase.getDatabase(application).postDao()
        repository = PostRepository(dao)
    }

    // ── Post Data ────────────────────────────────────────────────

    private val _postId = MutableLiveData<String>()

    /** The post loaded from Room (reactive) */
    val post: LiveData<Post?> = _postId.switchMap { id ->
        repository.getPostById(id)
    }

    fun loadPost(postId: String) {
        _postId.value = postId
        loadComments(postId)
    }

    // ── Comments (real-time from Firestore) ──────────────────────

    private val _comments = MutableLiveData<List<Comment>>(emptyList())
    val comments: LiveData<List<Comment>> get() = _comments

    private val _isLoadingComments = MutableLiveData(false)
    val isLoadingComments: LiveData<Boolean> get() = _isLoadingComments

    private val _commentError = MutableLiveData<String?>()
    val commentError: LiveData<String?> get() = _commentError

    private fun loadComments(postId: String) {
        _isLoadingComments.value = true
        viewModelScope.launch {
            repository.getCommentsFlow(postId)
                .catch { e ->
                    Log.w(TAG, "Comments flow error: ${e.message}")
                    _isLoadingComments.postValue(false)
                    _commentError.postValue("Could not load comments")
                }
                .collectLatest { commentList ->
                    _comments.postValue(commentList)
                    _isLoadingComments.postValue(false)
                }
        }
    }

    fun addComment(text: String) {
        val postId = _postId.value ?: return
        if (text.isBlank()) return

        val user = FirebaseAuth.getInstance().currentUser
        val comment = Comment(
            id = UUID.randomUUID().toString(),
            postId = postId,
            userId = user?.uid ?: "",
            userName = user?.displayName ?: user?.email ?: "Anonymous",
            userProfileImage = user?.photoUrl?.toString() ?: "",
            text = text.trim(),
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                repository.addComment(comment)
                // No need to manually update _comments — the Flow listener picks it up
            } catch (e: Exception) {
                Log.w(TAG, "Failed to post comment: ${e.message}")
                _commentError.postValue("Failed to post comment")
            }
        }
    }

    fun clearCommentError() {
        _commentError.value = null
    }
}
