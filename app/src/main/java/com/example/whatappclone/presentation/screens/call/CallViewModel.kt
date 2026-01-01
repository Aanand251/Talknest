package com.example.whatappclone.presentation.screens.call

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatappclone.data.model.Call
import com.example.whatappclone.data.model.CallStatus
import com.example.whatappclone.data.repository.ImprovedCallRepository
import com.example.whatappclone.data.webrtc.WebRTCManager
import com.example.whatappclone.service.CallService
import com.example.whatappclone.utils.CallRingtoneManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream

class CallViewModel(application: Application) : AndroidViewModel(application) {
    
    private val callRepository = ImprovedCallRepository(application)
    private val webRTCManager = WebRTCManager(application)
    private val ringtoneManager = CallRingtoneManager(application)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private val _callState = MutableStateFlow<Call?>(null)
    val callState: StateFlow<Call?> = _callState
    
    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration
    
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted
    
    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn
    
    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled
    
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    
    private var callStartTime: Long = 0
    private var durationJob: Job? = null
    private var timeoutJob: Job? = null
    
    init {
        webRTCManager.initialize()
    }
    
    /**
     * Load call and start WebRTC connection for caller
     */
    fun loadCallAsCaller(callId: String, isVideoCall: Boolean) {
        viewModelScope.launch {
            try {
                Log.d("CallViewModel", "Loading call as caller: $callId")
                
                // Start timeout immediately for caller
                startTimeoutTimer(callId)
                
                // Listen for call updates
                callRepository.listenForCallUpdates(callId).collect { call ->
                    _callState.value = call
                    Log.d("CallViewModel", "Call update received: ${call?.callStatus}")
                    
                    when (call?.callStatus) {
                        CallStatus.RINGING -> {
                            // Play ringback tone for caller
                            ringtoneManager.playOutgoingRingtone()
                        }
                        
                        CallStatus.ANSWERED -> {
                            Log.d("CallViewModel", "Call answered! Stopping timeout and ringtone")
                            ringtoneManager.stopRingtone()
                            ringtoneManager.playCallConnectedSound()
                            _isConnecting.value = true
                            
                            // Cancel timeout
                            timeoutJob?.cancel()
                            
                            // Initialize WebRTC connection as CALLER
                            initializeWebRTCConnection(callId, isVideoCall, isCaller = true)
                        }
                        
                        CallStatus.CONNECTED -> {
                            _isConnecting.value = false
                            startCallDurationTimer()
                            
                            // Start call service
                            CallService.startOngoingCall(getApplication(), callId)
                        }
                        
                        CallStatus.REJECTED, CallStatus.MISSED, CallStatus.ENDED -> {
                            Log.d("CallViewModel", "Call ended with status: ${call.callStatus}")
                            ringtoneManager.stopRingtone()
                            timeoutJob?.cancel()
                            cleanupCall()
                        }
                        
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("CallViewModel", "Error loading call", e)
                _errorMessage.value = "Failed to load call: ${e.message}"
            }
        }
    }
    
    /**
     * Load call and answer for receiver
     */
    fun loadCallAsReceiver(callId: String, isVideoCall: Boolean) {
        viewModelScope.launch {
            try {
                val call = callRepository.getCall(callId)
                _callState.value = call
                
                // Stop ringtone (was playing from service)
                ringtoneManager.stopRingtone()
                ringtoneManager.playCallConnectedSound()
                
                // Answer the call
                callRepository.answerCall(callId)
                
                _isConnecting.value = true
                
                // Initialize WebRTC connection as RECEIVER
                initializeWebRTCConnection(callId, isVideoCall, isCaller = false)
                
                // Listen for updates
                callRepository.listenForCallUpdates(callId).collect { updatedCall ->
                    _callState.value = updatedCall
                    
                    when (updatedCall?.callStatus) {
                        CallStatus.CONNECTED -> {
                            _isConnecting.value = false
                            startCallDurationTimer()
                            
                            // Start call service
                            CallService.startOngoingCall(getApplication(), callId)
                        }
                        
                        CallStatus.ENDED -> {
                            cleanupCall()
                        }
                        
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("CallViewModel", "Error answering call", e)
                _errorMessage.value = "Failed to answer call: ${e.message}"
            }
        }
    }
    
    /**
     * Initialize WebRTC peer connection
     */
    private fun initializeWebRTCConnection(callId: String, isVideoCall: Boolean, isCaller: Boolean) {
        viewModelScope.launch {
            try {
                Log.d("CallViewModel", "🚀 Initializing WebRTC - callId: $callId, isCaller: $isCaller, isVideo: $isVideoCall")
                
                // Configure audio for call BEFORE creating peer connection
                configureAudioForCall(isVideoCall)
                
                webRTCManager.createPeerConnection(
                    callId = callId,
                    isVideoCall = isVideoCall,
                    onIceCandidate = { candidate ->
                        // Send ICE candidate to other peer via Firebase
                        Log.d("CallViewModel", "🧊 Local ICE candidate generated")
                        sendIceCandidate(callId, candidate)
                    },
                    onAddStream = { mediaStream ->
                        // Handle remote stream (display video/audio)
                        Log.d("CallViewModel", "📺 Remote stream received with ${mediaStream.audioTracks?.size ?: 0} audio, ${mediaStream.videoTracks?.size ?: 0} video tracks")
                        handleRemoteStream(mediaStream)
                    }
                )
                
                // Create and send offer (caller) or answer (receiver)
                if (isCaller) {
                    // Caller creates offer
                    Log.d("CallViewModel", "📤 Caller: Creating SDP offer...")
                    webRTCManager.createOffer(callId) { offer ->
                        viewModelScope.launch {
                            Log.d("CallViewModel", "📤 Caller: Sending SDP offer (${offer.length} chars)")
                            callRepository.sendSignalingData(callId, "offer", offer)
                        }
                    }
                    
                    // Listen for answer
                    listenForAnswer(callId)
                } else {
                    // Receiver listens for offer then creates answer
                    Log.d("CallViewModel", "📥 Receiver: Waiting for SDP offer...")
                    listenForOffer(callId)
                }
                
                // Listen for ICE candidates
                listenForIceCandidates(callId)
                
                // Mark as connected after WebRTC setup
                delay(3000) // Give time for ICE gathering and connection
                callRepository.updateCallStatus(callId, CallStatus.CONNECTED)
                
            } catch (e: Exception) {
                Log.e("CallViewModel", "❌ Error initializing WebRTC", e)
                _errorMessage.value = "Connection failed: ${e.message}"
            }
        }
    }
    
    /**
     * Listen for WebRTC offer
     */
    private fun listenForOffer(callId: String) {
        viewModelScope.launch {
            Log.d("CallViewModel", "📥 Started listening for SDP offer...")
            callRepository.listenForSignalingData(callId, "offer").collect { offer ->
                offer?.let {
                    Log.d("CallViewModel", "📥 Received SDP offer (${it.length} chars), setting remote description...")
                    webRTCManager.setRemoteOffer(it)
                    
                    // Create and send answer
                    Log.d("CallViewModel", "📤 Creating SDP answer...")
                    webRTCManager.createAnswer(callId) { answer ->
                        viewModelScope.launch {
                            Log.d("CallViewModel", "📤 Sending SDP answer (${answer.length} chars)")
                            callRepository.sendSignalingData(callId, "answer", answer)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Listen for WebRTC answer
     */
    private fun listenForAnswer(callId: String) {
        viewModelScope.launch {
            Log.d("CallViewModel", "📥 Started listening for SDP answer...")
            callRepository.listenForSignalingData(callId, "answer").collect { answer ->
                answer?.let {
                    Log.d("CallViewModel", "📥 Received SDP answer (${it.length} chars), setting remote description...")
                    webRTCManager.setRemoteAnswer(it)
                }
            }
        }
    }
    
    /**
     * Send ICE candidate
     */
    private fun sendIceCandidate(callId: String, candidate: IceCandidate) {
        viewModelScope.launch {
            val candidateData = "${candidate.sdp}|${candidate.sdpMLineIndex}|${candidate.sdpMid}"
            Log.d("CallViewModel", "Sending ICE candidate: ${candidate.sdpMid}")
            callRepository.sendSignalingData(callId, "ice_candidate", candidateData)
        }
    }
    
    /**
     * Listen for ICE candidates - using new method that handles multiple candidates
     */
    private fun listenForIceCandidates(callId: String) {
        viewModelScope.launch {
            callRepository.listenForIceCandidates(callId).collect { data ->
                try {
                    val parts = data.split("|")
                    if (parts.size >= 3) {
                        val candidate = IceCandidate(parts[2], parts[1].toInt(), parts[0])
                        Log.d("CallViewModel", "Adding remote ICE candidate: ${parts[2]}")
                        webRTCManager.addIceCandidate(candidate)
                    }
                } catch (e: Exception) {
                    Log.e("CallViewModel", "Error parsing ICE candidate", e)
                }
            }
        }
    }
    
    /**
     * Handle remote media stream
     */
    private fun handleRemoteStream(mediaStream: MediaStream) {
        // This will be handled in the UI layer with SurfaceViewRenderer
        Log.d("CallViewModel", "Remote stream received")
    }
    
    /**
     * Start 30-second timeout timer
     */
    private fun startTimeoutTimer(callId: String) {
        Log.d("CallViewModel", "Starting 30s timeout timer for call: $callId")
        timeoutJob?.cancel() // Cancel any existing timeout
        
        timeoutJob = viewModelScope.launch {
            delay(ImprovedCallRepository.CALL_TIMEOUT_MS)
            
            Log.d("CallViewModel", "Call timeout reached! Ending call: $callId")
            // Call not answered, mark as missed
            callRepository.handleCallTimeout(callId)
            ringtoneManager.stopRingtone()
            _errorMessage.value = "No answer"
            
            // Auto-close after 2 seconds
            delay(2000)
            cleanupCall()
        }
    }
    
    /**
     * Start call duration timer
     */
    private fun startCallDurationTimer() {
        callStartTime = System.currentTimeMillis()
        
        durationJob = viewModelScope.launch {
            while (true) {
                val elapsed = (System.currentTimeMillis() - callStartTime) / 1000
                _callDuration.value = elapsed
                delay(1000)
            }
        }
    }
    
    /**
     * Toggle microphone
     */
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        webRTCManager.toggleMicrophone(!_isMuted.value)
    }
    
    /**
     * Toggle speaker
     */
    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
        audioManager.isSpeakerphoneOn = _isSpeakerOn.value
        Log.d("CallViewModel", "Speaker toggled: ${_isSpeakerOn.value}")
    }
    
    /**
     * Configure audio for call
     */
    private fun configureAudioForCall(isVideoCall: Boolean) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = isVideoCall  // Speaker for video, earpiece for voice
        _isSpeakerOn.value = isVideoCall
        Log.d("CallViewModel", "Audio configured - mode: MODE_IN_COMMUNICATION, speaker: $isVideoCall")
    }
    
    /**
     * Restore audio settings
     */
    private fun restoreAudioSettings() {
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        Log.d("CallViewModel", "Audio settings restored")
    }
    
    /**
     * Toggle video (only for video calls)
     */
    fun toggleVideo() {
        _isVideoEnabled.value = !_isVideoEnabled.value
        webRTCManager.toggleCamera(_isVideoEnabled.value)
    }
    
    /**
     * Switch camera (front/back)
     */
    fun switchCamera() {
        webRTCManager.switchCamera()
    }
    
    /**
     * End call
     */
    fun endCall() {
        viewModelScope.launch {
            _callState.value?.callId?.let { callId ->
                val duration = _callDuration.value
                callRepository.endCall(callId, duration)
            }
            
            ringtoneManager.stopRingtone()
            ringtoneManager.playCallEndedSound()
            cleanupCall()
        }
    }
    
    /**
     * Cleanup call resources
     */
    private fun cleanupCall() {
        durationJob?.cancel()
        timeoutJob?.cancel()
        webRTCManager.close()
        ringtoneManager.release()
        restoreAudioSettings()  // Restore audio when call ends
        Log.d("CallViewModel", "Call cleanup completed")
    }
    
    override fun onCleared() {
        super.onCleared()
        cleanupCall()
        webRTCManager.dispose()
    }
}
