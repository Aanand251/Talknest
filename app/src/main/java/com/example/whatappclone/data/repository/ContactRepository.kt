package com.example.whatappclone.data.repository

import com.example.whatappclone.data.model.ContactRequest
import com.example.whatappclone.data.model.RequestStatus
import com.example.whatappclone.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ContactRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Search user by email
     */
    suspend fun searchUserByEmail(email: String): Result<User?> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", email.trim().lowercase())
                .limit(1)
                .get()
                .await()
            
            val user = snapshot.documents.firstOrNull()?.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send friend request to registered user
     */
    suspend fun sendFriendRequest(
        currentUser: User,
        receiverEmail: String,
        receiverId: String,
        message: String = "Hi! Let's connect on TalkNest"
    ): Result<String> {
        return try {
            val requestId = UUID.randomUUID().toString()
            val request = ContactRequest(
                requestId = requestId,
                senderId = currentUser.userId,
                senderName = currentUser.name,
                senderEmail = currentUser.email,
                senderProfileImage = currentUser.profileImageUrl,
                receiverEmail = receiverEmail,
                receiverId = receiverId,
                status = RequestStatus.PENDING,
                message = message
            )
            
            firestore.collection("contactRequests")
                .document(requestId)
                .set(request.toMap())
                .await()
            
            Result.success(requestId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate invitation link for unregistered users
     */
    fun generateInvitationLink(senderName: String, senderEmail: String): String {
        val baseUrl = "https://talknest.app/invite" // Replace with actual deep link
        val encodedName = java.net.URLEncoder.encode(senderName, "UTF-8")
        val encodedEmail = java.net.URLEncoder.encode(senderEmail, "UTF-8")
        return "$baseUrl?from=$encodedName&email=$encodedEmail"
    }
    
    /**
     * Share invitation via system share sheet
     */
    fun shareInvitation(link: String): String {
        return """
            🎉 Join me on TalkNest! 🎉
            
            A modern, secure messaging app with end-to-end encryption.
            
            Download now: $link
            
            ✨ Features:
            • End-to-end encrypted chats
            • Voice & Video calls
            • Modern UI
            • Fast & Secure
            
            See you there! 🚀
        """.trimIndent()
    }
    
    /**
     * Observe incoming friend requests
     */
    fun observeIncomingRequests(userId: String): Flow<List<ContactRequest>> = callbackFlow {
        val listener = firestore.collection("contactRequests")
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("status", RequestStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ContactRequest::class.java)
                } ?: emptyList()
                
                trySend(requests)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Observe sent friend requests
     */
    fun observeSentRequests(userId: String): Flow<List<ContactRequest>> = callbackFlow {
        val listener = firestore.collection("contactRequests")
            .whereEqualTo("senderId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ContactRequest::class.java)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                
                trySend(requests)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Accept friend request
     */
    suspend fun acceptFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection("contactRequests")
                .document(requestId)
                .update("status", RequestStatus.ACCEPTED.name)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Reject friend request
     */
    suspend fun rejectFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection("contactRequests")
                .document(requestId)
                .update("status", RequestStatus.REJECTED.name)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancel sent friend request
     */
    suspend fun cancelFriendRequest(requestId: String): Result<Unit> {
        return try {
            firestore.collection("contactRequests")
                .document(requestId)
                .update("status", RequestStatus.CANCELLED.name)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user's contacts (accepted requests)
     */
    suspend fun getContacts(userId: String): Result<List<User>> {
        return try {
            // Get accepted requests where user is sender or receiver
            val sentRequests = firestore.collection("contactRequests")
                .whereEqualTo("senderId", userId)
                .whereEqualTo("status", RequestStatus.ACCEPTED.name)
                .get()
                .await()
            
            val receivedRequests = firestore.collection("contactRequests")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", RequestStatus.ACCEPTED.name)
                .get()
                .await()
            
            // Get contact user IDs
            val contactIds = mutableSetOf<String>()
            sentRequests.documents.forEach { doc ->
                doc.getString("receiverId")?.let { contactIds.add(it) }
            }
            receivedRequests.documents.forEach { doc ->
                doc.getString("senderId")?.let { contactIds.add(it) }
            }
            
            // Fetch user profiles
            val contacts = mutableListOf<User>()
            for (contactId in contactIds) {
                val userDoc = firestore.collection("users")
                    .document(contactId)
                    .get()
                    .await()
                
                userDoc.toObject(User::class.java)?.let { contacts.add(it) }
            }
            
            Result.success(contacts.sortedBy { it.name })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
