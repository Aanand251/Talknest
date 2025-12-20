package com.example.whatappclone.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.whatappclone.data.model.MessageStatus
import com.example.whatappclone.data.model.MessageType

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val mediaUrl: String,
    val mediaType: String,
    val timestamp: Long,
    val status: String, // Store as String
    val type: String, // Store as String
    val isDeleted: Boolean = false
)
