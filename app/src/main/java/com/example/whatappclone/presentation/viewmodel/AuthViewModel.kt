package com.example.whatappclone.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatappclone.data.model.User
import com.example.whatappclone.data.repository.AuthRepository
import com.example.whatappclone.util.Resource
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    private val authRepository = AuthRepository()
    
    // Initialize with null to indicate "not yet authenticated"
    private val _authState = MutableStateFlow<Resource<String?>>(Resource.Success(null))
    val authState: StateFlow<Resource<String?>> = _authState.asStateFlow()
    
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()
    
    private val _profileCreationState = MutableStateFlow<Resource<Unit>>(Resource.Loading())
    val profileCreationState: StateFlow<Resource<Unit>> = _profileCreationState.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    private fun checkAuthState() {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            _authState.value = Resource.Success(userId)
            loadUserProfile(userId)
        }
    }
    
    // Email Authentication
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            val result = authRepository.signInWithEmail(email, password)
            
            result.onSuccess { userId ->
                _authState.value = Resource.Success(userId)
                // Check if user profile exists
                val profileResult = authRepository.getUserProfile(userId)
                profileResult.onSuccess { user ->
                    _userProfile.value = user
                }
            }.onFailure { exception ->
                _authState.value = Resource.Error(exception.message ?: "Sign in failed")
            }
        }
    }
    
    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            val result = authRepository.signUpWithEmail(email, password)
            
            result.onSuccess { userId ->
                _authState.value = Resource.Success(userId)
                // User profile needs to be created
                _userProfile.value = null
            }.onFailure { exception ->
                _authState.value = Resource.Error(exception.message ?: "Sign up failed")
            }
        }
    }
    
    // Phone Authentication (keep for future use)
    fun signInWithCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _authState.value = Resource.Loading()
            val result = authRepository.signInWithPhoneCredential(credential)
            
            result.onSuccess { userId ->
                _authState.value = Resource.Success(userId)
                // Check if user profile exists
                val profileResult = authRepository.getUserProfile(userId)
                profileResult.onSuccess { user ->
                    _userProfile.value = user
                }
            }.onFailure { exception ->
                _authState.value = Resource.Error(exception.message ?: "Authentication failed")
            }
        }
    }
    
    fun createUserProfile(user: User) {
        viewModelScope.launch {
            _profileCreationState.value = Resource.Loading()
            val result = authRepository.createUserProfile(user)
            
            result.onSuccess {
                _profileCreationState.value = Resource.Success(Unit)
                _userProfile.value = user
                setUserOnline(user.userId)
            }.onFailure { exception ->
                _profileCreationState.value = Resource.Error(exception.message ?: "Failed to create profile")
            }
        }
    }
    
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            val result = authRepository.getUserProfile(userId)
            result.onSuccess { user ->
                _userProfile.value = user
                user?.let { setUserOnline(it.userId) }
            }
        }
    }
    
    fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            authRepository.updateUserProfile(userId, updates)
            loadUserProfile(userId)
        }
    }
    
    fun setUserOnline(userId: String) {
        viewModelScope.launch {
            authRepository.updateOnlineStatus(userId, true)
        }
    }
    
    fun setUserOffline(userId: String) {
        viewModelScope.launch {
            authRepository.updateOnlineStatus(userId, false)
        }
    }
    
    fun getCurrentUserId(): String? = authRepository.getCurrentUserId()
    
    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()
    
    fun uploadProfilePicture(uri: android.net.Uri) {
        viewModelScope.launch {
            val userId = getCurrentUserId() ?: return@launch
            
            // Upload to Firebase Storage
            val storageRepository = com.example.whatappclone.data.repository.StorageRepository()
            val result = storageRepository.uploadProfileImage(uri, userId)
            
            result.onSuccess { downloadUrl ->
                // Update user profile with new image URL
                _userProfile.value = _userProfile.value?.copy(profileImageUrl = downloadUrl)
            }
        }
    }
    
    fun signOut() {
        val userId = getCurrentUserId()
        userId?.let { setUserOffline(it) }
        authRepository.signOut()
        _authState.value = Resource.Success(null)
        _userProfile.value = null
    }
    
    fun resetProfileCreationState() {
        _profileCreationState.value = Resource.Loading()
    }
}
