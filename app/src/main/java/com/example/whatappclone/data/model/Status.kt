package com.example.whatappclone.data.model

data class Status(
    val statusId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfileImage: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "", // image, video, text
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val expiryTime: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24 hours
    val viewedBy: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "statusId" to statusId,
            "userId" to userId,
            "userName" to userName,
            "userProfileImage" to userProfileImage,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "caption" to caption,
            "timestamp" to timestamp,
            "expiryTime" to expiryTime,
            "viewedBy" to viewedBy
        )
    }
}
