package com.example.whatappclone.data.model

data class ContactRequest(
    val requestId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val senderProfileImage: String = "",
    val receiverEmail: String = "",
    val receiverId: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String = ""
) {
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "requestId" to requestId,
            "senderId" to senderId,
            "senderName" to senderName,
            "senderEmail" to senderEmail,
            "senderProfileImage" to senderProfileImage,
            "receiverEmail" to receiverEmail,
            "receiverId" to receiverId,
            "status" to status.name,
            "timestamp" to timestamp,
            "message" to message
        )
    }
}

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED
}
