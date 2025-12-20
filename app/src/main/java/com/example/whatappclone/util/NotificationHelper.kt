package com.example.whatappclone.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.example.whatappclone.MainActivity
import com.example.whatappclone.R

class NotificationHelper(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID = "talknest_messages"
        const val CHANNEL_NAME = "Messages"
        private const val REQUEST_CODE = 1001
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "TalkNest message notifications"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                
                // Set default sound
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showMessageNotification(
        senderId: String,
        senderName: String,
        senderImage: String?,
        messageText: String,
        chatId: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chatId", chatId)
            putExtra("senderId", senderId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create sender person
        val sender = Person.Builder()
            .setName(senderName)
            .setKey(senderId)
            .build()
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // Heads-up notification (popup)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Messaging style
            .setStyle(
                NotificationCompat.MessagingStyle(sender)
                    .addMessage(messageText, System.currentTimeMillis(), sender)
            )
            .build()
        
        // Set flags for heads-up display
        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        
        notificationManager.notify(senderId.hashCode(), notification)
    }
    
    fun setNotificationSound(soundUri: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            channel?.let {
                // Delete existing channel
                notificationManager.deleteNotificationChannel(CHANNEL_ID)
                
                // Create new channel with custom sound
                val importance = NotificationManager.IMPORTANCE_HIGH
                val newChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = "TalkNest message notifications"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                    
                    val uri = when (soundUri) {
                        "silent" -> null
                        "default" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        else -> Uri.parse(soundUri)
                    }
                    
                    uri?.let { soundUri ->
                        val audioAttributes = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                        setSound(soundUri, audioAttributes)
                    }
                }
                notificationManager.createNotificationChannel(newChannel)
            }
        }
    }
    
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
    
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
