package com.example.whatappclone.util

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PresenceManager(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    fun updatePresence(userId: String, isOnline: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val presenceData = hashMapOf(
                "online" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
            
            firestore.collection("presence")
                .document(userId)
                .set(presenceData)
                .addOnSuccessListener {
                    // Success
                }
                .addOnFailureListener {
                    // Handle error
                }
        }
    }
    
    fun setUserOnline(userId: String) {
        updatePresence(userId, true)
    }
    
    fun setUserOffline(userId: String) {
        updatePresence(userId, false)
    }
    
    fun observePresence(userId: String, onPresenceChanged: (Boolean, Long) -> Unit) {
        firestore.collection("presence")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                
                snapshot?.let {
                    val online = it.getBoolean("online") ?: false
                    val lastSeen = it.getLong("lastSeen") ?: System.currentTimeMillis()
                    onPresenceChanged(online, lastSeen)
                }
            }
    }
}
