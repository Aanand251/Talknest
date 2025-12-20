package com.example.whatappclone.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager as AndroidRingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages ringtone and vibration for incoming/outgoing calls
 */
class CallRingtoneManager(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    
    private val _isRinging = MutableStateFlow(false)
    val isRinging: StateFlow<Boolean> = _isRinging
    
    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Play incoming call ringtone
     */
    fun playIncomingRingtone() {
        if (_isRinging.value) return
        
        try {
            // Use default ringtone
            val ringtoneUri = AndroidRingtoneManager.getDefaultUri(AndroidRingtoneManager.TYPE_RINGTONE)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, ringtoneUri)
                
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                
                setAudioAttributes(audioAttributes)
                isLooping = true
                
                setOnPreparedListener {
                    start()
                    _isRinging.value = true
                }
                
                setOnErrorListener { _, _, _ ->
                    _isRinging.value = false
                    true
                }
                
                prepareAsync()
            }
            
            // Start vibration pattern (1 second on, 1 second off)
            startVibration()
            
        } catch (e: Exception) {
            e.printStackTrace()
            _isRinging.value = false
        }
    }
    
    /**
     * Play outgoing call ringing sound (ringback tone)
     */
    fun playOutgoingRingtone() {
        if (_isRinging.value) return
        
        try {
            // Use notification ringtone for outgoing
            val ringtoneUri = AndroidRingtoneManager.getDefaultUri(AndroidRingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, ringtoneUri)
                
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                
                setAudioAttributes(audioAttributes)
                isLooping = true
                
                setOnPreparedListener {
                    start()
                    _isRinging.value = true
                }
                
                prepareAsync()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            _isRinging.value = false
        }
    }
    
    /**
     * Start vibration pattern
     */
    private fun startVibration() {
        try {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Pattern: wait 500ms, vibrate 1000ms, wait 1000ms, repeat
                    val pattern = longArrayOf(500, 1000, 1000)
                    val effect = VibrationEffect.createWaveform(pattern, 0) // 0 = repeat
                    it.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    val pattern = longArrayOf(500, 1000, 1000)
                    it.vibrate(pattern, 0) // 0 = repeat
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Stop ringtone and vibration
     */
    fun stopRingtone() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            
            vibrator?.cancel()
            
            _isRinging.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Play call connected sound (short beep)
     */
    fun playCallConnectedSound() {
        try {
            val notificationUri = AndroidRingtoneManager.getDefaultUri(AndroidRingtoneManager.TYPE_NOTIFICATION)
            val mp = MediaPlayer().apply {
                setDataSource(context, notificationUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener { player ->
                    player.release()
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Play call ended sound (short tone)
     */
    fun playCallEndedSound() {
        try {
            // Use a short notification sound
            val notificationUri = AndroidRingtoneManager.getDefaultUri(AndroidRingtoneManager.TYPE_NOTIFICATION)
            val mp = MediaPlayer().apply {
                setDataSource(context, notificationUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener { player ->
                    player.release()
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        stopRingtone()
        mediaPlayer = null
        vibrator = null
    }
}
