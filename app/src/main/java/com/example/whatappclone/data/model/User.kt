package com.example.whatappclone.data.model

data class User(
    val userId: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val name: String = "",
    val about: String = "Hey there! I am using TalkNest",
    val profileImageUrl: String = "",
    val lastSeen: Long = System.currentTimeMillis(),
    @get:JvmName("getIsOnline")
    val isOnline: Boolean = false,
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // Privacy Settings
    val lastSeenPrivacy: String = "Everyone", // Everyone, My Contacts, Nobody
    val readReceiptsEnabled: Boolean = true,
    val notificationSound: String = "default", // default, silent, custom_1, custom_2, etc.
    val popupNotificationsEnabled: Boolean = true
) {
    // For Firestore
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "userId" to userId,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "name" to name,
            "about" to about,
            "profileImageUrl" to profileImageUrl,
            "lastSeen" to lastSeen,
            "isOnline" to isOnline,
            "fcmToken" to fcmToken,
            "createdAt" to createdAt,
            "lastSeenPrivacy" to lastSeenPrivacy,
            "readReceiptsEnabled" to readReceiptsEnabled,
            "notificationSound" to notificationSound,
            "popupNotificationsEnabled" to popupNotificationsEnabled
        )
    }
    
    // Generate avatar URL if no profile image
    fun getAvatarUrl(): String {
        return if (profileImageUrl.isNotEmpty()) {
            profileImageUrl
        } else {
            // DiceBear Avatars API - generates animated avatars
            "https://api.dicebear.com/7.x/avataaars/svg?seed=${name.replace(" ", "")}&backgroundColor=b6e3f4,c0aede,d1d4f9"
        }
    }
}
