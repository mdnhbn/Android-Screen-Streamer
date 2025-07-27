# Complete Setup Guide: Android Screen Sharing Application

This comprehensive guide will walk you through setting up and running the Android screen sharing application from start to finish.

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Environment Setup](#environment-setup)
3. [Signaling Server Setup](#signaling-server-setup)
4. [Android Application Setup](#android-application-setup)
5. [Network Configuration](#network-configuration)
6. [Testing and Verification](#testing-and-verification)
7. [Troubleshooting](#troubleshooting)

## System Requirements

### Development Environment

- **Operating System**: Windows 10/11, macOS 10.14+, or Ubuntu 18.04+
- **Android Studio**: Arctic Fox (2020.3.1) or later
- **Java Development Kit**: JDK 8 or later
- **Node.js**: Version 14.0 or later
- **npm**: Version 6.0 or later

### Android Devices

- **Minimum Android Version**: Android 7.0 (API level 24)
- **Recommended**: Android 8.0+ for better performance
- **RAM**: Minimum 2GB, recommended 4GB+
- **Network**: Wi-Fi capability
- **Permissions**: Ability to grant screen capture permissions

### Network Requirements

- **Wi-Fi Network**: All devices must be on the same local network
- **Bandwidth**: Minimum 10 Mbps for 720p streaming
- **Latency**: Low-latency network preferred (< 50ms)

## Environment Setup

### 1. Install Android Studio

1. Download Android Studio from [developer.android.com](https://developer.android.com/studio)
2. Run the installer and follow the setup wizard
3. Install the Android SDK and necessary build tools
4. Configure an Android Virtual Device (AVD) if needed for testing

### 2. Install Node.js

1. Download Node.js from [nodejs.org](https://nodejs.org/)
2. Install the LTS version for your operating system
3. Verify installation:
   ```bash
   node --version
   npm --version
   ```

### 3. Enable Developer Options on Android Devices

1. Go to **Settings > About phone**
2. Tap **Build number** seven times
3. Return to **Settings** and find **Developer options**
4. Enable **USB debugging**
5. Enable **Wireless debugging** (Android 11+) if desired

## Signaling Server Setup

### 1. Navigate to Server Directory

```bash
cd signaling_server
```

### 2. Install Dependencies

```bash
npm install
```

This will install:
- `socket.io`: WebSocket communication
- `express`: Web server framework
- `cors`: Cross-origin resource sharing
- `nodemon`: Development auto-restart (dev dependency)

### 3. Start the Server

For development (with auto-restart):
```bash
npm run dev
```

For production:
```bash
npm start
```

### 4. Verify Server is Running

Open a web browser and navigate to `http://localhost:3000`. You should see:

```json
{
  "message": "WebRTC Signaling Server is running",
  "connectedClients": 0,
  "activeRooms": 0
}
```

### 5. Find Your Server IP Address

You'll need your computer's IP address for the Android app configuration.

**Windows:**
```cmd
ipconfig
```
Look for "IPv4 Address" under your Wi-Fi adapter.

**macOS/Linux:**
```bash
ifconfig
```
Look for your Wi-Fi interface (usually `wlan0` or `en0`).

**Alternative method:**
```bash
hostname -I
```

Note down this IP address (e.g., `192.168.1.100`).

## Android Application Setup

### 1. Open Project in Android Studio

1. Launch Android Studio
2. Select **Open an existing Android Studio project**
3. Navigate to the `android_app` directory
4. Click **OK** and wait for Gradle sync

### 2. Configure Signaling Server URL

1. Open `app/src/main/java/com/example/screensharingapp/SignalingClient.kt`
2. Find the line:
   ```kotlin
   private const val SERVER_URL = "ws://192.168.1.100:3000"
   ```
3. Replace `192.168.1.100` with your actual server IP address
4. Save the file

### 3. Sync Project

1. Click **Sync Now** if prompted
2. Wait for Gradle sync to complete
3. Resolve any dependency issues if they arise

### 4. Build the Project

1. Go to **Build > Make Project**
2. Ensure there are no compilation errors
3. Fix any issues that arise

## Network Configuration

### 1. Ensure All Devices Are on Same Network

- Connect your computer (running the signaling server) to Wi-Fi
- Connect both Android devices to the same Wi-Fi network
- Verify connectivity by pinging between devices

### 2. Configure Firewall (if necessary)

**Windows:**
1. Open Windows Defender Firewall
2. Allow Node.js through the firewall
3. Ensure port 3000 is not blocked

**macOS:**
1. Go to **System Preferences > Security & Privacy > Firewall**
2. Add Node.js to allowed applications

**Linux:**
```bash
sudo ufw allow 3000
```

### 3. Test Network Connectivity

From an Android device, try accessing the signaling server:
1. Open a web browser on the Android device
2. Navigate to `http://YOUR_SERVER_IP:3000`
3. You should see the server status JSON response

## Testing and Verification

### 1. Install App on Both Devices

**Method 1: USB Connection**
1. Connect first Android device via USB
2. In Android Studio, click **Run > Run 'app'**
3. Select the connected device
4. Repeat for the second device

**Method 2: Wireless Installation**
1. Enable wireless debugging on both devices
2. Pair devices with Android Studio
3. Install app wirelessly

### 2. Test Broadcasting

**On Broadcaster Device:**
1. Launch the app
2. Tap **Start Broadcasting**
3. Grant screen capture permission when prompted
4. Tap **Start Streaming**
5. Verify status shows "Streaming started - Waiting for viewer"

**On Viewer Device:**
1. Launch the app
2. Tap **Join as Viewer**
3. Wait for connection to establish
4. Verify you can see the broadcaster's screen

### 3. Verify Functionality

Test the following scenarios:
- Screen content updates in real-time on viewer
- Connection status updates correctly
- App handles network interruptions gracefully
- Stop streaming works properly
- App permissions are handled correctly

## Troubleshooting

### Common Issues and Solutions

#### 1. "Connection Failed" Error

**Symptoms:**
- App shows "Disconnected from signaling server"
- Devices cannot connect to each other

**Solutions:**
- Verify signaling server is running (`npm start`)
- Check server IP address in `SignalingClient.kt`
- Ensure all devices are on the same Wi-Fi network
- Check firewall settings
- Restart the signaling server

#### 2. "Permission Denied" for Screen Capture

**Symptoms:**
- Screen capture permission dialog doesn't appear
- Permission is denied even when granted

**Solutions:**
- Ensure Android version is 7.0 or later
- Check app permissions in Android settings
- Restart the app and try again
- Clear app data and reinstall if necessary

#### 3. Poor Video Quality or Lag

**Symptoms:**
- Video is pixelated or choppy
- Significant delay between broadcaster and viewer

**Solutions:**
- Check Wi-Fi signal strength
- Reduce other network usage
- Move devices closer to Wi-Fi router
- Consider lowering video resolution in code:
  ```kotlin
  videoCapturer?.startCapture(960, 540, 24) // Lower resolution
  ```

#### 4. App Crashes on Startup

**Symptoms:**
- App immediately closes when launched
- Error messages in Android Studio logcat

**Solutions:**
- Check Android Studio logcat for specific errors
- Verify all dependencies are properly installed
- Clean and rebuild the project
- Check Android version compatibility

#### 5. WebRTC Connection Fails

**Symptoms:**
- Signaling works but video doesn't appear
- ICE connection state shows "failed"

**Solutions:**
- Check STUN server accessibility
- Verify WebRTC permissions are granted
- Restart both apps
- Check for NAT/firewall issues

### Debug Logging

Enable detailed logging to diagnose issues:

1. Open Android Studio
2. Go to **View > Tool Windows > Logcat**
3. Filter by these tags:
   - `WebRTCClient`
   - `SignalingClient`
   - `BroadcasterActivity`
   - `ViewerActivity`

### Performance Monitoring

Monitor performance using:
- Android Studio's CPU Profiler
- Network usage in Android settings
- Battery usage statistics
- Memory usage in Developer options

### Advanced Configuration

#### Custom Video Settings

Modify video capture settings in `BroadcasterActivity.kt`:

```kotlin
// High quality (may impact performance)
videoCapturer?.startCapture(1920, 1080, 30)

// Balanced quality
videoCapturer?.startCapture(1280, 720, 30)

// Low quality (better performance)
videoCapturer?.startCapture(960, 540, 24)
```

#### Network Optimization

For better network performance, consider:
- Using 5GHz Wi-Fi instead of 2.4GHz
- Reducing background app network usage
- Using a dedicated Wi-Fi network for testing

## Additional Resources

### Documentation Links

- [Android MediaProjection API](https://developer.android.com/reference/android/media/projection/MediaProjection)
- [WebRTC Android Documentation](https://webrtc.org/native-code/android/)
- [Socket.IO Documentation](https://socket.io/docs/v4/)

### Sample Code References

- [WebRTC Android Examples](https://github.com/webrtc/samples)
- [Android Screen Capture Examples](https://developer.android.com/guide/topics/media/mediarecorder)

### Community Support

- [WebRTC Google Group](https://groups.google.com/forum/#!forum/discuss-webrtc)
- [Android Developers Community](https://developer.android.com/community)
- [Stack Overflow WebRTC Tag](https://stackoverflow.com/questions/tagged/webrtc)

This setup guide should provide everything needed to successfully deploy and run the Android screen sharing application. If you encounter issues not covered here, check the debug logs and refer to the troubleshooting section for additional guidance.

