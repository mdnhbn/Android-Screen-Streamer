const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const cors = require('cors');

const app = express();
const server = http.createServer(app);

// Enable CORS for all origins
app.use(cors({
    origin: "*",
    methods: ["GET", "POST"],
    credentials: true
}));

// Initialize Socket.IO with CORS configuration
const io = socketIo(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"],
        credentials: true
    },
    transports: ['websocket', 'polling']
});

const PORT = process.env.PORT || 3000;

// Store connected clients and rooms
const rooms = new Map();
const clients = new Map();

// Basic route for health check
app.get('/', (req, res) => {
    res.json({
        message: 'WebRTC Signaling Server is running',
        connectedClients: clients.size,
        activeRooms: rooms.size
    });
});

// Socket.IO connection handling
io.on('connection', (socket) => {
    console.log(`Client connected: ${socket.id}`);
    
    // Store client information
    clients.set(socket.id, {
        id: socket.id,
        room: null,
        role: null,
        connectedAt: new Date()
    });

    // Handle joining a room
    socket.on('join-room', (data) => {
        const { roomId, role } = data; // role: 'broadcaster' or 'viewer'
        
        console.log(`Client ${socket.id} joining room ${roomId} as ${role}`);
        
        // Leave any existing room
        if (clients.get(socket.id).room) {
            socket.leave(clients.get(socket.id).room);
        }
        
        // Join the new room
        socket.join(roomId);
        
        // Update client information
        const clientInfo = clients.get(socket.id);
        clientInfo.room = roomId;
        clientInfo.role = role;
        
        // Initialize room if it doesn't exist
        if (!rooms.has(roomId)) {
            rooms.set(roomId, {
                id: roomId,
                broadcaster: null,
                viewers: [],
                createdAt: new Date()
            });
        }
        
        const room = rooms.get(roomId);
        
        // Add client to room based on role
        if (role === 'broadcaster') {
            if (room.broadcaster) {
                // Disconnect existing broadcaster
                const existingBroadcaster = room.broadcaster;
                io.to(existingBroadcaster).emit('broadcaster-replaced');
                console.log(`Replacing existing broadcaster in room ${roomId}`);
            }
            room.broadcaster = socket.id;
            
            // Notify all viewers that broadcaster has joined
            socket.to(roomId).emit('broadcaster-joined');
            
        } else if (role === 'viewer') {
            room.viewers.push(socket.id);
            
            // Notify broadcaster that a viewer has joined
            if (room.broadcaster) {
                io.to(room.broadcaster).emit('viewer-joined', { viewerId: socket.id });
            }
        }
        
        // Send room status to the client
        socket.emit('room-joined', {
            roomId: roomId,
            role: role,
            broadcasterPresent: !!room.broadcaster,
            viewerCount: room.viewers.length
        });
        
        console.log(`Room ${roomId} status: Broadcaster: ${!!room.broadcaster}, Viewers: ${room.viewers.length}`);
    });

    // Handle WebRTC signaling messages
    socket.on('offer', (data) => {
        console.log(`Offer from ${socket.id}`);
        const clientInfo = clients.get(socket.id);
        if (clientInfo && clientInfo.room) {
            // Broadcast offer to all other clients in the room
            socket.to(clientInfo.room).emit('offer', {
                offer: data.offer,
                from: socket.id
            });
        }
    });

    socket.on('answer', (data) => {
        console.log(`Answer from ${socket.id}`);
        const clientInfo = clients.get(socket.id);
        if (clientInfo && clientInfo.room) {
            // Send answer to the broadcaster or specific client
            if (data.to) {
                io.to(data.to).emit('answer', {
                    answer: data.answer,
                    from: socket.id
                });
            } else {
                socket.to(clientInfo.room).emit('answer', {
                    answer: data.answer,
                    from: socket.id
                });
            }
        }
    });

    socket.on('ice-candidate', (data) => {
        console.log(`ICE candidate from ${socket.id}`);
        const clientInfo = clients.get(socket.id);
        if (clientInfo && clientInfo.room) {
            // Broadcast ICE candidate to all other clients in the room
            socket.to(clientInfo.room).emit('ice-candidate', {
                candidate: data.candidate,
                from: socket.id
            });
        }
    });

    // Handle simple signaling for basic WebSocket clients (like Android OkHttp)
    socket.on('message', (message) => {
        try {
            const data = typeof message === 'string' ? JSON.parse(message) : message;
            console.log(`Message from ${socket.id}:`, data);
            
            const clientInfo = clients.get(socket.id);
            if (clientInfo && clientInfo.room) {
                // Broadcast message to all other clients in the room
                socket.to(clientInfo.room).emit('message', data);
            } else {
                // If no room specified, try to auto-join default room
                const defaultRoom = 'default-room';
                socket.join(defaultRoom);
                
                // Update client info
                if (clientInfo) {
                    clientInfo.room = defaultRoom;
                    clientInfo.role = data.type === 'offer' ? 'broadcaster' : 'viewer';
                }
                
                // Initialize default room if needed
                if (!rooms.has(defaultRoom)) {
                    rooms.set(defaultRoom, {
                        id: defaultRoom,
                        broadcaster: null,
                        viewers: [],
                        createdAt: new Date()
                    });
                }
                
                // Broadcast to default room
                socket.to(defaultRoom).emit('message', data);
            }
        } catch (error) {
            console.error('Error parsing message:', error);
        }
    });

    // Handle disconnection
    socket.on('disconnect', () => {
        console.log(`Client disconnected: ${socket.id}`);
        
        const clientInfo = clients.get(socket.id);
        if (clientInfo && clientInfo.room) {
            const room = rooms.get(clientInfo.room);
            if (room) {
                if (clientInfo.role === 'broadcaster' && room.broadcaster === socket.id) {
                    room.broadcaster = null;
                    // Notify all viewers that broadcaster has left
                    socket.to(clientInfo.room).emit('broadcaster-left');
                    console.log(`Broadcaster left room ${clientInfo.room}`);
                } else if (clientInfo.role === 'viewer') {
                    room.viewers = room.viewers.filter(id => id !== socket.id);
                    // Notify broadcaster that a viewer has left
                    if (room.broadcaster) {
                        io.to(room.broadcaster).emit('viewer-left', { viewerId: socket.id });
                    }
                    console.log(`Viewer left room ${clientInfo.room}`);
                }
                
                // Clean up empty rooms
                if (!room.broadcaster && room.viewers.length === 0) {
                    rooms.delete(clientInfo.room);
                    console.log(`Room ${clientInfo.room} deleted (empty)`);
                }
            }
        }
        
        // Remove client from tracking
        clients.delete(socket.id);
    });

    // Handle errors
    socket.on('error', (error) => {
        console.error(`Socket error for ${socket.id}:`, error);
    });
});

// Start the server
server.listen(PORT, '0.0.0.0', () => {
    console.log(`WebRTC Signaling Server running on port ${PORT}`);
    console.log(`Server accessible at: http://0.0.0.0:${PORT}`);
    console.log('Waiting for WebRTC clients to connect...');
});

// Graceful shutdown
process.on('SIGINT', () => {
    console.log('\nShutting down server...');
    server.close(() => {
        console.log('Server closed');
        process.exit(0);
    });
});

// Error handling
process.on('uncaughtException', (error) => {
    console.error('Uncaught Exception:', error);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('Unhandled Rejection at:', promise, 'reason:', reason);
});

