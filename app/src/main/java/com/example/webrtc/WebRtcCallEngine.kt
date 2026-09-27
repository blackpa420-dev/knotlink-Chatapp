package com.example.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioAttributes
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.ConcurrentHashMap

enum class CallQuality(
    val displayName: String,
    val resolution: String,
    val targetWidth: Int,
    val targetHeight: Int,
    val targetBitrateKbps: Int,
    val targetFps: Int,
    val scaleResolutionDownBy: Double
) {
    ULTRA_2K("2K Ultra HD", "2560x1440", 2560, 1440, 2200, 30, 1.0),
    FHD_1080P("1080p Full HD", "1920x1080", 1920, 1080, 1500, 30, 1.333333),
    HD_720P("720p HD", "1280x720", 1280, 720, 850, 30, 2.0),
    SD_480P("480p SD", "640x480", 640, 480, 450, 24, 3.0),
    LOW_360P("360p Low Bandwidth", "480x360", 480, 360, 300, 20, 4.0)
}

enum class NetworkStatus(val label: String, val colorHex: Long) {
    EXCELLENT("Excellent Network (up to 2K)", 0xFF10B981),
    GOOD("Good Network (720p+)", 0xFF3B82F6),
    MODERATE("Fair Network (480p)", 0xFFF59E0B),
    POOR("Poor Network (Adapting)", 0xFFEF4444)
}

data class CallEngineState(
    val isCallActive: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val hasRemoteVideo: Boolean = false,
    val activeCallId: String? = null,
    val callType: String = "AUDIO", // AUDIO or VIDEO
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraOn: Boolean = true,
    val isFrontCamera: Boolean = true,
    val echoCancellationActive: Boolean = true,
    val noiseSuppressionActive: Boolean = true,
    val autoGainControlActive: Boolean = true,
    val currentQuality: CallQuality = CallQuality.HD_720P,
    val networkStatus: NetworkStatus = NetworkStatus.EXCELLENT,
    val currentBitrateKbps: Int = 1200,
    val roundTripTimeMs: Long = 28L,
    val packetLossPercent: Float = 0.0f,
    val localAudioLevel: Float = 0.0f,
    val remoteAudioLevel: Float = 0.0f,
    val callDurationSeconds: Int = 0,
    val connectedAt: Long? = null,
    val peerId: String = "",
    val peerName: String = ""
)

class WebRtcCallEngine private constructor(private val context: Context) {

    private val TAG = "WebRtcEngine"

    private val audioManager: AudioManager? = try {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    } catch (e: Throwable) {
        Log.w(TAG, "AudioManager unavailable: ${e.message}")
        null
    }

