package com.example.whatappclone.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.whatappclone.MainActivity
import com.example.whatappclone.R
import com.example.whatappclone.data.model.Call
import com.example.whatappclone.data.model.CallType
import com.example.whatappclone.data.repository.ImprovedCallRepository
import com.example.whatappclone.utils.CallRingtoneManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service for handling incoming and active calls
 */
class CallService : Service() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var callRepository: ImprovedCallRepository
    private lateinit var ringtoneManager: CallRingtoneManager
    private var currentCallId: String? = null
    
    // 🎯 Audio routing management
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var previousAudioMode = AudioManager.MODE_NORMAL
    private var previousSpeakerphoneState = false
    
    companion object {
        private const val TAG = "CallService"
        const val ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL"
        const val ACTION_ANSWER_CALL = "ACTION_ANSWER_CALL"
        const val ACTION_REJECT_CALL = "ACTION_REJECT_CALL"
        const val ACTION_END_CALL = "ACTION_END_CALL"
        const val ACTION_ONGOING_CALL = "ACTION_ONGOING_CALL"
        
        const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        const val EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME"
        const val EXTRA_CALL_TYPE = "EXTRA_CALL_TYPE"
        
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "call_channel"
        const val CHANNEL_NAME = "Call Notifications"
        
        fun startIncomingCall(context: Context, call: Call) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_INCOMING_CALL
                putExtra(EXTRA_CALL_ID, call.callId)
                putExtra(EXTRA_CALLER_NAME, call.callerName)
                putExtra(EXTRA_CALL_TYPE, call.callType.name)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun startOngoingCall(context: Context, callId: String) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_ONGOING_CALL
                putExtra(EXTRA_CALL_ID, callId)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        callRepository = ImprovedCallRepository(this)
        ringtoneManager = CallRingtoneManager(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        Log.d(TAG, "CallService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_INCOMING_CALL -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return START_NOT_STICKY
                val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Unknown"
                val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: CallType.AUDIO.name
                
                currentCallId = callId
                showIncomingCallNotification(callId, callerName, callType)
                ringtoneManager.playIncomingRingtone()
            }
            
            ACTION_ANSWER_CALL -> {
                currentCallId?.let { callId ->
                    serviceScope.launch {
                        callRepository.answerCall(callId)
                        ringtoneManager.stopRingtone()
                        ringtoneManager.playCallConnectedSound()
                        
                        // 🎯 Configure audio for call
                        configureAudioForCall()
                        
                        // Switch to ongoing call notification
                        showOngoingCallNotification(callId)
                        
                        // Open app to call screen
                        openCallScreen(callId)
                    }
                }
            }
            
            ACTION_REJECT_CALL -> {
                currentCallId?.let { callId ->
                    serviceScope.launch {
                        callRepository.rejectCall(callId)
                        ringtoneManager.stopRingtone()
                        stopSelf()
                    }
                }
            }
            
            ACTION_END_CALL -> {
                ringtoneManager.stopRingtone()
                restoreAudioSettings()
                stopSelf()
            }
            
            ACTION_ONGOING_CALL -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return START_NOT_STICKY
                currentCallId = callId
                configureAudioForCall()
                showOngoingCallNotification(callId)
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        ringtoneManager.release()
        restoreAudioSettings()
        serviceScope.cancel()
        Log.d(TAG, "CallService destroyed")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming and ongoing calls"
                setSound(null, null) // We handle sound with ringtone manager
                enableVibration(false) // We handle vibration with ringtone manager
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showIncomingCallNotification(callId: String, callerName: String, callType: String) {
        // Answer action
        val answerIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_ANSWER_CALL
        }
        val answerPendingIntent = PendingIntent.getService(
            this,
            0,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Reject action
        val rejectIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_REJECT_CALL
        }
        val rejectPendingIntent = PendingIntent.getService(
            this,
            1,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Full screen intent to open call UI immediately
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("CALL_ID", callId)
            putExtra("IS_INCOMING", true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            2,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val callTypeText = if (callType == CallType.VIDEO.name) "Video" else "Voice"
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming $callTypeText Call")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                android.R.drawable.ic_menu_call,
                "Answer",
                answerPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Reject",
                rejectPendingIntent
            )
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun showOngoingCallNotification(callId: String) {
        val endCallIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_END_CALL
        }
        val endCallPendingIntent = PendingIntent.getService(
            this,
            3,
            endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("CALL_ID", callId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            4,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ongoing Call")
            .setContentText("Tap to return to call")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                endCallPendingIntent
            )
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    // 🎯 Configure audio routing for call
    private fun configureAudioForCall() {
        try {
            // Save previous audio state
            previousAudioMode = audioManager.mode
            previousSpeakerphoneState = audioManager.isSpeakerphoneOn
            
            // Request audio focus
            requestAudioFocus()
            
            // Set audio mode for voice communication
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            
            // Enable speakerphone for video calls by default (can be toggled by user)
            // For audio calls, use earpiece
            audioManager.isSpeakerphoneOn = false  // Start with earpiece
            
            Log.d(TAG, "Audio configured for call - Mode: ${audioManager.mode}, Speaker: ${audioManager.isSpeakerphoneOn}")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring audio", e)
        }
    }
    
    // 🎯 Request audio focus for call
    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d(TAG, "Audio focus changed: $focusChange")
                        when (focusChange) {
                            AudioManager.AUDIOFOCUS_LOSS -> {
                                // End call if we lose audio focus permanently
                                Log.w(TAG, "Lost audio focus - ending call")
                            }
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                                // Temporarily lost focus
                                Log.w(TAG, "Temporarily lost audio focus")
                            }
                        }
                    }
                    .build()
                
                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                Log.d(TAG, "Audio focus request result: ${if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "GRANTED" else "DENIED"}")
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    { focusChange ->
                        Log.d(TAG, "Audio focus changed: $focusChange")
                    },
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
                Log.d(TAG, "Audio focus request result: ${if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "GRANTED" else "DENIED"}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus", e)
        }
    }
    
    // 🎯 Restore audio settings after call
    private fun restoreAudioSettings() {
        try {
            // Release audio focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                    audioFocusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            
            // Restore previous audio settings
            audioManager.mode = previousAudioMode
            audioManager.isSpeakerphoneOn = previousSpeakerphoneState
            
            Log.d(TAG, "Audio settings restored")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring audio settings", e)
        }
    }
    
    private fun openCallScreen(callId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("CALL_ID", callId)
            putExtra("IS_INCOMING", true)
        }
        startActivity(intent)
    }
}
