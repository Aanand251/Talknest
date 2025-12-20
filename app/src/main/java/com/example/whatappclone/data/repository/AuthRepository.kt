package com.example.whatappclone.data.repository

import com.example.whatappclone.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AuthRepository {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    
    // Email Authentication
    suspend fun signInWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("User ID is null"))
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signUpWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("User ID is null"))
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Phone Authentication (keep for future use)
    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<String> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val userId = result.user?.uid ?: return Result.failure(Exception("User ID is null"))
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            // Get FCM token
            val token = messaging.token.await()
            val userWithToken = user.copy(fcmToken = token)
            
            firestore.collection("users")
                .document(user.userId)
                .set(userWithToken.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserProfile(userId: String): Result<User?> {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            val user = doc.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean) {
        try {
            val updates = hashMapOf<String, Any>(
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun observeUserProfile(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val currentUserId = getCurrentUserId()
            val snapshot = firestore.collection("users")
                .get()
                .await()
            
            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }.filter { it.userId != currentUserId } // Exclude current user
            
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
}
