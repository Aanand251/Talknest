package com.example.whatappclone.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val chatId: String,
    val otherUserId: String, // For 1-to-1 chats
    val otherUserName: String,
    val otherUserImage: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val lastMessageType: String,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val groupName: String = ""
)
