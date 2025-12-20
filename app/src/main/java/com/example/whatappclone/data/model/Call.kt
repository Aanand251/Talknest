package com.example.whatappclone.data.model

data class Call(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerImage: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverImage: String = "",
    val callType: CallType = CallType.AUDIO,
    val callStatus: CallStatus = CallStatus.RINGING,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0,
    val offer: String = "",
    val answer: String = "",
    val iceCandidates: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "callId" to callId,
            "callerId" to callerId,
            "callerName" to callerName,
            "callerImage" to callerImage,
            "receiverId" to receiverId,
            "receiverName" to receiverName,
            "receiverImage" to receiverImage,
            "callType" to callType.name,
            "callStatus" to callStatus.name,
            "timestamp" to timestamp,
            "duration" to duration,
            "offer" to offer,
            "answer" to answer,
            "iceCandidates" to iceCandidates
        )
    }
    
    companion object {
        fun fromMap(map: Map<String, Any>): Call {
            return Call(
                callId = map["callId"] as? String ?: "",
                callerId = map["callerId"] as? String ?: "",
                callerName = map["callerName"] as? String ?: "",
                callerImage = map["callerImage"] as? String ?: "",
                receiverId = map["receiverId"] as? String ?: "",
                receiverName = map["receiverName"] as? String ?: "",
                receiverImage = map["receiverImage"] as? String ?: "",
                callType = CallType.valueOf(map["callType"] as? String ?: "AUDIO"),
                callStatus = CallStatus.valueOf(map["callStatus"] as? String ?: "RINGING"),
                timestamp = map["timestamp"] as? Long ?: 0L,
                duration = map["duration"] as? Long ?: 0L,
                offer = map["offer"] as? String ?: "",
                answer = map["answer"] as? String ?: "",
                iceCandidates = (map["iceCandidates"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            )
        }
    }
}

enum class CallType {
    AUDIO,
    VIDEO
}

enum class CallStatus {
    RINGING,
    CONNECTING,
    CONNECTED,
    ANSWERED,
    REJECTED,
    MISSED,
    ENDED,
    BUSY,
    NO_ANSWER,
    FAILED
}
