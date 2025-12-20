package com.example.whatappclone.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.whatappclone.MainActivity
import com.example.whatappclone.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Check if message notifications are enabled
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        val messageNotificationsEnabled = prefs.getBoolean("message_notifications_enabled", true)
        
        if (!notificationsEnabled || !messageNotificationsEnabled) return
        
        remoteMessage.notification?.let {
            sendNotification(
                it.title ?: "New Message",
                it.body ?: "",
                remoteMessage.data
            )
        }
        
        remoteMessage.data.takeIf { it.isNotEmpty() }?.let { data ->
            val title = data["title"] ?: "New Message"
            val body = data["body"] ?: ""
            val type = data["type"] ?: "message"
            
            sendNotification(title, body, data, type)
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server if needed
        sendTokenToServer(token)
    }
    
    private fun sendNotification(title: String, messageBody: String, data: Map<String, String>, type: String = "message") {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data["chatId"]?.let { putExtra("chatId", it) }
            data["userId"]?.let { putExtra("userId", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val channelId = if (type == "call") "call_channel" else "message_channel"
        val channelName = if (type == "call") "Calls" else "Messages"
        
        // Get sound preference
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val soundUri = if (type == "call") {
            val callSound = prefs.getString("call_notification_sound", "default")
            if (callSound == "default") RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            else android.net.Uri.parse(callSound)
        } else {
            val messageSound = prefs.getString("message_notification_sound", "default")
            if (messageSound == "default") RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            else android.net.Uri.parse(messageSound)
        }
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (type == "call") "Call notifications" else "Message notifications"
                setSound(soundUri, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
    
    private fun sendTokenToServer(token: String) {
        // Store token in Firestore for sending notifications
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }
}
