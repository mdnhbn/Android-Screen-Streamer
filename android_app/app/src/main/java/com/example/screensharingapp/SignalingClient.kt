package com.example.screensharingapp

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.ws.WebSocket
import okhttp3.ws.WebSocketListener
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit

class SignalingClient(private val listener: SignalingListener) {
    companion object {
        private const val TAG = "SignalingClient"
        private const val SERVER_URL = "ws://192.168.1.100:3000" // Replace with your server IP
    }

    interface SignalingListener {
        fun onOfferReceived(sessionDescription: SessionDescription)
        fun onAnswerReceived(sessionDescription: SessionDescription)
        fun onIceCandidateReceived(iceCandidate: IceCandidate)
        fun onConnectionEstablished()
        fun onConnectionClosed()
    }

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connection opened")
            listener.onConnectionEstablished()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: $text")
            try {
                val json = JSONObject(text)
                val type = json.getString("type")

                when (type) {
                    "offer" -> {
                        val sdp = json.getString("sdp")
                        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)
                        listener.onOfferReceived(sessionDescription)
                    }
                    "answer" -> {
                        val sdp = json.getString("sdp")
                        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                        listener.onAnswerReceived(sessionDescription)
                    }
                    "ice-candidate" -> {
                        val candidate = json.getString("candidate")
                        val sdpMid = json.getString("sdpMid")
                        val sdpMLineIndex = json.getInt("sdpMLineIndex")
                        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
                        listener.onIceCandidateReceived(iceCandidate)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing message: ${e.message}")
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket connection closed: $reason")
            listener.onConnectionClosed()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket connection failed: ${t.message}")
            listener.onConnectionClosed()
        }
    }

    fun connect() {
        val request = Request.Builder()
            .url(SERVER_URL)
            .build()
        webSocket = client.newWebSocket(request, webSocketListener)
    }

    fun sendOffer(sessionDescription: SessionDescription) {
        val json = JSONObject().apply {
            put("type", "offer")
            put("sdp", sessionDescription.description)
        }
        sendMessage(json.toString())
    }

    fun sendAnswer(sessionDescription: SessionDescription) {
        val json = JSONObject().apply {
            put("type", "answer")
            put("sdp", sessionDescription.description)
        }
        sendMessage(json.toString())
    }

    fun sendIceCandidate(iceCandidate: IceCandidate) {
        val json = JSONObject().apply {
            put("type", "ice-candidate")
            put("candidate", iceCandidate.sdp)
            put("sdpMid", iceCandidate.sdpMid)
            put("sdpMLineIndex", iceCandidate.sdpMLineIndex)
        }
        sendMessage(json.toString())
    }

    private fun sendMessage(message: String) {
        Log.d(TAG, "Sending message: $message")
        webSocket?.send(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "Closing connection")
        webSocket = null
    }
}

