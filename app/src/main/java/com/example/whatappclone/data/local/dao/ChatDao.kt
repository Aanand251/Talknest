package com.example.whatappclone.data.local.dao

import androidx.room.*
import com.example.whatappclone.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)
    
    @Update
    suspend fun updateChat(chat: ChatEntity)
    
    @Delete
    suspend fun deleteChat(chat: ChatEntity)
    
    @Query("SELECT * FROM chats ORDER BY lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>
    
    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?
    
    @Query("UPDATE chats SET unreadCount = :count WHERE chatId = :chatId")
    suspend fun updateUnreadCount(chatId: String, count: Int)
    
    @Query("UPDATE chats SET lastMessage = :message, lastMessageTime = :time, lastMessageType = :type WHERE chatId = :chatId")
    suspend fun updateLastMessage(chatId: String, message: String, time: Long, type: String)
}