    private val connectivityManager: ConnectivityManager? = try {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    } catch (e: Throwable) {
        Log.w(TAG, "ConnectivityManager unavailable: ${e.message}")
        null
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _engineState = MutableStateFlow(CallEngineState())
    val engineState: StateFlow<CallEngineState> = _engineState.asStateFlow()

    // WebRTC EGL Base & Context
    val eglBase: EglBase? by lazy {
        try {
            initWebRtcInternal()
            EglBase.create()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create EglBase: ${e.message}", e)
            null
        }
    }
    val eglBaseContext: EglBase.Context? get() = eglBase?.eglBaseContext

    // WebRTC Factory
    private var isWebRtcInitialized = false
    private val peerConnectionFactory: PeerConnectionFactory? by lazy {
        try {
            initWebRtcInternal()
            val audioDeviceModule = JavaAudioDeviceModule.builder(context)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule()

            val builder = PeerConnectionFactory.builder()
                .setAudioDeviceModule(audioDeviceModule)
            eglBaseContext?.let { ctx ->
                try {
                    builder.setVideoEncoderFactory(DefaultVideoEncoderFactory(ctx, true, true))
                    builder.setVideoDecoderFactory(DefaultVideoDecoderFactory(ctx))
                } catch (e: Throwable) {
                    Log.w(TAG, "Failed to attach hardware video factories: ${e.message}")
                }
            }
            builder.createPeerConnectionFactory()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create PeerConnectionFactory: ${e.message}", e)
            null
        }
    }

    // Media resources
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrackInstance: VideoTrack? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    // Sinks for video rendering
    private val localSinks = mutableSetOf<VideoSink>()
    private val remoteSinks = mutableSetOf<VideoSink>()

    // Track Flows for Jetpack Compose UI
    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    // Active PeerConnection
    private var peerConnection: PeerConnection? = null
    private val processedCandidates = ConcurrentHashMap.newKeySet<String>()
    private val pendingRemoteCandidates = java.util.concurrent.ConcurrentLinkedQueue<IceCandidate>()

    // Jobs
    private var networkMonitorJob: Job? = null
    private var timerJob: Job? = null
    private var signalingJob: Job? = null
    private var realtimeObserverJob: Job? = null
    private var videoCaptureControlJob: Job? = null
    private var lastQualityChangeAt: Long = 0L
    private var videoCapturePausedByScreen = false
    private var audioFocusRequest: AudioFocusRequest? = null

    private fun initWebRtcInternal() {
        if (!isWebRtcInitialized) {
            try {
                val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
                PeerConnectionFactory.initialize(initOptions)
                isWebRtcInitialized = true
                Log.d(TAG, "WebRTC Core Initialized successfully")
            } catch (e: Throwable) {
                Log.e(TAG, "WebRTC init failed: ${e.message}", e)
            }
        }
    }

    /**
     * Start a WebRTC call session as Caller or Receiver.
     */
    /**
     * Warm up WebRTC native/audio resources while the incoming call is still ringing.
     * This keeps the expensive first-time factory initialization off the Answer path.
     */
    fun prewarmForCall() {
        scope.launch(Dispatchers.Default) {
            try {
                eglBaseContext
                peerConnectionFactory
                Log.d(TAG, "WebRTC call resources pre-warmed")
            } catch (e: Throwable) {
                Log.w(TAG, "WebRTC prewarm failed: ${e.message}")
            }
        }
    }

    fun startCall(
        callId: String = "session_${System.currentTimeMillis()}",
        isCaller: Boolean = true,
        callType: String = "VIDEO",
        isVideo: Boolean = callType.equals("VIDEO", ignoreCase = true),
        peerId: String = "",
        peerName: String = ""
    ) {
        Log.d(TAG, "startCall called: callId=$callId, isCaller=$isCaller, isVideo=$isVideo")
        setupAudioSubsystem(isVideo)

        _engineState.value = CallEngineState(
            isCallActive = true,
            isConnecting = true,
            isConnected = false,
            hasRemoteVideo = false,
            activeCallId = callId,
            callType = if (isVideo) "VIDEO" else "AUDIO",
            isCameraOn = isVideo,
            isSpeakerOn = isVideo,
            peerId = peerId,
            peerName = peerName,
            currentQuality = if (isVideo) CallQuality.HD_720P else CallQuality.SD_480P
        )

        scope.launch(Dispatchers.Main) {
            try {
                // 1. Setup local tracks
                setupLocalMedia(isVideo)

                // 2. Create PeerConnection
                setupPeerConnection(callId, isCaller, isVideo)

                // 3. Start signaling and negotiation
                if (isCaller) {
                    createAndSendOffer(callId)
                }

                // 4. Start monitoring loops
                // Subscribe to Realtime before the REST fallback so an answer/offer
                // can be consumed immediately instead of waiting for a polling tick.
                startRealtimeObserver(callId, isCaller)
                startSignalingLoop(callId, isCaller)
                startNetworkAdaptationLoop()
                // The UI/ViewModel timer starts only after local ICE CONNECTED/COMPLETED.
            } catch (e: Throwable) {
                Log.e(TAG, "Error starting WebRTC call: ${e.message}", e)
                // Do not leave the UI in an active-call state when WebRTC setup
                // fails. Clean up partially-created media/PeerConnection resources.
                endCall(notifyRemote = false)
            }
        }
    }

    private fun setupAudioSubsystem(isVideo: Boolean) {
        try {
            val am = audioManager ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(attrs).setAcceptsDelayedFocusGain(false).build()
                am.requestAudioFocus(audioFocusRequest!!)
            }
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            am.isMicrophoneMute = false
            setCommunicationRoute(isVideo)
        } catch (e: Throwable) {
            Log.w(TAG, "setupAudioSubsystem warning: " + e.message, e)
        }
    }

