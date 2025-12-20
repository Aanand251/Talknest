package com.example.whatappclone.data.webrtc

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.webrtc.*

/**
 * WebRTC Manager for real peer-to-peer video/audio calling
 */
class WebRTCManager(private val context: Context) {
    
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    
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
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        
        PeerConnectionFactory.initialize(options)
        
        val encoderFactory = DefaultVideoEncoderFactory(
            EglBase.create().eglBaseContext,
            true,
            true
        )
        
        val decoderFactory = DefaultVideoDecoderFactory(EglBase.create().eglBaseContext)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
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
        currentCallId = callId
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                iceCandidate?.let { onIceCandidate(it) }
            }
            
            override fun onAddStream(mediaStream: MediaStream?) {
                mediaStream?.let { onAddStream(it) }
            }
            
            override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>?) {}
            override fun onRemoveStream(mediaStream: MediaStream?) {}
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        
        // Add local tracks
        if (isVideoCall) {
            addVideoTrack()
        }
        addAudioTrack()
        
        return peerConnection
    }
    
    /**
     * Add video track (camera)
     */
    private fun addVideoTrack() {
        val videoSource = peerConnectionFactory?.createVideoSource(false)
        localVideoTrack = peerConnectionFactory?.createVideoTrack("video_track", videoSource)
        
        // Initialize camera
        val camera2Enumerator = Camera2Enumerator(context)
        val deviceNames = camera2Enumerator.deviceNames
        
        // Find front camera
        val frontCamera = deviceNames.find { 
            camera2Enumerator.isFrontFacing(it) 
        } ?: deviceNames.firstOrNull()
        
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
        
        val localStream = peerConnectionFactory?.createLocalMediaStream("local_stream")
        localVideoTrack?.let { localStream?.addTrack(it) }
        localAudioTrack?.let { localStream?.addTrack(it) }
        
        localStream?.let { peerConnection?.addStream(it) }
    }
    
    /**
     * Add audio track (microphone)
     */
    private fun addAudioTrack() {
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }
        
        val audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio_track", audioSource)
    }
    
    /**
     * Create and send offer (caller)
     */
    fun createOffer(
        callId: String,
        onOfferCreated: (String) -> Unit
    ) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            // Send offer to Firebase
                            onOfferCreated(it.description)
                        }
                        override fun onSetFailure(error: String?) {}
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, it)
                }
            }
            
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
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
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        
        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            // Send answer to Firebase
                            onAnswerCreated(it.description)
                        }
                        override fun onSetFailure(error: String?) {}
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, it)
                }
            }
            
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    
    /**
     * Set remote offer (receiver)
     */
    fun setRemoteOffer(offer: String) {
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, offer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sessionDescription)
    }
    
    /**
     * Set remote answer (caller)
     */
    fun setRemoteAnswer(answer: String) {
        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, answer)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
        }, sessionDescription)
    }
    
    /**
     * Add ICE candidate
     */
    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }
    
    /**
     * Toggle microphone
     */
    fun toggleMicrophone(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
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
    
    /**
     * Get local video track for preview
     */
    fun getLocalVideoTrack(): VideoTrack? = localVideoTrack
    
    /**
     * Close connection and release resources
     */
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
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
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
