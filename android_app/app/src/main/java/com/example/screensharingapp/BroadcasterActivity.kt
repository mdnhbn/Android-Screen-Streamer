package com.example.screensharingapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.screensharingapp.databinding.ActivityBroadcasterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*

class BroadcasterActivity : AppCompatActivity(), SignalingClient.SignalingListener {
    companion object {
        private const val TAG = "BroadcasterActivity"
        private const val SCREEN_CAPTURE_REQUEST_CODE = 1000
    }

    private lateinit var binding: ActivityBroadcasterBinding
    private lateinit var webRTCClient: WebRTCClient
    private lateinit var signalingClient: SignalingClient
    
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var videoCapturer: VideoCapturer? = null
    private var isStreaming = false

    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(iceCandidate: IceCandidate) {
            Log.d(TAG, "onIceCandidate: ${iceCandidate.sdp}")
            signalingClient.sendIceCandidate(iceCandidate)
        }

        override fun onDataChannel(dataChannel: DataChannel?) {
            Log.d(TAG, "onDataChannel")
        }

        override fun onIceConnectionReceiveTimeout() {
            Log.d(TAG, "onIceConnectionReceiveTimeout")
        }

        override fun onIceConnectionStateChange(newState: PeerConnection.IceConnectionState?) {
            Log.d(TAG, "onIceConnectionStateChange: $newState")
            runOnUiThread {
                when (newState) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        binding.statusText.text = "Connected - Streaming"
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        binding.statusText.text = "Disconnected"
                    }
                    PeerConnection.IceConnectionState.FAILED -> {
                        binding.statusText.text = "Connection failed"
                    }
                    else -> {}
                }
            }
        }

        override fun onIceGatheringStateChange(newState: PeerConnection.IceGatheringState?) {
            Log.d(TAG, "onIceGatheringStateChange: $newState")
        }

        override fun onAddStream(mediaStream: MediaStream?) {
            Log.d(TAG, "onAddStream")
        }

        override fun onSignalingStateChange(newState: PeerConnection.SignalingState?) {
            Log.d(TAG, "onSignalingStateChange: $newState")
        }

        override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>?) {
            Log.d(TAG, "onIceCandidatesRemoved")
        }

        override fun onRemoveStream(mediaStream: MediaStream?) {
            Log.d(TAG, "onRemoveStream")
        }

        override fun onRenegotiationNeeded() {
            Log.d(TAG, "onRenegotiationNeeded")
        }

        override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
            Log.d(TAG, "onAddTrack")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBroadcasterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupClickListeners()
    }

    private fun initializeComponents() {
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        webRTCClient = WebRTCClient(this, peerConnectionObserver)
        signalingClient = SignalingClient(this)
        
        // Connect to signaling server
        signalingClient.connect()
    }

    private fun setupClickListeners() {
        binding.startStreamingButton.setOnClickListener {
            if (!isStreaming) {
                requestScreenCapture()
            }
        }

        binding.stopStreamingButton.setOnClickListener {
            if (isStreaming) {
                stopStreaming()
            }
        }
    }

    private fun requestScreenCapture() {
        val captureIntent = mediaProjectionManager?.createScreenCaptureIntent()
        startActivityForResult(captureIntent, SCREEN_CAPTURE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
                startStreaming()
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startStreaming() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Initialize peer connection
                webRTCClient.initializePeerConnection()

                // Create screen capturer
                videoCapturer = ScreenCapturerAndroid(mediaProjection, object : VideoCapturer.CapturerObserver {
                    override fun onCapturerStarted(success: Boolean) {
                        Log.d(TAG, "Screen capturer started: $success")
                    }

                    override fun onCapturerStopped() {
                        Log.d(TAG, "Screen capturer stopped")
                    }

                    override fun onFrameCaptured(frame: VideoFrame?) {
                        // Frame captured
                    }
                })

                // Create video track from screen capture
                val videoTrack = webRTCClient.createVideoTrackFromScreenCapture(videoCapturer!!)
                val audioTrack = webRTCClient.createAudioTrack()

                // Add tracks to peer connection
                webRTCClient.addStreamToPeerConnection(videoTrack, audioTrack)

                // Start capturing
                videoCapturer?.startCapture(1280, 720, 30)

                // Create and send offer
                webRTCClient.createOffer { sessionDescription ->
                    signalingClient.sendOffer(sessionDescription)
                }

                isStreaming = true
                binding.startStreamingButton.isEnabled = false
                binding.stopStreamingButton.isEnabled = true
                binding.statusText.text = "Streaming started - Waiting for viewer"

            } catch (e: Exception) {
                Log.e(TAG, "Error starting stream: ${e.message}")
                Toast.makeText(this@BroadcasterActivity, "Failed to start streaming", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopStreaming() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null
        
        mediaProjection?.stop()
        mediaProjection = null
        
        webRTCClient.close()
        
        isStreaming = false
        binding.startStreamingButton.isEnabled = true
        binding.stopStreamingButton.isEnabled = false
        binding.statusText.text = "Streaming stopped"
    }

    // SignalingClient.SignalingListener implementation
    override fun onOfferReceived(sessionDescription: SessionDescription) {
        // Broadcaster doesn't receive offers
    }

    override fun onAnswerReceived(sessionDescription: SessionDescription) {
        Log.d(TAG, "Answer received")
        webRTCClient.setRemoteDescription(sessionDescription)
        runOnUiThread {
            binding.statusText.text = "Viewer connected"
        }
    }

    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        Log.d(TAG, "ICE candidate received")
        webRTCClient.addIceCandidate(iceCandidate)
    }

    override fun onConnectionEstablished() {
        Log.d(TAG, "Signaling connection established")
        runOnUiThread {
            binding.statusText.text = "Connected to signaling server"
        }
    }

    override fun onConnectionClosed() {
        Log.d(TAG, "Signaling connection closed")
        runOnUiThread {
            binding.statusText.text = "Disconnected from signaling server"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isStreaming) {
            stopStreaming()
        }
        signalingClient.disconnect()
    }
}

