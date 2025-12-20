package com.example.whatappclone.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SettingsRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun updateLastSeenPrivacy(userId: String, privacy: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update("lastSeenPrivacy", privacy)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateReadReceipts(userId: String, enabled: Boolean): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update("readReceiptsEnabled", enabled)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateNotificationSound(userId: String, sound: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update("notificationSound", sound)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePopupNotifications(userId: String, enabled: Boolean): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update("popupNotificationsEnabled", enabled)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
