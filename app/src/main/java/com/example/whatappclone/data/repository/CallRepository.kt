package com.example.whatappclone.data.repository

import android.util.Log
import com.example.whatappclone.data.model.Call
import com.example.whatappclone.data.model.CallStatus
import com.example.whatappclone.data.model.CallType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CallRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "CallRepository"
    
    // Create new call
    suspend fun initiateCall(
        receiverId: String,
        receiverName: String,
        receiverImage: String,
        callType: CallType
    ): Result<String> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not logged in"))
            val callId = UUID.randomUUID().toString()
            
            val call = Call(
                callId = callId,
                callerId = currentUser.uid,
                callerName = currentUser.displayName ?: "Unknown",
                callerImage = currentUser.photoUrl?.toString() ?: "",
                receiverId = receiverId,
                receiverName = receiverName,
                receiverImage = receiverImage,
                callType = callType,
                callStatus = CallStatus.RINGING,
                timestamp = System.currentTimeMillis()
            )
            
            firestore.collection("calls")
                .document(callId)
                .set(call.toMap())
                .await()
            
            Log.d(TAG, "Call initiated: $callId")
            Result.success(callId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate call", e)
            Result.failure(e)
        }
    }
    
    // Update call status
    suspend fun updateCallStatus(callId: String, status: CallStatus): Result<Unit> {
        return try {
            firestore.collection("calls")
                .document(callId)
                .update("callStatus", status.name)
                .await()
            
            Log.d(TAG, "Call status updated: $callId -> $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update call status", e)
            Result.failure(e)
        }
    }
    
    // End call
    suspend fun endCall(callId: String, duration: Long): Result<Unit> {
        return try {
            firestore.collection("calls")
                .document(callId)
                .update(
                    mapOf(
                        "callStatus" to CallStatus.ENDED.name,
                        "duration" to duration,
                        "endTime" to System.currentTimeMillis()
                    )
                )
                .await()
            
            Log.d(TAG, "Call ended: $callId, duration: ${duration}ms")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end call", e)
            Result.failure(e)
        }
    }
    
    // Listen for incoming calls
    fun listenForIncomingCalls(): Flow<Call?> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            close()
            return@callbackFlow
        }
        
        val listener: ListenerRegistration = firestore.collection("calls")
            .whereEqualTo("receiverId", currentUser.uid)
            .whereEqualTo("callStatus", CallStatus.RINGING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for calls", error)
                    return@addSnapshotListener
                }
                
                val calls = snapshot?.documents?.mapNotNull {
                    try {
                        Call.fromMap(it.data ?: emptyMap())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing call", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(calls.firstOrNull())
            }
        
        awaitClose { listener.remove() }
    }
    
    // Listen for call updates
    fun listenForCallUpdates(callId: String): Flow<Call?> = callbackFlow {
        val listener: ListenerRegistration = firestore.collection("calls")
            .document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for call updates", error)
                    return@addSnapshotListener
                }
                
                val call = try {
                    snapshot?.data?.let { Call.fromMap(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing call update", e)
                    null
                }
                
                trySend(call)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Get call by ID
    suspend fun getCall(callId: String): Result<Call?> {
        return try {
            val doc = firestore.collection("calls")
                .document(callId)
                .get()
                .await()
            
            val call = doc.data?.let { Call.fromMap(it) }
            Result.success(call)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get call", e)
            Result.failure(e)
        }
    }
    
    // Answer call
    suspend fun answerCall(callId: String): Result<Unit> {
        return updateCallStatus(callId, CallStatus.CONNECTING)
    }
    
    // Reject call
    suspend fun rejectCall(callId: String): Result<Unit> {
        return updateCallStatus(callId, CallStatus.REJECTED)
    }
}
