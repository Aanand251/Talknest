package com.example.whatappclone.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatappclone.data.model.Chat
import com.example.whatappclone.data.model.Message
import com.example.whatappclone.data.model.MessageStatus
import com.example.whatappclone.data.model.MessageType
import com.example.whatappclone.data.model.User
import com.example.whatappclone.data.repository.AuthRepository
import com.example.whatappclone.data.repository.ChatRepository
import com.example.whatappclone.data.repository.StorageRepository
import com.example.whatappclone.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val chatRepository = ChatRepository(application.applicationContext)
    private val storageRepository = StorageRepository()
    private val authRepository = AuthRepository()
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()
    
    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat.asStateFlow()
    
    private val _sendMessageState = MutableStateFlow<Resource<Unit>>(Resource.Loading())
    val sendMessageState: StateFlow<Resource<Unit>> = _sendMessageState.asStateFlow()
    
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()
    
    private val _selectedChatUser = MutableStateFlow<User?>(null)
    val selectedChatUser: StateFlow<User?> = _selectedChatUser.asStateFlow()
    
    fun loadChats(userId: String) {
        viewModelScope.launch {
            chatRepository.observeChats(userId).collect { chatList ->
                _chats.value = chatList
            }
        }
    }
    
    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            chatRepository.observeMessages(chatId).collect { messageList ->
                _messages.value = messageList
                // Mark messages as read
                val currentUserId = authRepository.getCurrentUserId()
                currentUserId?.let { userId ->
                    messageList.forEach { message ->
                        if (message.receiverId == userId && message.status != MessageStatus.SEEN) {
                            updateMessageStatus(chatId, message.messageId, MessageStatus.SEEN)
                        }
                    }
                }
            }
        }
    }
    
    fun sendTextMessage(chatId: String, receiverId: String, text: String) {
        viewModelScope.launch {
            _sendMessageState.value = Resource.Loading()
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            
            val message = Message(
                chatId = chatId,
                senderId = currentUserId,
                receiverId = receiverId,
                text = text,
                type = MessageType.TEXT,
                status = MessageStatus.SENT
            )
            
            val result = chatRepository.sendMessage(message)
            result.onSuccess {
                _sendMessageState.value = Resource.Success(Unit)
            }.onFailure { exception ->
                _sendMessageState.value = Resource.Error(exception.message ?: "Failed to send message")
            }
        }
    }
    
    fun sendMediaMessage(
        chatId: String,
        receiverId: String,
        uri: Uri,
        messageType: MessageType,
        caption: String = ""
    ) {
        viewModelScope.launch {
            _sendMessageState.value = Resource.Loading()
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            
            // Upload media first
            val uploadResult = when (messageType) {
                MessageType.IMAGE -> storageRepository.uploadImage(uri)
                MessageType.VIDEO -> storageRepository.uploadVideo(uri)
                MessageType.AUDIO -> storageRepository.uploadAudio(uri)
                MessageType.DOCUMENT -> storageRepository.uploadDocument(uri, "document")
                else -> return@launch
            }
            
            uploadResult.onSuccess { mediaUrl ->
                val message = Message(
                    chatId = chatId,
                    senderId = currentUserId,
                    receiverId = receiverId,
                    text = caption,
                    mediaUrl = mediaUrl,
                    type = messageType,
                    status = MessageStatus.SENT
                )
                
                val result = chatRepository.sendMessage(message)
                result.onSuccess {
                    _sendMessageState.value = Resource.Success(Unit)
                }.onFailure { exception ->
                    _sendMessageState.value = Resource.Error(exception.message ?: "Failed to send message")
                }
            }.onFailure { exception ->
                _sendMessageState.value = Resource.Error(exception.message ?: "Failed to upload media")
            }
        }
    }
    
    fun updateMessageStatus(chatId: String, messageId: String, status: MessageStatus) {
        viewModelScope.launch {
            chatRepository.updateMessageStatus(chatId, messageId, status)
        }
    }
    
    fun deleteMessage(chatId: String, messageId: String, deleteForEveryone: Boolean) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            chatRepository.deleteMessage(chatId, messageId, currentUserId, deleteForEveryone)
        }
    }
    
    fun updateTypingStatus(chatId: String, isTyping: Boolean) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            chatRepository.updateTypingStatus(chatId, currentUserId, isTyping)
        }
    }
    
    fun getOrCreateChat(otherUserId: String) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            val result = chatRepository.getOrCreateChat(currentUserId, otherUserId)
            result.onSuccess { chatId ->
                loadMessages(chatId)
            }
        }
    }
    
    fun loadAllUsers() {
        viewModelScope.launch {
            val result = authRepository.getAllUsers()
            result.onSuccess { userList ->
                _users.value = userList
            }
        }
    }
    
    fun setSelectedChatUser(user: User) {
        _selectedChatUser.value = user
    }
    
    fun observeUser(userId: String) {
        viewModelScope.launch {
            authRepository.observeUserProfile(userId).collect { user ->
                _selectedChatUser.value = user
            }
        }
    }
    
    fun resetSendMessageState() {
        _sendMessageState.value = Resource.Loading()
    }
}
