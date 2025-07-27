# Android Screen Sharing Application

A native Android application built with Kotlin that enables real-time screen streaming between Android devices over a local Wi-Fi network using WebRTC technology.

## Overview

This application allows one Android device to broadcast its screen to another Android device with low latency and high performance. The solution uses Android's native `MediaProjection` API for screen capture, WebRTC for peer-to-peer video streaming, and WebSocket for signaling between devices.

## Features

- **Real-time Screen Streaming**: Low-latency screen sharing using WebRTC
- **Native Android Implementation**: Built with Kotlin and Android Studio
- **Dual Role Support**: Each device can act as either broadcaster or viewer
- **Network Efficient**: Optimized for local Wi-Fi networks
- **Modern UI**: Clean, intuitive interface with Material Design
- **Permission Handling**: Proper screen capture permission management

## Architecture

The application consists of two main components:

1. **Broadcaster Role**: Captures and streams the device screen
2. **Viewer Role**: Receives and displays the remote screen stream

### Technology Stack

- **Language**: Kotlin
- **Screen Capture**: Android MediaProjection API
- **Streaming**: Google WebRTC library
- **Signaling**: WebSocket with OkHttp
- **UI**: Android View Binding with Material Design
- **Async Operations**: Kotlin Coroutines

## Prerequisites

- Android Studio Arctic Fox or later
- Android SDK API level 24 (Android 7.0) or higher
- Two Android devices on the same Wi-Fi network
- Node.js signaling server (included in project)

## Project Structure

```
android_app/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/screensharingapp/
│   │   │   ├── MainActivity.kt
│   │   │   ├── BroadcasterActivity.kt
│   │   │   ├── ViewerActivity.kt
│   │   │   ├── WebRTCClient.kt
│   │   │   └── SignalingClient.kt
│   │   ├── res/layout/
│   │   │   ├── activity_main.xml
│   │   │   ├── activity_broadcaster.xml
│   │   │   └── activity_viewer.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
└── README.md
```

## Setup Instructions

### 1. Clone and Import Project

1. Open Android Studio
2. Select "Open an existing Android Studio project"
3. Navigate to the `android_app` directory and select it
4. Wait for Gradle sync to complete

### 2. Configure Dependencies

The project uses the following key dependencies (already configured in `build.gradle.kts`):

```kotlin
// WebRTC
implementation("org.webrtc:google-webrtc:1.0.32006")

// WebSocket client
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:okhttp-ws:4.12.0")

// Kotlin Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

### 3. Configure Signaling Server URL

Update the server URL in `SignalingClient.kt`:

```kotlin
private const val SERVER_URL = "ws://YOUR_SERVER_IP:3000"
```

Replace `YOUR_SERVER_IP` with the actual IP address of your signaling server.

### 4. Build and Install

1. Connect your Android devices via USB or use wireless debugging
2. Build the project: `Build > Make Project`
3. Install on both devices: `Run > Run 'app'`

## Usage Guide

### Starting a Broadcast Session

1. Launch the app on the broadcaster device
2. Tap "Start Broadcasting"
3. Grant screen capture permission when prompted
4. Tap "Start Streaming"
5. Wait for viewer to connect

### Joining as a Viewer

1. Launch the app on the viewer device
2. Tap "Join as Viewer"
3. The app will automatically connect to the broadcaster
4. The remote screen will appear once connection is established

## Technical Implementation Details

### Screen Capture Implementation

The application uses Android's `MediaProjection` API to capture screen content:

```kotlin
private fun requestScreenCapture() {
    val captureIntent = mediaProjectionManager?.createScreenCaptureIntent()
    startActivityForResult(captureIntent, SCREEN_CAPTURE_REQUEST_CODE)
}
```

### WebRTC Configuration

WebRTC is configured with optimized settings for local network streaming:

```kotlin
val rtcConfig = PeerConnection.RTCConfiguration(ICE_SERVERS).apply {
    tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
    bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
    rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
    continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
}
```

### Signaling Protocol

The application uses a simple JSON-based signaling protocol:

- **Offer**: Broadcaster sends connection offer
- **Answer**: Viewer responds with connection answer
- **ICE Candidates**: Both devices exchange network information

## Permissions

The application requires the following permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

## Performance Optimization

### Video Quality Settings

The application is configured for optimal performance:

- **Resolution**: 1280x720 (720p)
- **Frame Rate**: 30 FPS
- **Codec**: VP8/VP9 (hardware accelerated when available)

### Network Optimization

- Uses STUN servers for NAT traversal
- Optimized for local network communication
- Automatic bitrate adaptation based on network conditions

## Troubleshooting

### Common Issues

**Connection Failed**
- Verify both devices are on the same Wi-Fi network
- Check signaling server is running and accessible
- Ensure firewall is not blocking connections

**Poor Video Quality**
- Check Wi-Fi signal strength
- Reduce background network usage
- Consider lowering video resolution in code

**Permission Denied**
- Ensure screen capture permission is granted
- Check Android version compatibility (API 24+)
- Restart app if permission issues persist

**Audio Not Working**
- Verify microphone permission is granted
- Check device audio settings
- Note: Screen audio capture requires Android 10+

### Debug Logging

Enable detailed logging by checking Android Studio's Logcat with these filters:

- `WebRTCClient`: WebRTC connection status
- `SignalingClient`: WebSocket communication
- `BroadcasterActivity`: Broadcasting events
- `ViewerActivity`: Viewing events

## Security Considerations

- The application is designed for local network use only
- No authentication mechanism is implemented
- Screen content is transmitted unencrypted over local network
- Consider implementing authentication for production use

## Future Enhancements

Potential improvements for the application:

- **Authentication**: Add user authentication and room passwords
- **Multiple Viewers**: Support multiple viewers per broadcaster
- **Recording**: Add ability to record streaming sessions
- **Chat**: Implement text chat between broadcaster and viewers
- **Quality Controls**: Add user-configurable video quality settings
- **Cloud Deployment**: Support for internet-based streaming

## Contributing

When contributing to this project:

1. Follow Kotlin coding conventions
2. Add appropriate comments for complex logic
3. Test on multiple Android versions and devices
4. Update documentation for any new features

## License

This project is provided as-is for educational and development purposes.

