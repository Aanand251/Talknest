package com.example.whatappclone.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatappclone.data.model.Status
import com.example.whatappclone.data.model.User
import com.example.whatappclone.data.repository.AuthRepository
import com.example.whatappclone.data.repository.StatusRepository
import com.example.whatappclone.data.repository.StorageRepository
import com.example.whatappclone.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatusViewModel : ViewModel() {
    
    private val statusRepository = StatusRepository()
    private val storageRepository = StorageRepository()
    private val authRepository = AuthRepository()
    
    private val _statuses = MutableStateFlow<List<Status>>(emptyList())
    val statuses: StateFlow<List<Status>> = _statuses.asStateFlow()
    
    private val _uploadState = MutableStateFlow<Resource<Unit>>(Resource.Loading())
    val uploadState: StateFlow<Resource<Unit>> = _uploadState.asStateFlow()
    
    fun loadStatuses() {
        viewModelScope.launch {
            statusRepository.observeStatuses().collect { statusList ->
                _statuses.value = statusList
            }
        }
    }
    
    fun createStatus(uri: Uri, isVideo: Boolean, caption: String = "") {
        viewModelScope.launch {
            _uploadState.value = Resource.Loading()
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            
            // Get current user profile
            val userResult = authRepository.getUserProfile(currentUserId)
            userResult.onSuccess { user ->
                if (user == null) return@onSuccess
                
                // Upload media
                val uploadResult = storageRepository.uploadStatusMedia(uri, currentUserId, isVideo)
                uploadResult.onSuccess { mediaUrl ->
                    val status = Status(
                        userId = currentUserId,
                        userName = user.name,
                        userProfileImage = user.profileImageUrl,
                        mediaUrl = mediaUrl,
                        mediaType = if (isVideo) "video" else "image",
                        caption = caption
                    )
                    
                    val result = statusRepository.createStatus(status)
                    result.onSuccess {
                        _uploadState.value = Resource.Success(Unit)
                    }.onFailure { exception ->
                        _uploadState.value = Resource.Error(exception.message ?: "Failed to create status")
                    }
                }.onFailure { exception ->
                    _uploadState.value = Resource.Error(exception.message ?: "Failed to upload media")
                }
            }.onFailure { exception ->
                _uploadState.value = Resource.Error(exception.message ?: "Failed to get user profile")
            }
        }
    }
    
    fun markStatusAsViewed(statusId: String) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            statusRepository.markStatusAsViewed(statusId, currentUserId)
        }
    }
    
    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            statusRepository.deleteStatus(statusId)
        }
    }
    
    fun resetUploadState() {
        _uploadState.value = Resource.Loading()
    }
}