    private fun setCommunicationRoute(isSpeaker: Boolean) {
        val am = audioManager ?: return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val devices = am.getAvailableCommunicationDevices()
                val wantedType = if (isSpeaker) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                val target = devices.firstOrNull { it.type == wantedType }
                    ?: if (!isSpeaker) devices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                    } else null
                if (target != null) am.setCommunicationDevice(target) else if (!isSpeaker) am.clearCommunicationDevice()
            } else {
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = isSpeaker
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Communication route change failed: " + e.message, e)
        }
    }
    private fun setupLocalMedia(isVideo: Boolean) {
        val factory = peerConnectionFactory ?: run {
            Log.e(TAG, "peerConnectionFactory is null; cannot setup local media")
            return
        }
        try {
            // Audio Track
            if (localAudioTrack == null) {
                val audioConstraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation2", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl2", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression2", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googAudioMirroring", "false"))
                    mandatory.add(MediaConstraints.KeyValuePair("echoCancellation", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("noiseSuppression", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("autoGainControl", "true"))
                }
                audioSource = factory.createAudioSource(audioConstraints)
                localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource)
                localAudioTrack?.setEnabled(true)
            }

            // Video Track
            if (isVideo && localVideoTrackInstance == null) {
                ensureCameraStarted(isVideo = true)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "setupLocalMedia error: ${e.message}", e)
        }
    }

    fun ensureCameraStarted(isVideo: Boolean = true) {
        if (!isVideo) return
        val factory = peerConnectionFactory ?: run {
            Log.e(TAG, "peerConnectionFactory is null in ensureCameraStarted")
            return
        }
        try {
            if (localVideoTrackInstance == null || videoCapturer == null) {
                val capturer = createVideoCapturer(_engineState.value.isFrontCamera)
                videoCapturer = capturer
                val eglCtx = eglBaseContext
                if (capturer != null && eglCtx != null) {
                    val helper = SurfaceTextureHelper.create("WebRtcCaptureThread", eglCtx)
                    surfaceTextureHelper = helper
                    val vSource = factory.createVideoSource(capturer.isScreencast)
                    videoSource = vSource
                    capturer.initialize(helper, context, vSource.capturerObserver)
                    
                    try {
                        capturer.startCapture(1280, 720, 30)
                        Log.d(TAG, "Camera started capture at 1280x720 @ 30 FPS")
                    } catch (e: Throwable) {
                        Log.w(TAG, "720p capture unavailable, trying 640x480")
                        try {
                            capturer.startCapture(640, 480, 24)
                        } catch (e2: Throwable) {
                            Log.e(TAG, "All camera startCapture attempts failed", e2)
                        }
                    }

                    val vTrack = factory.createVideoTrack("ARDAMSv0", vSource)
                    vTrack.setEnabled(true)
                    localVideoTrackInstance = vTrack
                    _localVideoTrack.value = vTrack

                    synchronized(localSinks) {
                        localSinks.forEach { vTrack.addSink(it) }
                    }

                    // Attach local video track to active PeerConnection if already created
                    peerConnection?.let { pc ->
                        try {
                            pc.addTrack(vTrack, listOf("ARDAMS"))
                            Log.d(TAG, "Attached local video track to active PeerConnection")
                        } catch (e: Throwable) {
                            Log.w(TAG, "Error adding video track to active PeerConnection: ${e.message}")
                        }
                    }

                    Log.d(TAG, "Local video track created and started capture successfully")
                } else {
                    Log.w(TAG, "Could not initialize physical camera capturer; continuing with audio only")
                }
            } else if (localVideoTrackInstance != null) {
                localVideoTrackInstance?.setEnabled(true)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "ensureCameraStarted exception: ${e.message}", e)
        }
    }

    private fun createVideoCapturer(isFront: Boolean): CameraVideoCapturer? {
        try {
            val enumerator2 = Camera2Enumerator(context)
            for (name in enumerator2.deviceNames) {
                if (isFront && enumerator2.isFrontFacing(name)) {
                    val capturer = enumerator2.createCapturer(name, null)
                    if (capturer != null) return capturer
                } else if (!isFront && enumerator2.isBackFacing(name)) {
                    val capturer = enumerator2.createCapturer(name, null)
                    if (capturer != null) return capturer
                }
            }
            if (enumerator2.deviceNames.isNotEmpty()) {
                val capturer = enumerator2.createCapturer(enumerator2.deviceNames[0], null)
                if (capturer != null) return capturer
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Camera2Enumerator failed, trying Camera1: ${e.message}")
        }

        try {
            val enumerator1 = Camera1Enumerator(true)
            for (name in enumerator1.deviceNames) {
                if (isFront && enumerator1.isFrontFacing(name)) {
                    val capturer = enumerator1.createCapturer(name, null)
                    if (capturer != null) return capturer
                } else if (!isFront && enumerator1.isBackFacing(name)) {
                    val capturer = enumerator1.createCapturer(name, null)
                    if (capturer != null) return capturer
                }
            }
            if (enumerator1.deviceNames.isNotEmpty()) {
                return enumerator1.createCapturer(enumerator1.deviceNames[0], null)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Camera1Enumerator also failed: ${e.message}")
        }
        return null
    }

    private fun setupPeerConnection(callId: String, isCaller: Boolean, isVideo: Boolean) {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:global.stun.twilio.com:3478").createIceServer(),
            // TURN Relay Servers for Symmetric NAT / Cellular Networks (4G/5G) / Firewalls
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "PeerConnection onSignalingChange: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "PeerConnection onIceConnectionChange: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        val connectedAt = _engineState.value.connectedAt ?: System.currentTimeMillis()
                        _engineState.value = _engineState.value.copy(
                            isConnected = true,
                            isConnecting = false,
                            connectedAt = connectedAt
                        )
                        scope.launch {
                            try {
                                com.example.data.supabase.SupabaseService.updateCallSessionStatus(callId, "CONNECTED", connectedAt = connectedAt)
                            } catch (e: Throwable) {
                                Log.w(TAG, "Failed to publish CONNECTED state: ${e.message}")
                            }
                        }
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED -> {
                        _engineState.value = _engineState.value.copy(isConnected = false)
                    }
                    else -> Unit
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "PeerConnection onIceGatheringChange: $state")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null) {
                    scope.launch {
                        try {
                            val candidateJson = JSONObject().apply {
                                put("from", if (isCaller) "caller" else "receiver")
                                put("sdp", candidate.sdp)
                                put("sdpMid", candidate.sdpMid)
                                put("sdpMLineIndex", candidate.sdpMLineIndex)
                            }
                            com.example.data.supabase.SupabaseService.addCallIceCandidate(callId, candidateJson)
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error posting ICE candidate: ${e.message}")
                        }
                    }
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onAddStream(stream: MediaStream?) {
                val vTrack = stream?.videoTracks?.firstOrNull()
                if (vTrack != null) {
                    handleRemoteVideo(vTrack)
                }
            }

            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "PeerConnection onRemoveStream")
            }

            override fun onDataChannel(channel: DataChannel?) {}

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "PeerConnection onRenegotiationNeeded")
            }

            override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
                val track = receiver?.track()
                when (track) {
                    is VideoTrack -> handleRemoteVideo(track)
                    is AudioTrack -> {
                        track.setEnabled(true)
                        Log.d(TAG, "Remote audio track received and enabled")
                    }
                }
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                when (track) {
                    is VideoTrack -> handleRemoteVideo(track)
                    is AudioTrack -> {
                        track.setEnabled(true)
                        Log.d(TAG, "Remote audio transceiver track received and enabled")
                    }
                }
            }
        }

        val pc = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        peerConnection = pc

        // Add local tracks to PeerConnection.
        // Explicitly enable WebRTC audio capture/playout for both audio and video calls.
        val streamIds = listOf("ARDAMS")
        localAudioTrack?.let { it.setEnabled(true); pc?.addTrack(it, streamIds) }
        localVideoTrackInstance?.let { pc?.addTrack(it, streamIds) }
        try {
            pc?.setAudioRecording(true)
            pc?.setAudioPlayout(true)
        } catch (_: Throwable) {
            Log.w(TAG, "Could not explicitly enable WebRTC audio I/O")
        }

        // addTrack() above creates Unified Plan transceivers/senders. Avoid duplicate transceivers; duplicate m-lines can cause blank remote video.
        try {
            configureVideoSenderBitrate(_engineState.value.currentQuality)
        } catch (e: Throwable) {
            Log.w(TAG, "Video sender configuration warning: ${e.message}")
        }
    }

    private fun handleRemoteVideo(vTrack: VideoTrack) {
        scope.launch(Dispatchers.Main) {
            Log.d(TAG, "Remote video track received! Attaching to sinks...")
            _remoteVideoTrack.value = vTrack
            _engineState.value = _engineState.value.copy(
                hasRemoteVideo = true
            )
            synchronized(remoteSinks) {
                remoteSinks.forEach { vTrack.addSink(it) }
            }
        }
    }

    private fun createAndSendOffer(callId: String) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createOffer(object : SdpObserverAdapter("createOffer") {
            override fun onCreateSuccess(desc: SessionDescription?) {
                if (desc != null) {
                    peerConnection?.setLocalDescription(SdpObserverAdapter("setLocalDescription(Offer)"), desc)
                    scope.launch {
                        com.example.data.supabase.SupabaseService.setCallSdpOffer(callId, desc.description)
                        Log.d(TAG, "Caller SDP Offer uploaded to Supabase")
                    }
                }
            }
        }, constraints)
    }

    private fun drainPendingCandidates() {
        val pc = peerConnection ?: return
        Log.d(TAG, "Draining ${pendingRemoteCandidates.size} queued ICE candidates...")
        while (pendingRemoteCandidates.isNotEmpty()) {
            val cand = pendingRemoteCandidates.poll()
            if (cand != null) {
                pc.addIceCandidate(cand)
                Log.d(TAG, "Drained pending ICE candidate: ${cand.sdpMid}")
            }
        }
    }

    private fun handleRemoteOfferAndSendAnswer(callId: String, offerSdp: String) {
        if (offerSdp.isBlank()) return
        val offerDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter("setRemoteDescription(Offer)") {
            override fun onSetSuccess() {
                drainPendingCandidates()
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                }
                peerConnection?.createAnswer(object : SdpObserverAdapter("createAnswer") {
                    override fun onCreateSuccess(answerDesc: SessionDescription?) {
                        if (answerDesc != null) {
                            peerConnection?.setLocalDescription(SdpObserverAdapter("setLocalDescription(Answer)"), answerDesc)
                            scope.launch {
                                com.example.data.supabase.SupabaseService.setCallSdpAnswer(callId, answerDesc.description)
                                Log.d(TAG, "Receiver SDP Answer uploaded to Supabase")
                            }
                        }
                    }
                }, constraints)
            }
        }, offerDesc)
    }

    private fun handleRemoteAnswer(answerSdp: String) {
        if (answerSdp.isBlank()) return
        val answerDesc = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter("setRemoteDescription(Answer)") {
            override fun onSetSuccess() {
                drainPendingCandidates()
                Log.d(TAG, "Caller set Remote Description from Answer successfully")
            }
        }, answerDesc)
    }

    private fun startRealtimeObserver(callId: String, isCaller: Boolean) {
        realtimeObserverJob?.cancel()
        realtimeObserverJob = scope.launch {
            com.example.data.supabase.SupabaseRealtimeManager.callSessionUpdates.collect { session ->
                if (session.id == callId) {
                    processSessionUpdate(session, callId, isCaller)
                }
            }
        }
    }

    private fun startSignalingLoop(callId: String, isCaller: Boolean) {
        signalingJob?.cancel()
        signalingJob = scope.launch {
            while (isActive && _engineState.value.isCallActive) {
                // If the call is already connected, signaling is 100% complete! Stop polling to conserve egress.
                if (_engineState.value.isConnected) {
                    Log.d(TAG, "Call is connected! Halting signaling loop.")
                    break
                }
                try {
                    val sessionRes = com.example.data.supabase.SupabaseService.getCallSession(callId)
                    if (sessionRes.isSuccess) {
                        val session = sessionRes.getOrNull()
                        if (session != null) {
                            if (isCaller && session.sdpOffer.isBlank()) {
                                peerConnection?.localDescription?.description?.let { offerSdp ->
                                    if (offerSdp.isNotBlank()) {
                                        Log.d(TAG, "Signaling loop: re-uploading missing SDP Offer to Supabase...")
                                        com.example.data.supabase.SupabaseService.setCallSdpOffer(callId, offerSdp)
                                    }
                                }
                            }
                            processSessionUpdate(session, callId, isCaller)
                            if (_engineState.value.isConnected) {
                                break
                            }
                        }
                    }
                } catch (e: Throwable) {
                    Log.w(TAG, "Signaling loop error: ${e.message}")
                }
                // REST is only a safety net; keep the fallback short so a missed
                // Realtime event cannot add several seconds to call connection time.
                delay(750)
            }
        }
    }

    private fun processSessionUpdate(session: com.example.data.supabase.SupabaseCallSession, callId: String, isCaller: Boolean) {
        // 1. Check if call was ended or declined
        if (session.status == "ENDED" || session.status == "DECLINED" || session.status == "CANCELLED") {
            Log.d(TAG, "Remote party ended or declined the call")
            endCall(notifyRemote = false)
            return
        }

        // 2. Caller waiting for Answer
        if (isCaller) {
            if (session.sdpAnswer.isNotBlank() && peerConnection?.remoteDescription == null) {
                handleRemoteAnswer(session.sdpAnswer)
            }
        } else {
            // 3. Receiver waiting for Offer
            if (session.sdpOffer.isNotBlank() && peerConnection?.remoteDescription == null) {
                handleRemoteOfferAndSendAnswer(callId, session.sdpOffer)
            }
        }

        // Do not mark local WebRTC CONNECTED merely because the remote DB row
        // says CONNECTED. The authoritative local signal is ICE CONNECTED/COMPLETED.
        // The DB status remains useful for signaling and termination.

        // 4. Ingest new ICE Candidates from opposite party
        val targetRole = if (isCaller) "receiver" else "caller"
        try {
            val array = JSONArray(session.iceCandidates)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val from = obj.optString("from")
                val sdp = obj.optString("sdp")
                val sdpMid = obj.optString("sdpMid")
                val sdpMLineIndex = obj.optInt("sdpMLineIndex", 0)
                val candidateKey = "$sdpMid:$sdpMLineIndex:$sdp"

                if (from == targetRole && sdp.isNotBlank() && processedCandidates.add(candidateKey)) {
                    val candidateObj = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                    if (peerConnection?.remoteDescription != null) {
                        peerConnection?.addIceCandidate(candidateObj)
                        Log.d(TAG, "Added incoming ICE candidate from $from")
                    } else {
                        pendingRemoteCandidates.add(candidateObj)
                        Log.d(TAG, "Queued incoming ICE candidate from $from (waiting for remote description)")
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error parsing candidates JSON: ${e.message}")
        }
    }

    // ==========================================
    // SINK ATTACHMENT FOR COMPOSE UI
    // ==========================================

    fun attachLocalVideoSink(sink: VideoSink) {
        synchronized(localSinks) {
            localSinks.add(sink)
            localVideoTrackInstance?.addSink(sink)
        }
    }

    fun detachLocalVideoSink(sink: VideoSink) {
        synchronized(localSinks) {
            localSinks.remove(sink)
            localVideoTrackInstance?.removeSink(sink)
        }
    }

    fun attachRemoteVideoSink(sink: VideoSink) {
        synchronized(remoteSinks) {
            remoteSinks.add(sink)
            _remoteVideoTrack.value?.addSink(sink)
        }
    }

    fun detachRemoteVideoSink(sink: VideoSink) {
        synchronized(remoteSinks) {
            remoteSinks.remove(sink)
            _remoteVideoTrack.value?.removeSink(sink)
        }
    }

    // ==========================================
    // HARDWARE CONTROLS
    // ==========================================

    fun toggleMute(): Boolean {
        val newMute = !_engineState.value.isMuted
        try {
            localAudioTrack?.setEnabled(!newMute)
            localAudioTrack?.setVolume(if (newMute) 0.0 else 1.0)
            peerConnection?.senders?.forEach { sender ->
                val track = sender.track()
                if (track?.kind() == "audio") {
                    track.setEnabled(!newMute)
                    if (track is AudioTrack) {
                        track.setVolume(if (newMute) 0.0 else 1.0)
                    }
                }
            }
            audioManager?.isMicrophoneMute = newMute
        } catch (e: Throwable) {
            Log.w(TAG, "toggleMute error: " + e.message, e)
        }
        _engineState.value = _engineState.value.copy(isMuted = newMute)
        return newMute
    }

    fun toggleSpeaker(): Boolean {
        val newSpeaker = !_engineState.value.isSpeakerOn
        try { setCommunicationRoute(newSpeaker) }
        catch (e: Throwable) { Log.w(TAG, "toggleSpeaker error: " + e.message, e) }
        _engineState.value = _engineState.value.copy(isSpeakerOn = newSpeaker)
        return newSpeaker
    }
    fun toggleCamera(): Boolean {
        val newCamera = !_engineState.value.isCameraOn
        localVideoTrackInstance?.setEnabled(newCamera)
        _engineState.value = _engineState.value.copy(isCameraOn = newCamera)
        return newCamera
    }

    fun setVideoPausedByScreen(paused: Boolean) {
        if (!_engineState.value.isCallActive || !_engineState.value.isCameraOn) return
        if (paused == videoCapturePausedByScreen) return

        // Camera stop/start can block while the camera session transitions.
        // Keep that work off MainActivity's BroadcastReceiver/main thread.
        videoCaptureControlJob?.cancel()
        videoCaptureControlJob = scope.launch(Dispatchers.Default) {
            try {
                if (!_engineState.value.isCallActive || !_engineState.value.isCameraOn) return@launch
                if (paused) {
                    localVideoTrackInstance?.setEnabled(false)
                    videoCapturer?.stopCapture()
                    videoCapturePausedByScreen = true
                } else {
                    if (!videoCapturePausedByScreen || !_engineState.value.isCallActive) return@launch
                    videoCapturer?.startCapture(1280, 720, 30)
                    localVideoTrackInstance?.setEnabled(true)
                    videoCapturePausedByScreen = false
                }
            } catch (e: Throwable) {
                Log.w(TAG, "setVideoPausedByScreen error: " + e.message)
            }
        }
    }
    fun switchCamera(): Boolean {
        val newFront = !_engineState.value.isFrontCamera
        videoCapturer?.switchCamera(null)
        _engineState.value = _engineState.value.copy(isFrontCamera = newFront)
        return newFront
    }

    fun setQualityManually(quality: CallQuality) {
        _engineState.value = _engineState.value.copy(
            currentQuality = quality,
            currentBitrateKbps = quality.targetBitrateKbps
        )
    }

    fun endCall(notifyRemote: Boolean = false) {
        val callId = _engineState.value.activeCallId
        if (notifyRemote && callId != null) {
            scope.launch {
                try {
                    com.example.data.supabase.SupabaseService.updateCallSessionStatus(
                        callId = callId,
                        status = "ENDED",
                        endedAt = System.currentTimeMillis()
                    )
                } catch (e: Throwable) {
                    Log.e(TAG, "Error setting call ENDED in Supabase: ${e.message}")
                }
            }
        }

        videoCaptureControlJob?.cancel()
        videoCaptureControlJob = null
        signalingJob?.cancel()
        signalingJob = null
        realtimeObserverJob?.cancel()
        realtimeObserverJob = null
        networkMonitorJob?.cancel()
        networkMonitorJob = null
        timerJob?.cancel()
        timerJob = null

        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null
        } catch (e: Throwable) {
            Log.w(TAG, "Error stopping capturer: ${e.message}")
        }

        try {
            surfaceTextureHelper?.dispose()
            surfaceTextureHelper = null
        } catch (e: Throwable) {
            Log.w(TAG, "Error disposing surfaceTextureHelper: ${e.message}")
        }

        try {
            localVideoTrackInstance?.dispose()
            localVideoTrackInstance = null
            videoSource?.dispose()
            videoSource = null
        } catch (e: Throwable) {
            Log.w(TAG, "Error disposing local video: ${e.message}")
        }

        try {
            localAudioTrack?.dispose()
            localAudioTrack = null
            audioSource?.dispose()
            audioSource = null
        } catch (e: Throwable) {
            Log.w(TAG, "Error disposing local audio: ${e.message}")
        }

        try {
            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null
        } catch (e: Throwable) {
            Log.w(TAG, "Error disposing peerConnection: ${e.message}")
        }

        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
        processedCandidates.clear()
        videoCapturePausedByScreen = false

        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = false
            audioManager?.isMicrophoneMute = false
        } catch (e: Throwable) {
            Log.w(TAG, "Error restoring audioManager: ${e.message}")
        }

        @Suppress("DEPRECATION")
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) audioManager?.clearCommunicationDevice()
            else audioManager?.isSpeakerphoneOn = false
            audioManager?.isMicrophoneMute = false
            audioManager?.mode = AudioManager.MODE_NORMAL
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            }
        } catch (e: Throwable) { Log.w(TAG, "releaseAudioSubsystem warning: " + e.message, e) }
        _engineState.value = CallEngineState(isCallActive = false)
        Log.d(TAG, "WebRtcCallEngine call ended and cleaned up")
    }

    private fun configureVideoSenderBitrate(quality: CallQuality) {
        try {
            val senders = peerConnection?.senders ?: return
            for (sender in senders) {
                if (sender.track()?.kind() != "video") continue
                val params = sender.parameters ?: continue
                if (params.encodings.isEmpty()) continue
                for (encoding in params.encodings) {
                    encoding.minBitrateBps = (quality.targetBitrateKbps * 0.30 * 1000).toInt().coerceAtLeast(100_000)
                    encoding.maxBitrateBps = quality.targetBitrateKbps * 1000
                    encoding.maxFramerate = quality.targetFps
                    encoding.scaleResolutionDownBy = quality.scaleResolutionDownBy
                }
                // Prefer preserving motion smoothness; WebRTC can reduce resolution
                // before aggressively reducing frame rate when bandwidth falls.
                params.degradationPreference = RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE
                sender.parameters = params
            }
            peerConnection?.setBitrate(100_000, (quality.targetBitrateKbps * 1000).coerceAtLeast(250_000), (quality.targetBitrateKbps * 1000).coerceAtLeast(300_000))
        } catch (e: Throwable) {
            Log.w(TAG, "configureVideoSenderBitrate warning")
        }
    }
    private fun startNetworkAdaptationLoop() {
        networkMonitorJob?.cancel()
        networkMonitorJob = scope.launch {
            while (isActive && _engineState.value.isCallActive) {
                delay(2500)
                try {
                    val cm = connectivityManager
                    val network = cm?.activeNetwork
                    val caps = if (network != null) cm.getNetworkCapabilities(network) else null
                    val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
                    val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                    // Keep normal calls capped at FHD/720p. The previous 2K Wi-Fi
                    // ceiling created large visible quality swings on mobile devices.
                    val transportCeiling = if (isWifi) CallQuality.FHD_1080P else if (isCellular) CallQuality.HD_720P else CallQuality.HD_720P
                    var rttMs = _engineState.value.roundTripTimeMs
                    var packetLoss = _engineState.value.packetLossPercent

                    peerConnection?.getStats { report ->
                        try {
                            var received = 0L
                            var lost = 0L
                            var measuredRtt: Long? = null
                            for (stat in report.statsMap.values) {
                                when (stat.type) {
                                    "inbound-rtp" -> {
                                        val kind = stat.members["kind"] ?: stat.members["mediaType"]
                                        if (kind == "video") {
                                            received += (stat.members["packetsReceived"] as? Number)?.toLong() ?: 0L
                                            lost += (stat.members["packetsLost"] as? Number)?.toLong() ?: 0L
                                        }
                                    }
                                    "candidate-pair", "remote-inbound-rtp" -> {
                                        (stat.members["currentRoundTripTime"] as? Number)?.let { measuredRtt = (it.toDouble() * 1000.0).toLong() }
                                        (stat.members["roundTripTime"] as? Number)?.let { measuredRtt = (it.toDouble() * 1000.0).toLong() }
                                    }
                                }
                            }
                            val total = received + lost
                            if (total > 0) packetLoss = lost * 100f / total
                            if (measuredRtt != null) rttMs = measuredRtt!!
                            _engineState.value = _engineState.value.copy(roundTripTimeMs = rttMs, packetLossPercent = packetLoss)
                        } catch (_: Throwable) { }
                    }

                    val current = _engineState.value.currentQuality
                    val now = System.currentTimeMillis()
                    val veryBad = packetLoss >= 15f || rttMs >= 500L
                    val bad = packetLoss >= 8f || rttMs >= 280L
                    val target = when {
                        veryBad -> CallQuality.LOW_360P
                        bad -> CallQuality.SD_480P
                        isWifi -> CallQuality.FHD_1080P
                        isCellular -> if (packetLoss >= 4f || rttMs >= 180L) CallQuality.HD_720P else CallQuality.HD_720P
                        else -> CallQuality.HD_720P
                    }
                    val capped = if (target.ordinal > transportCeiling.ordinal) transportCeiling else target
                    // Hysteresis prevents visible quality oscillation: downgrades need
                    // a sustained problem and upgrades need a longer stable window.
                    val canUpgrade = now - lastQualityChangeAt >= 15_000L
                    val canChange = now - lastQualityChangeAt >= 8_000L
                    if (capped.ordinal > current.ordinal && !canUpgrade) {
                        // Keep current quality until the connection is stable.
                    } else if (capped != current && canChange) {
                        configureVideoSenderBitrate(capped)
                        lastQualityChangeAt = now
                        _engineState.value = _engineState.value.copy(
                            currentQuality = capped,
                            currentBitrateKbps = capped.targetBitrateKbps,
                            networkStatus = when {
                                veryBad -> NetworkStatus.POOR
                                bad -> NetworkStatus.MODERATE
                                capped == CallQuality.ULTRA_2K || capped == CallQuality.FHD_1080P -> NetworkStatus.EXCELLENT
                                else -> NetworkStatus.GOOD
                            }
                        )
                    }
                } catch (_: Throwable) { }
            }
        }
    }
    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && _engineState.value.isCallActive) {
                delay(1000)
                _engineState.value = _engineState.value.copy(
                    callDurationSeconds = _engineState.value.callDurationSeconds + 1
                )
            }
        }
    }

    private open class SdpObserverAdapter(private val tag: String) : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {
            Log.d("WebRtcEngine", "$tag onCreateSuccess")
        }
        override fun onSetSuccess() {
            Log.d("WebRtcEngine", "$tag onSetSuccess")
        }
        override fun onCreateFailure(error: String?) {
            Log.e("WebRtcEngine", "$tag onCreateFailure: $error")
        }
        override fun onSetFailure(error: String?) {
            Log.e("WebRtcEngine", "$tag onSetFailure: $error")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: WebRtcCallEngine? = null

        fun getInstance(context: Context): WebRtcCallEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebRtcCallEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
