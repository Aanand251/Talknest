package com.example.whatappclone.data.repository

import android.content.Context
import com.example.whatappclone.data.model.Chat
import com.example.whatappclone.data.model.Message
import com.example.whatappclone.data.model.MessageStatus
import com.example.whatappclone.util.EncryptionHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class ChatRepository(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val encryptionHelper by lazy { EncryptionHelper.getInstance(context) }
    
    fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }
    
    suspend fun getOrCreateChat(currentUserId: String, otherUserId: String): Result<String> {
        return try {
            val chatId = getChatId(currentUserId, otherUserId)
            val chatDoc = firestore.collection("chats").document(chatId).get().await()
            
            if (!chatDoc.exists()) {
                val chat = Chat(
                    chatId = chatId,
                    participants = listOf(currentUserId, otherUserId),
                    createdBy = currentUserId
                )
                firestore.collection("chats").document(chatId).set(chat.toMap()).await()
            }
            
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendMessage(message: Message): Result<Unit> {
        return try {
            android.util.Log.d("ChatRepository", "sendMessage: START")
            
            val messageId = UUID.randomUUID().toString()
            
            // Send without encryption for now (encryption helper failing)
            val messageToSend = message.copy(
                messageId = messageId,
                isEncrypted = false
            )
            
            android.util.Log.d("ChatRepository", "sendMessage: Writing to Firestore...")
        
            // Send message to Firestore
            firestore.collection("chats")
                .document(message.chatId)
                .collection("messages")
                .document(messageId)
                .set(messageToSend.toMap())
                .await()
            
            android.util.Log.d("ChatRepository", "sendMessage: SUCCESS!")

            // Update chat last message
            val lastMessagePreview = when(message.type.name) {
                "TEXT" -> message.text.take(50)
                "IMAGE" -> "📷 Photo"
                "VIDEO" -> "🎥 Video"
                "AUDIO" -> "🎤 Voice"
                "DOCUMENT" -> "📄 Document"
                else -> "Message"
            }
            
            val chatUpdates = hashMapOf<String, Any>(
                "lastMessage" to lastMessagePreview,
                "lastMessageTime" to message.timestamp,
                "lastMessageType" to message.type.name
            )
            
            firestore.collection("chats")
                .document(message.chatId)
                .update(chatUpdates)
                .await()
            
            android.util.Log.d("ChatRepository", "sendMessage: Chat updated!")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "sendMessage: FAILED", e)
            Result.failure(e)
        }
    }
    

    private fun decryptMessage(message: Message): Message {
        // Return as is (no encryption/decryption for now)
        return message
    }
    
    fun observeMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        if (chatId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        android.util.Log.d("ChatRepository", "observeMessages: Starting listener for chatId: $chatId")
        
        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ChatRepository", "observeMessages: Error", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                android.util.Log.d("ChatRepository", "observeMessages: Snapshot received with ${snapshot?.documents?.size ?: 0} documents")
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val message = doc.toObject(Message::class.java)
                        val messageWithStatus = message?.copy(
                            status = MessageStatus.valueOf(doc.getString("status") ?: "SENT"),
                            type = com.example.whatappclone.data.model.MessageType.valueOf(
                                doc.getString("type") ?: "TEXT"
                            )
                        )

                        messageWithStatus?.let { decryptMessage(it) }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatRepository", "observeMessages: Error parsing message", e)
                        null
                    }
                } ?: emptyList()
                
                android.util.Log.d("ChatRepository", "observeMessages: Emitting ${messages.size} messages")
                trySend(messages)
            }
        
        awaitClose { 
            android.util.Log.d("ChatRepository", "observeMessages: Closing listener for chatId: $chatId")
            listener.remove() 
        }
    }
    
    fun observeChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)
                }?.sortedByDescending { it.lastMessageTime } ?: emptyList()
                
                trySend(chats)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun updateMessageStatus(
        chatId: String,
        messageId: String,
        status: MessageStatus
    ): Result<Unit> {
        return try {
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update("status", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteMessage(chatId: String, messageId: String, userId: String, deleteForEveryone: Boolean): Result<Unit> {
        return try {
            if (deleteForEveryone) {
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(messageId)
                    .update(
                        mapOf(
                            "isDeleted" to true,
                            "text" to "This message was deleted"
                        )
                    )
                    .await()
            } else {
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(messageId)
                    .update("deletedBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateTypingStatus(chatId: String, userId: String, isTyping: Boolean): Result<Unit> {
        return try {
            val fieldValue = if (isTyping) {
                com.google.firebase.firestore.FieldValue.arrayUnion(userId)
            } else {
                com.google.firebase.firestore.FieldValue.arrayRemove(userId)
            }
            
            firestore.collection("chats")
                .document(chatId)
                .update("typingUsers", fieldValue)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
