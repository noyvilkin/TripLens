package com.colman.triplens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser

/**
 * ViewModel that mediates between the auth UI (Login / Register / Profile)
 * and the [AuthRepository].
 *
 * Fragments observe [LiveData] properties; they never touch the repository directly.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)

    // ── Observable state exposed to fragments ────────────────────

    private val _currentUser = MutableLiveData<FirebaseUser?>(repository.getCurrentUser())
    /** The currently authenticated Firebase user (null == signed out). */
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    private val _isLoading = MutableLiveData(false)
    /** True while a login / register call is in progress. */
    val isLoading: LiveData<Boolean> = _isLoading

    private val _authError = MutableLiveData<String?>()
    /** Non-null when the last auth operation failed; fragments show a Toast and reset. */
    val authError: LiveData<String?> = _authError

    private val _authSuccess = MutableLiveData(false)
    /** Emits true once after a successful login or register so the fragment can navigate. */
    val authSuccess: LiveData<Boolean> = _authSuccess

    // ── Public API called by fragments ───────────────────────────

    /** Returns true if a valid session exists AND "Stay Logged In" is on. */
    fun shouldAutoLogin(): Boolean =
        repository.isLoggedIn() && repository.isStayLoggedIn()

    fun login(email: String, password: String, stayLoggedIn: Boolean) {
        _isLoading.value = true
        _authError.value = null

        repository.login(email, password) { success, errorMessage ->
            _isLoading.postValue(false)
            if (success) {
                repository.setStayLoggedIn(stayLoggedIn)
                _currentUser.postValue(repository.getCurrentUser())
                _authSuccess.postValue(true)
            } else {
                _authError.postValue(errorMessage ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String) {
        _isLoading.value = true
        _authError.value = null

        repository.register(email, password) { success, errorMessage ->
            _isLoading.postValue(false)
            if (success) {
                // After registration, keep the user logged in by default
                repository.setStayLoggedIn(true)
                _currentUser.postValue(repository.getCurrentUser())
                _authSuccess.postValue(true)
            } else {
                _authError.postValue(errorMessage ?: "Registration failed")
            }
        }
    }

    fun logout() {
        repository.logout()
        _currentUser.value = null
        _authSuccess.value = false
    }

    /** Call after the fragment has consumed the error (e.g. shown a Toast). */
    fun clearError() {
        _authError.value = null
    }

    /** Call after the fragment has consumed the success event (e.g. navigated). */
    fun clearAuthSuccess() {
        _authSuccess.value = false
    }
}
