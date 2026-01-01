package com.example.whatappclone.data.repository

import android.content.Context
import android.util.Log
import com.example.whatappclone.data.model.Call
import com.example.whatappclone.data.model.CallStatus
import com.example.whatappclone.data.model.CallType
import com.example.whatappclone.data.model.Message
import com.example.whatappclone.data.model.MessageStatus
import com.example.whatappclone.data.model.MessageType
import com.example.whatappclone.utils.NetworkStatusManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class ImprovedCallRepository(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val networkManager = NetworkStatusManager(context)
    private val TAG = "ImprovedCallRepository"
    
    companion object {
        const val CALL_TIMEOUT_MS = 30000L // 30 seconds
    }
    
    /**
     * Check if receiver is online - with quick timeout for better UX
     */
    suspend fun checkUserOnlineStatus(userId: String): Boolean {
        return try {
            Log.d(TAG, "checkUserOnlineStatus: Checking user $userId...")
            
            val snapshot = withTimeoutOrNull(2000L) { // 2 second timeout
                realtimeDb.reference
                    .child("users")
                    .child(userId)
                    .child("online")
                    .get()
                    .await()
            }
            
            if (snapshot == null) {
                Log.d(TAG, "checkUserOnlineStatus: Timeout - will try call anyway")
                return true // Timeout - assume available
            }
            
            val isOnline = snapshot.getValue(Boolean::class.java) ?: true
            Log.d(TAG, "checkUserOnlineStatus: Result = $isOnline")
            isOnline
        } catch (e: Exception) {
            Log.e(TAG, "checkUserOnlineStatus: Error", e)
            true // Assume available on error
        }
    }
    
    /**
     * Initiate call with network check
     */
    suspend fun initiateCall(
        receiverId: String,
        receiverName: String,
        receiverImage: String,
        callType: CallType
    ): Result<Pair<String, Boolean>> = withContext(Dispatchers.IO) { // Run on IO thread
        return@withContext try {
            Log.d(TAG, "initiateCall: Starting...")
            
            // Check if caller has network
            if (!networkManager.isOnline()) {
                Log.d(TAG, "initiateCall: No internet")
                return@withContext Result.failure(Exception("No internet connection"))
            }
            
            Log.d(TAG, "initiateCall: Network OK")
            
            val currentUser = auth.currentUser 
            if (currentUser == null) {
                Log.d(TAG, "initiateCall: Not logged in")
                return@withContext Result.failure(Exception("Not logged in"))
            }
            
            Log.d(TAG, "initiateCall: User logged in: ${currentUser.uid}")
            
            // Check if receiver is online
            Log.d(TAG, "initiateCall: Checking receiver online status...")
            val isReceiverOnline = checkUserOnlineStatus(receiverId)
            Log.d(TAG, "initiateCall: Receiver online: $isReceiverOnline")
            
            val callId = UUID.randomUUID().toString()
            Log.d(TAG, "initiateCall: Generated callId: $callId")
            
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
            
            Log.d(TAG, "initiateCall: Storing in Firestore...")
            // Store in Firestore with 5 second timeout
            val firestoreSuccess = withTimeoutOrNull(5000L) {
                firestore.collection("calls")
                    .document(callId)
                    .set(call.toMap())
                    .await()
                true
            }
            
            if (firestoreSuccess == null) {
                Log.e(TAG, "Firestore write timeout!")
                return@withContext Result.failure(Exception("Failed to create call - timeout"))
            }
            
            Log.d(TAG, "initiateCall: Firestore OK!")
            // NOTE: Skipping Realtime DB storage for now (add it later if needed)
            
            Log.d(TAG, "Call initiated successfully: $callId, Receiver online: $isReceiverOnline")
            Result.success(Pair(callId, isReceiverOnline))
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate call", e)
            Result.failure(e)
        }
    }
    
    /**
     * Answer incoming call
     */
    suspend fun answerCall(callId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "callStatus" to CallStatus.ANSWERED.name,
                "answeredAt" to System.currentTimeMillis()
            )
            
            // Update in both databases
            firestore.collection("calls")
                .document(callId)
                .update(updates)
                .await()
            
            realtimeDb.reference
                .child("active_calls")
                .child(callId)
                .updateChildren(updates)
                .await()
            
            Log.d(TAG, "Call answered: $callId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to answer call", e)
            Result.failure(e)
        }
    }
    
    /**
     * Reject incoming call
     */
    suspend fun rejectCall(callId: String): Result<Unit> {
        return try {
            // Update status
            updateCallStatus(callId, CallStatus.REJECTED)
            
            // Create missed call message for caller
            val call = getCall(callId)
            call?.let {
                createMissedCallMessage(
                    chatId = getChatId(it.callerId, it.receiverId),
                    callerId = it.callerId,
                    receiverId = it.receiverId,
                    callType = it.callType,
                    status = "rejected"
                )
            }
            
            Log.d(TAG, "Call rejected: $callId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reject call", e)
            Result.failure(e)
        }
    }
    
    /**
     * End active call with duration
     */
    suspend fun endCall(callId: String, duration: Long): Result<Unit> {
        return try {
            val updates = mapOf(
                "callStatus" to CallStatus.ENDED.name,
                "duration" to duration,
                "endedAt" to System.currentTimeMillis()
            )
            
            firestore.collection("calls")
                .document(callId)
                .update(updates)
                .await()
            
            // Remove from active calls
            realtimeDb.reference
                .child("active_calls")
                .child(callId)
                .removeValue()
                .await()
            
            // Save to call history
            saveCallHistory(callId)
            
            Log.d(TAG, "Call ended: $callId, duration: ${duration}s")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to end call", e)
            Result.failure(e)
        }
    }
    
    /**
     * Handle call timeout (no answer after 30s)
     */
    suspend fun handleCallTimeout(callId: String): Result<Unit> {
        return try {
            val call = getCall(callId)
            
            if (call?.callStatus == CallStatus.RINGING) {
                // Mark as missed
                updateCallStatus(callId, CallStatus.MISSED)
                
                // Create missed call message
                call.let {
                    createMissedCallMessage(
                        chatId = getChatId(it.callerId, it.receiverId),
                        callerId = it.callerId,
                        receiverId = it.receiverId,
                        callType = it.callType,
                        status = "missed"
                    )
                }
                
                // Remove from active calls
                realtimeDb.reference
                    .child("active_calls")
                    .child(callId)
                    .removeValue()
                    .await()
                
                Log.d(TAG, "Call timeout: $callId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle timeout", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update call status - Firestore only
     */
    suspend fun updateCallStatus(callId: String, status: CallStatus): Result<Unit> {
        return try {
            val updates = mapOf("callStatus" to status.name)
            
            firestore.collection("calls")
                .document(callId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Call status updated: $callId -> $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update call status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Listen for incoming calls - using Firestore (no Realtime DB permission needed)
     * Only listens for NEW calls (within last 30 seconds)
     */
    fun listenForIncomingCalls(): Flow<Call?> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            close()
            return@callbackFlow
        }
        
        // Track processed calls to avoid duplicates
        val processedCalls = mutableSetOf<String>()
        
        Log.d(TAG, "Listening for incoming calls (Firestore) for user: $currentUserId")
        
        val listener = firestore.collection("calls")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("callStatus", CallStatus.RINGING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen for calls error", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && !snapshot.isEmpty) {
                    val currentTime = System.currentTimeMillis()
                    
                    // Filter only NEW calls (within last 30 seconds)
                    val newCalls = snapshot.documents.mapNotNull { doc ->
                        val call = Call.fromMap(doc.data ?: return@mapNotNull null)
                        val callAge = currentTime - call.timestamp
                        
                        // Only process if:
                        // 1. Call is less than 30 seconds old
                        // 2. Not already processed
                        if (callAge < CALL_TIMEOUT_MS && !processedCalls.contains(call.callId)) {
                            call
                        } else {
                            if (callAge >= CALL_TIMEOUT_MS) {
                                Log.d(TAG, "Ignoring old call: ${call.callId}, age: ${callAge}ms")
                                // Auto-mark old ringing calls as missed (in background)
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                    updateCallStatus(call.callId, CallStatus.MISSED)
                                }
                            }
                            null
                        }
                    }
                    
                    // Get the most recent new call
                    newCalls.maxByOrNull { it.timestamp }?.let { call ->
                        Log.d(TAG, "New incoming call detected: ${call.callId} from ${call.callerName}")
                        processedCalls.add(call.callId)
                        trySend(call)
                    }
                } else {
                    Log.d(TAG, "No incoming calls")
                }
            }
        
        awaitClose {
            Log.d(TAG, "Stopped listening for incoming calls")
            processedCalls.clear()
            listener.remove()
        }
    }
    
    /**
     * Listen for call updates - using Firestore (no Realtime DB permission needed)
     */
    fun listenForCallUpdates(callId: String): Flow<Call?> = callbackFlow {
        Log.d(TAG, "Listening for call updates (Firestore): $callId")
        
        val listener = firestore.collection("calls")
            .document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Listen for call updates error", error)
                    return@addSnapshotListener
                }
                
                val call = snapshot?.data?.let { 
                    Call.fromMap(it)
                }
                if (call != null) {
                    Log.d(TAG, "Call update: ${call.callId} -> ${call.callStatus}")
                    trySend(call)
                }
            }
        
        awaitClose {
            listener.remove()
        }
    }
    
    /**
     * Get call by ID
     */
    suspend fun getCall(callId: String): Call? {
        return try {
            val snapshot = firestore.collection("calls")
                .document(callId)
                .get()
                .await()
            
            snapshot.data?.let { Call.fromMap(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get call", e)
            null
        }
    }
    
    /**
     * Save call to history
     */
    private suspend fun saveCallHistory(callId: String) {
        try {
            val call = getCall(callId) ?: return
            
            // Save for caller
            firestore.collection("users")
                .document(call.callerId)
                .collection("call_history")
                .document(callId)
                .set(call.toMap())
                .await()
            
            // Save for receiver
            firestore.collection("users")
                .document(call.receiverId)
                .collection("call_history")
                .document(callId)
                .set(call.toMap())
                .await()
            
            Log.d(TAG, "Call saved to history: $callId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save call history", e)
        }
    }
    
    /**
     * Create missed call message in chat
     */
    private suspend fun createMissedCallMessage(
        chatId: String,
        callerId: String,
        receiverId: String,
        callType: CallType,
        status: String // "missed" or "rejected"
    ) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return
            
            val messageText = when (status) {
                "missed" -> if (currentUserId == callerId) {
                    "📞 Missed ${if (callType == CallType.VIDEO) "video" else "voice"} call"
                } else {
                    "📞 You missed a ${if (callType == CallType.VIDEO) "video" else "voice"} call"
                }
                "rejected" -> "📞 Call declined"
                else -> "📞 Missed call"
            }
            
            val message = Message(
                messageId = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = callerId,
                receiverId = receiverId,
                text = messageText,
                type = MessageType.TEXT,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENT
            )
            
            // Add to chat messages
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(message.messageId)
                .set(message.toMap())
                .await()
            
            // Update chat's last message
            firestore.collection("chats")
                .document(chatId)
                .update(
                    mapOf(
                        "lastMessage" to messageText,
                        "lastMessageTime" to message.timestamp,
                        "lastMessageSenderId" to callerId
                    )
                )
                .await()
            
            Log.d(TAG, "Missed call message created in chat: $chatId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create missed call message", e)
        }
    }
    
    /**
     * Generate chat ID from two user IDs
     */
    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_${userId2}"
        } else {
            "${userId2}_${userId1}"
        }
    }
    
    /**
     * Send signaling data (WebRTC offer/answer/ICE)
     */
    suspend fun sendSignalingData(
        callId: String,
        type: String,
        data: String
    ): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            
            if (type == "ice_candidate") {
                // ICE candidates need to be stored as array (multiple candidates)
                val candidateId = UUID.randomUUID().toString()
                val signalingData = mapOf(
                    "type" to type,
                    "data" to data,
                    "senderId" to currentUserId,
                    "timestamp" to System.currentTimeMillis()
                )
                
                realtimeDb.reference
                    .child("signaling")
                    .child(callId)
                    .child("${type}_${currentUserId}")  // Separate ICE candidates per user
                    .child(candidateId)
                    .setValue(signalingData)
                    .await()
                    
                Log.d(TAG, "ICE Candidate sent: $candidateId from $currentUserId")
            } else {
                // Offer/Answer - single value
                val signalingData = mapOf(
                    "type" to type,
                    "data" to data,
                    "senderId" to currentUserId,
                    "timestamp" to System.currentTimeMillis()
                )
                
                realtimeDb.reference
                    .child("signaling")
                    .child(callId)
                    .child(type)
                    .setValue(signalingData)
                    .await()
                    
                Log.d(TAG, "Signaling data sent: $type from $currentUserId, data length: ${data.length}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send signaling data: $type", e)
            Result.failure(e)
        }
    }
    
    /**
     * Listen for signaling data (offer/answer)
     */
    fun listenForSignalingData(callId: String, type: String): Flow<String?> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: ""
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.child("data").getValue(String::class.java)
                val senderId = snapshot.child("senderId").getValue(String::class.java)
                
                // Only process if it's from the other user
                if (data != null && senderId != currentUserId) {
                    Log.d(TAG, "Received signaling data: $type from $senderId, data length: ${data.length}")
                    trySend(data)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Listen for signaling cancelled", error.toException())
            }
        }
        
        val ref = realtimeDb.reference
            .child("signaling")
            .child(callId)
            .child(type)
        
        ref.addValueEventListener(listener)
        Log.d(TAG, "Started listening for: $type on callId: $callId")
        
        awaitClose {
            ref.removeEventListener(listener)
            Log.d(TAG, "Stopped listening for: $type")
        }
    }
    
    /**
     * Listen for ICE candidates from other user
     */
    fun listenForIceCandidates(callId: String): Flow<String> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: ""
        val processedCandidates = mutableSetOf<String>()
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Listen for ICE candidates from ALL other users (not current user)
                for (userSnapshot in snapshot.children) {
                    val key = userSnapshot.key ?: continue
                    
                    // Skip our own ICE candidates
                    if (key.contains(currentUserId)) continue
                    
                    // Process each candidate from other users
                    for (candidateSnapshot in userSnapshot.children) {
                        val candidateId = candidateSnapshot.key ?: continue
                        
                        // Skip already processed candidates
                        if (processedCandidates.contains(candidateId)) continue
                        processedCandidates.add(candidateId)
                        
                        val data = candidateSnapshot.child("data").getValue(String::class.java)
                        if (data != null) {
                            Log.d(TAG, "Received ICE candidate from $key: $candidateId")
                            trySend(data)
                        }
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Listen for ICE candidates cancelled", error.toException())
            }
        }
        
        // Listen for all ice_candidate_* children
        val ref = realtimeDb.reference
            .child("signaling")
            .child(callId)
        
        ref.addValueEventListener(listener)
        Log.d(TAG, "Started listening for ICE candidates on callId: $callId")
        
        awaitClose {
            ref.removeEventListener(listener)
            Log.d(TAG, "Stopped listening for ICE candidates")
        }
    }
}
