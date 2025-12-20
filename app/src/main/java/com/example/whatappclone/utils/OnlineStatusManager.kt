package com.example.whatappclone.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object OnlineStatusManager {
    
    private const val TAG = "OnlineStatusManager"
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Set user online when app starts
     */
    fun setUserOnline() {
        val userId = auth.currentUser?.uid ?: return
        
        Log.d(TAG, "Setting user online: $userId")
        
        database.reference
            .child("users")
            .child(userId)
            .child("online")
            .setValue(true)
            .addOnSuccessListener {
                Log.d(TAG, "User status set to online successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to set user online", e)
            }
        
        // Set offline when disconnected
        database.reference
            .child("users")
            .child(userId)
            .child("online")
            .onDisconnect()
            .setValue(false)
            .addOnSuccessListener {
                Log.d(TAG, "onDisconnect listener set successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to set onDisconnect", e)
            }
        
        // Also update last seen
        database.reference
            .child("users")
            .child(userId)
            .child("lastSeen")
            .setValue(System.currentTimeMillis())
    }
    
    /**
     * Set user offline when app closes
     */
    fun setUserOffline() {
        val userId = auth.currentUser?.uid ?: return
        
        Log.d(TAG, "Setting user offline: $userId")
        
        database.reference
            .child("users")
            .child(userId)
            .child("online")
            .setValue(false)
            .addOnSuccessListener {
                Log.d(TAG, "User status set to offline successfully")
            }
        
        database.reference
            .child("users")
            .child(userId)
            .child("lastSeen")
            .setValue(System.currentTimeMillis())
    }
    
    /**
     * Check if user is online
     */
    fun isUserOnline(userId: String, callback: (Boolean) -> Unit) {
        database.reference
            .child("users")
            .child(userId)
            .child("online")
            .get()
            .addOnSuccessListener { snapshot ->
                val isOnline = snapshot.getValue(Boolean::class.java) ?: false
                Log.d(TAG, "User $userId is online: $isOnline")
                callback(isOnline)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check user online status", e)
                callback(false)
            }
    }
}
