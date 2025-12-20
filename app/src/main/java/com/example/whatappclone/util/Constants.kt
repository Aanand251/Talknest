package com.example.whatappclone.util

object Constants {
    
    // SharedPreferences
    const val PREFS_NAME = "whatsapp_clone_prefs"
    const val KEY_USER_ID = "user_id"
    const val KEY_PHONE_NUMBER = "phone_number"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_USER_NAME = "user_name"
    const val KEY_PROFILE_IMAGE = "profile_image"
    
    // Firebase Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_CHATS = "chats"
    const val COLLECTION_MESSAGES = "messages"
    const val COLLECTION_STATUS = "status"
    const val COLLECTION_GROUPS = "groups"
    
    // Message Types
    const val MESSAGE_TYPE_TEXT = "TEXT"
    const val MESSAGE_TYPE_IMAGE = "IMAGE"
    const val MESSAGE_TYPE_VIDEO = "VIDEO"
    const val MESSAGE_TYPE_AUDIO = "AUDIO"
    const val MESSAGE_TYPE_DOCUMENT = "DOCUMENT"
    
    // Request Codes
    const val REQUEST_IMAGE_PICK = 1001
    const val REQUEST_VIDEO_PICK = 1002
    const val REQUEST_DOCUMENT_PICK = 1003
    const val REQUEST_CAMERA = 1004
    
    // Notification
    const val NOTIFICATION_CHANNEL_ID = "talknest_channel"
    const val NOTIFICATION_CHANNEL_NAME = "TalkNest Messages"
    
    // Max lengths
    const val MAX_STATUS_DURATION = 24 * 60 * 60 * 1000L // 24 hours
    const val MAX_ABOUT_LENGTH = 139
    const val MAX_NAME_LENGTH = 25
    const val MAX_GROUP_NAME_LENGTH = 50
    
    // Media
    const val MAX_IMAGE_SIZE = 5 * 1024 * 1024 // 5 MB
    const val MAX_VIDEO_SIZE = 16 * 1024 * 1024 // 16 MB
    const val MAX_AUDIO_DURATION = 15 * 60 * 1000L // 15 minutes
}
