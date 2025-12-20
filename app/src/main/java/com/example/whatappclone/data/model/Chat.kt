package com.example.whatappclone.data.model

data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(), // user IDs
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val lastMessageType: MessageType = MessageType.TEXT,
    val unreadCount: Map<String, Int> = emptyMap(), // userId to count
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupImageUrl: String = "",
    val groupAdmins: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val typingUsers: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "chatId" to chatId,
            "participants" to participants,
            "lastMessage" to lastMessage,
            "lastMessageTime" to lastMessageTime,
            "lastMessageType" to lastMessageType.name,
            "unreadCount" to unreadCount,
            "isGroup" to isGroup,
            "groupName" to groupName,
            "groupImageUrl" to groupImageUrl,
            "groupAdmins" to groupAdmins,
            "createdBy" to createdBy,
            "createdAt" to createdAt,
            "typingUsers" to typingUsers
        )
    }
}
