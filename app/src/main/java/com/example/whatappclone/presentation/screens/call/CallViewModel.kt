package com.example.whatappclone.presentation.screens.call

import android.app.Application
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
                // Listen for call updates
                callRepository.listenForCallUpdates(callId).collect { call ->
                    _callState.value = call
                    
                    when (call?.callStatus) {
                        CallStatus.RINGING -> {
                            // Play ringback tone for caller
                            ringtoneManager.playOutgoingRingtone()
                            
                            // Start timeout timer (30 seconds)
                            startTimeoutTimer(callId)
                        }
                        
                        CallStatus.ANSWERED -> {
                            ringtoneManager.stopRingtone()
                            ringtoneManager.playCallConnectedSound()
                            _isConnecting.value = true
                            
                            // Cancel timeout
                            timeoutJob?.cancel()
                            
                            // Initialize WebRTC connection
                            initializeWebRTCConnection(callId, isVideoCall)
                        }
                        
                        CallStatus.CONNECTED -> {
                            _isConnecting.value = false
                            startCallDurationTimer()
                            
                            // Start call service
                            CallService.startOngoingCall(getApplication(), callId)
                        }
                        
                        CallStatus.REJECTED, CallStatus.MISSED, CallStatus.ENDED -> {
                            ringtoneManager.stopRingtone()
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
                
                // Initialize WebRTC connection
                initializeWebRTCConnection(callId, isVideoCall)
                
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
    private fun initializeWebRTCConnection(callId: String, isVideoCall: Boolean) {
        viewModelScope.launch {
            try {
                webRTCManager.createPeerConnection(
                    callId = callId,
                    isVideoCall = isVideoCall,
                    onIceCandidate = { candidate ->
                        // Send ICE candidate to other peer via Firebase
                        sendIceCandidate(callId, candidate)
                    },
                    onAddStream = { mediaStream ->
                        // Handle remote stream (display video/audio)
                        handleRemoteStream(mediaStream)
                    }
                )
                
                // Create and send offer (caller) or answer (receiver)
                val call = _callState.value
                if (call?.callStatus == CallStatus.RINGING) {
                    // Caller creates offer
                    webRTCManager.createOffer(callId) { offer ->
                        viewModelScope.launch {
                            callRepository.sendSignalingData(callId, "offer", offer)
                        }
                    }
                    
                    // Listen for answer
                    listenForAnswer(callId)
                } else if (call?.callStatus == CallStatus.ANSWERED) {
                    // Receiver listens for offer then creates answer
                    listenForOffer(callId)
                }
                
                // Listen for ICE candidates
                listenForIceCandidates(callId)
                
                // Mark as connected after WebRTC setup
                delay(2000) // Give time for ICE gathering
                callRepository.updateCallStatus(callId, CallStatus.CONNECTED)
                
            } catch (e: Exception) {
                Log.e("CallViewModel", "Error initializing WebRTC", e)
                _errorMessage.value = "Connection failed: ${e.message}"
            }
        }
    }
    
    /**
     * Listen for WebRTC offer
     */
    private fun listenForOffer(callId: String) {
        viewModelScope.launch {
            callRepository.listenForSignalingData(callId, "offer").collect { offer ->
                offer?.let {
                    webRTCManager.setRemoteOffer(it)
                    
                    // Create and send answer
                    webRTCManager.createAnswer(callId) { answer ->
                        viewModelScope.launch {
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
            callRepository.listenForSignalingData(callId, "answer").collect { answer ->
                answer?.let {
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
            callRepository.sendSignalingData(callId, "ice_candidate", candidateData)
        }
    }
    
    /**
     * Listen for ICE candidates
     */
    private fun listenForIceCandidates(callId: String) {
        viewModelScope.launch {
            callRepository.listenForSignalingData(callId, "ice_candidate").collect { data ->
                data?.let {
                    val parts = it.split("|")
                    if (parts.size == 3) {
                        val candidate = IceCandidate(parts[2], parts[1].toInt(), parts[0])
                        webRTCManager.addIceCandidate(candidate)
                    }
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
        timeoutJob = viewModelScope.launch {
            delay(ImprovedCallRepository.CALL_TIMEOUT_MS)
            
            // Call not answered, mark as missed
            callRepository.handleCallTimeout(callId)
            ringtoneManager.stopRingtone()
            _errorMessage.value = "No answer"
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
        // TODO: Implement audio routing to speaker/earpiece
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
    }
    
    override fun onCleared() {
        super.onCleared()
        cleanupCall()
        webRTCManager.dispose()
    }
}
