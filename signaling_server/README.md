# WebRTC Signaling Server

A Node.js signaling server for WebRTC communication between Android devices for screen sharing.

## Features

- WebSocket-based signaling using Socket.IO
- Room-based communication
- Support for broadcaster and viewer roles
- Automatic room management
- CORS enabled for cross-origin requests
- Health check endpoint

## Installation

1. Navigate to the signaling server directory:
```bash
cd signaling_server
```

2. Install dependencies:
```bash
npm install
```

## Running the Server

### Development Mode (with auto-restart)
```bash
npm run dev
```

### Production Mode
```bash
npm start
```

The server will start on port 3000 by default. You can change this by setting the `PORT` environment variable:

```bash
PORT=8080 npm start
```

## Server Endpoints

### Health Check
- **GET** `/` - Returns server status and connection information

## WebSocket Events

### Client to Server Events

- `join-room` - Join a specific room with a role
  ```json
  {
    "roomId": "room123",
    "role": "broadcaster" // or "viewer"
  }
  ```

- `offer` - Send WebRTC offer
  ```json
  {
    "offer": { "type": "offer", "sdp": "..." }
  }
  ```

- `answer` - Send WebRTC answer
  ```json
  {
    "answer": { "type": "answer", "sdp": "..." }
  }
  ```

- `ice-candidate` - Send ICE candidate
  ```json
  {
    "candidate": "candidate:..."
  }
  ```

- `message` - Generic message (for simple WebSocket clients)
  ```json
  {
    "type": "offer|answer|ice-candidate",
    "sdp": "...",
    "candidate": "..."
  }
  ```

### Server to Client Events

- `room-joined` - Confirmation of room join
- `broadcaster-joined` - Broadcaster has joined the room
- `viewer-joined` - A viewer has joined the room
- `broadcaster-left` - Broadcaster has left the room
- `viewer-left` - A viewer has left the room
- `offer` - Received WebRTC offer
- `answer` - Received WebRTC answer
- `ice-candidate` - Received ICE candidate
- `message` - Generic message

## Configuration

The server listens on `0.0.0.0` to allow external connections. Make sure to:

1. Update the `SERVER_URL` in your Android app's `SignalingClient.kt` to point to your server's IP address
2. Ensure the server port (default 3000) is accessible from your Android devices
3. Both Android devices should be on the same network as the server

## Example Usage

1. Start the signaling server
2. Run the Android app on two devices
3. On the first device, choose "Start Broadcasting"
4. On the second device, choose "Join as Viewer"
5. The devices will connect through the signaling server and establish a WebRTC connection

## Troubleshooting

- Ensure both Android devices and the server are on the same network
- Check that the server IP address is correctly configured in the Android app
- Verify that the server port is not blocked by firewall
- Check server logs for connection and error messages

