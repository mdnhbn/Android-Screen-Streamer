package com.example.screensharingapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.screensharingapp.databinding.ActivityViewerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*

class ViewerActivity : AppCompatActivity(), SignalingClient.SignalingListener {
    companion object {
        private const val TAG = "ViewerActivity"
    }

    private lateinit var binding: ActivityViewerBinding
    private lateinit var webRTCClient: WebRTCClient
    private lateinit var signalingClient: SignalingClient
    
    private var isConnected = false

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
                        binding.statusText.text = "Connected - Receiving stream"
                        isConnected = true
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        binding.statusText.text = "Disconnected"
                        isConnected = false
                    }
                    PeerConnection.IceConnectionState.FAILED -> {
                        binding.statusText.text = "Connection failed"
                        isConnected = false
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
            mediaStream?.videoTracks?.firstOrNull()?.let { videoTrack ->
                runOnUiThread {
                    videoTrack.addSink(binding.remoteVideoView)
                    binding.statusText.text = "Receiving video stream"
                }
            }
        }

        override fun onSignalingStateChange(newState: PeerConnection.SignalingState?) {
            Log.d(TAG, "onSignalingStateChange: $newState")
        }

        override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>?) {
            Log.d(TAG, "onIceCandidatesRemoved")
        }

        override fun onRemoveStream(mediaStream: MediaStream?) {
            Log.d(TAG, "onRemoveStream")
            runOnUiThread {
                binding.statusText.text = "Stream ended"
            }
        }

        override fun onRenegotiationNeeded() {
            Log.d(TAG, "onRenegotiationNeeded")
        }

        override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
            Log.d(TAG, "onAddTrack")
            val track = rtpReceiver?.track()
            if (track is VideoTrack) {
                runOnUiThread {
                    track.addSink(binding.remoteVideoView)
                    binding.statusText.text = "Receiving video stream"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupVideoView()
    }

    private fun initializeComponents() {
        webRTCClient = WebRTCClient(this, peerConnectionObserver)
        signalingClient = SignalingClient(this)
        
        // Initialize peer connection
        webRTCClient.initializePeerConnection()
        
        // Connect to signaling server
        signalingClient.connect()
    }

    private fun setupVideoView() {
        binding.remoteVideoView.init(EglBase.create().eglBaseContext, null)
        binding.remoteVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        binding.remoteVideoView.setEnableHardwareScaler(true)
    }

    // SignalingClient.SignalingListener implementation
    override fun onOfferReceived(sessionDescription: SessionDescription) {
        Log.d(TAG, "Offer received")
        CoroutineScope(Dispatchers.Main).launch {
            // Set remote description
            webRTCClient.setRemoteDescription(sessionDescription)
            
            // Create and send answer
            webRTCClient.createAnswer { answerDescription ->
                signalingClient.sendAnswer(answerDescription)
            }
            
            runOnUiThread {
                binding.statusText.text = "Offer received - Sending answer"
            }
        }
    }

    override fun onAnswerReceived(sessionDescription: SessionDescription) {
        // Viewer doesn't receive answers
    }

    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        Log.d(TAG, "ICE candidate received")
        webRTCClient.addIceCandidate(iceCandidate)
    }

    override fun onConnectionEstablished() {
        Log.d(TAG, "Signaling connection established")
        runOnUiThread {
            binding.statusText.text = "Connected to signaling server - Waiting for broadcaster"
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
        binding.remoteVideoView.release()
        webRTCClient.close()
        signalingClient.disconnect()
    }
}

