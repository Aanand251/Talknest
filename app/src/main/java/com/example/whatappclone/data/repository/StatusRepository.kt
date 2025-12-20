package com.example.whatappclone.data.repository

import com.example.whatappclone.data.model.Status
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StatusRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun createStatus(status: Status): Result<Unit> {
        return try {
            val statusId = UUID.randomUUID().toString()
            val statusWithId = status.copy(statusId = statusId)
            
            firestore.collection("status")
                .document(statusId)
                .set(statusWithId.toMap())
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observeStatuses(): Flow<List<Status>> = callbackFlow {
        val currentTime = System.currentTimeMillis()
        
        val listener = firestore.collection("status")
            .whereGreaterThan("expiryTime", currentTime)
            .orderBy("expiryTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val statuses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Status::class.java)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                
                trySend(statuses)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun markStatusAsViewed(statusId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection("status")
                .document(statusId)
                .update("viewedBy", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteStatus(statusId: String): Result<Unit> {
        return try {
            firestore.collection("status")
                .document(statusId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
