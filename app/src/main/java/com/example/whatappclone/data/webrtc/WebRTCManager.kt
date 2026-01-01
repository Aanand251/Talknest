package com.example.whatappclone.data.webrtc

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * WebRTC Manager for real peer-to-peer video/audio calling
 */
class WebRTCManager(private val context: Context) {
    
    companion object {
        private const val TAG = "WebRTCManager"
    }
    
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null  // 🎯 Keep reference to audio device module
    
    private val database = FirebaseDatabase.getInstance()
    private var currentCallId: String? = null
    
    // ICE servers (STUN/TURN servers for NAT traversal)
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
    )
    
    /**
     * Initialize WebRTC
     */
    fun initialize() {
        Log.d(TAG, "Initializing WebRTC...")
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .setFieldTrials("")
            .createInitializationOptions()
        
        PeerConnectionFactory.initialize(options)
        Log.d(TAG, "PeerConnectionFactory initialized")
        
        val encoderFactory = DefaultVideoEncoderFactory(
            EglBase.create().eglBaseContext,
            true,
            true
        )
        
        val decoderFactory = DefaultVideoDecoderFactory(EglBase.create().eglBaseContext)
        
        val factoryOptions = PeerConnectionFactory.Options().apply {
            networkIgnoreMask = 0
        }
        
        // 🎯 IMPROVED: Properly configured audio device module for clear voice
        audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                override fun onWebRtcAudioRecordInitError(errorMessage: String?) {
                    Log.e(TAG, "Audio Record Init Error: $errorMessage")
                }
                override fun onWebRtcAudioRecordStartError(
                    errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode?,
                    errorMessage: String?
                ) {
                    Log.e(TAG, "Audio Record Start Error: $errorMessage")
                }
                override fun onWebRtcAudioRecordError(errorMessage: String?) {
                    Log.e(TAG, "Audio Record Error: $errorMessage")
                }
            })
            .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                override fun onWebRtcAudioTrackInitError(errorMessage: String?) {
                    Log.e(TAG, "Audio Track Init Error: $errorMessage")
                }
                override fun onWebRtcAudioTrackStartError(
                    errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode?,
                    errorMessage: String?
                ) {
                    Log.e(TAG, "Audio Track Start Error: $errorMessage")
                }
                override fun onWebRtcAudioTrackError(errorMessage: String?) {
                    Log.e(TAG, "Audio Track Error: $errorMessage")
                }
            })
            .createAudioDeviceModule()
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(factoryOptions)
            .createPeerConnectionFactory()
        
        // 🎯 DON'T release audio device module immediately - it needs to stay alive for the duration of calls!
        Log.d(TAG, "WebRTC initialization complete with audio device module active!")
    }
    
    /**
     * Create peer connection for a call
     */
    fun createPeerConnection(
        callId: String,
        isVideoCall: Boolean,
        onIceCandidate: (IceCandidate) -> Unit,
        onAddStream: (MediaStream) -> Unit
    ): PeerConnection? {
        Log.d(TAG, "Creating peer connection for call: $callId, isVideo: $isVideoCall")
        currentCallId = callId
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                Log.d(TAG, "ICE Candidate generated: ${iceCandidate?.sdp}")
                iceCandidate?.let { onIceCandidate(it) }
            }
            
            override fun onAddStream(mediaStream: MediaStream?) {
                Log.d(TAG, "Remote stream added: ${mediaStream?.id}")
                mediaStream?.let { 
                    Log.d(TAG, "Remote stream has ${it.audioTracks?.size ?: 0} audio tracks and ${it.videoTracks?.size ?: 0} video tracks")
                    
                    // Enable all audio tracks in the remote stream
                    it.audioTracks?.forEach { audioTrack ->
                        audioTrack.setEnabled(true)
                        Log.d(TAG, "Remote audio track enabled: ${audioTrack.id()}, state: ${audioTrack.state()}")
                    }
                    // Enable all video tracks in the remote stream
                    it.videoTracks?.forEach { videoTrack ->
                        videoTrack.setEnabled(true)
                        Log.d(TAG, "Remote video track enabled: ${videoTrack.id()}")
                    }
                    onAddStream(it)
                }
            }
            
            override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {
                Log.d(TAG, "Signaling state changed: $signalingState")
            }
            
            override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE Connection state changed: $iceConnectionState")
            }
            
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE Gathering state changed: $iceGatheringState")
            }
            override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>?) {}
            override fun onRemoveStream(mediaStream: MediaStream?) {
                Log.d(TAG, "Remote stream removed")
            }
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {
                Log.d(TAG, "Renegotiation needed")
            }
            override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                Log.d(TAG, "Track added: ${mediaStreams?.size} streams")
                // Enable the received track
                val track = rtpReceiver?.track()
                track?.setEnabled(true)
                Log.d(TAG, "Received track enabled: ${track?.id()}, kind: ${track?.kind()}")
                
                // Also enable all tracks in the media streams
                mediaStreams?.forEach { stream ->
                    stream.audioTracks?.forEach { audioTrack ->
                        audioTrack.setEnabled(true)
                        Log.d(TAG, "Stream audio track enabled: ${audioTrack.id()}")
                    }
                    onAddStream(stream)
                }
            }
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        Log.d(TAG, "PeerConnection created: ${peerConnection != null}")
        
        // Add local tracks
        if (isVideoCall) {
            Log.d(TAG, "Adding video track...")
            addVideoTrack()
            localVideoTrack?.let {
                peerConnection?.addTrack(it, listOf("local_stream"))
                Log.d(TAG, "Video track added to peer connection")
            }
        }
        Log.d(TAG, "Adding audio track...")
        addAudioTrack()
        localAudioTrack?.let {
            peerConnection?.addTrack(it, listOf("local_stream"))
            Log.d(TAG, "Audio track added to peer connection")
        }
        
        Log.d(TAG, "PeerConnection ready with local tracks")
        return peerConnection
    }
    
    /**
     * Add video track (camera)
     */
    private fun addVideoTrack() {
        Log.d(TAG, "Creating video track...")
        val videoSource = peerConnectionFactory?.createVideoSource(false)
        localVideoTrack = peerConnectionFactory?.createVideoTrack("video_track", videoSource)
        
        // Initialize camera
        val camera2Enumerator = Camera2Enumerator(context)
        val deviceNames = camera2Enumerator.deviceNames
        
        // Find front camera
        val frontCamera = deviceNames.find { 
            camera2Enumerator.isFrontFacing(it) 
        } ?: deviceNames.firstOrNull()
        
        Log.d(TAG, "Using camera: $frontCamera")
        
        frontCamera?.let { cameraName ->
            videoCapturer = camera2Enumerator.createCapturer(cameraName, null) as? CameraVideoCapturer
            
            videoCapturer?.let { capturer ->
                val surfaceTextureHelper = SurfaceTextureHelper.create(
                    "CaptureThread",
                    EglBase.create().eglBaseContext
                )
                capturer.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
                capturer.startCapture(1280, 720, 30)
            }
        }
        
        // Enable the video track
        localVideoTrack?.setEnabled(true)
        
        Log.d(TAG, "Video track created: ${localVideoTrack?.id()}, enabled: ${localVideoTrack?.enabled()}")
    }
    
    /**
     * Add audio track (microphone)
     */
    private fun addAudioTrack() {
        Log.d(TAG, "Creating audio track...")
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }
        
        val audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio_track", audioSource)
        
        // Enable the audio track
        localAudioTrack?.setEnabled(true)
        
        Log.d(TAG, "Audio track created: ${localAudioTrack?.id()}, enabled: ${localAudioTrack?.enabled()}")
    }
    
    /**
     * Create and send offer (caller)
     */
    fun createOffer(
        callId: String,
        onOfferCreated: (String) -> Unit
    ) {
        Log.d(TAG, "📤 Creating SDP offer for call: $callId")
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    Log.d(TAG, "📤 SDP Offer created successfully (${it.description.length} chars)")
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            Log.d(TAG, "📤 Local description set successfully")
                            onOfferCreated(it.description)
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "❌ Failed to set local description: $error")
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, it)
                }
            }
            
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "❌ Failed to create offer: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    
    /**
     * Create and send answer (receiver)
     */
    fun createAnswer(
        callId: String,
        onAnswerCreated: (String) -> Unit
    ) {
        Log.d(TAG, "📤 Creating SDP answer for call: $callId")
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    Log.d(TAG, "📤 SDP Answer created successfully (${it.description.length} chars)")
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            Log.d(TAG, "📤 Local description (answer) set successfully")
                            onAnswerCreated(it.description)
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "❌ Failed to set local description (answer): $error")
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, it)
                }
            }
            
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "❌ Failed to create answer: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    
    /**
     * Set remote offer (receiver)
     */
    fun setRemoteOffer(offer: String) {
        Log.d(TAG, "📥 Setting remote offer (${offer.length} chars)")
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, offer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "✅ Remote offer set successfully")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "❌ Failed to set remote offer: $error")
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sessionDescription)
    }
    
   
    fun setRemoteAnswer(answer: String) {
        Log.d(TAG, "📥 Setting remote answer (${answer.length} chars)")
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, answer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.d(TAG, "✅ Remote answer set successfully - Connection should be established!")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "❌ Failed to set remote answer: $error")
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sessionDescription)
    }
    
    /**
     * Add ICE candidate
     */
    fun addIceCandidate(candidate: IceCandidate) {
        val result = peerConnection?.addIceCandidate(candidate)
        Log.d(TAG, "🧊 Adding remote ICE candidate: ${candidate.sdpMid}, result: $result")
    }
    
    /**
     * Toggle microphone
     */
    fun toggleMicrophone(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
        Log.d(TAG, "🎤 Microphone toggled: $enabled")
    }
    
    /**
     * Toggle camera
     */
    fun toggleCamera(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }
    
    /**
     * Switch camera (front/back)
     */
    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }
    
   
    fun getLocalVideoTrack(): VideoTrack? = localVideoTrack

    fun close() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null
        
        localVideoTrack?.dispose()
        localVideoTrack = null
        
        localAudioTrack?.dispose()
        localAudioTrack = null
        
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        
        currentCallId = null
    }
    
    /**
     * Dispose factory
     */
    fun dispose() {
        close()
        audioDeviceModule?.release()  // 🎯 Release audio device module when disposing
        audioDeviceModule = null
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        Log.d(TAG, "WebRTC disposed and cleaned up")
    }
}

/**
 * Data classes for signaling
 */
data class SignalingData(
    val type: String, // "offer", "answer", "ice_candidate"
    val data: String,
    val callId: String,
    val senderId: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class IceCandidateData(
    val sdp: String,
    val sdpMLineIndex: Int,
    val sdpMid: String
)
