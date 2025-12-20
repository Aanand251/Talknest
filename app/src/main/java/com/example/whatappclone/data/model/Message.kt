package com.example.whatappclone.data.model

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "", // Encrypted text
    val mediaUrl: String = "", // Encrypted media URL
    val mediaType: String = "", // image, video, audio, document
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val type: MessageType = MessageType.TEXT,
    val isDeleted: Boolean = false,
    val deletedBy: List<String> = emptyList(),
    val isEncrypted: Boolean = true // Flag to indicate encryption status
) {
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "messageId" to messageId,
            "chatId" to chatId,
            "senderId" to senderId,
            "receiverId" to receiverId,
            "text" to text,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "timestamp" to timestamp,
            "status" to status.name,
            "type" to type.name,
            "isDeleted" to isDeleted,
            "deletedBy" to deletedBy,
            "isEncrypted" to isEncrypted
        )
    }
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    SEEN
}

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT
}
