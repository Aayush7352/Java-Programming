package phase16.projects;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/**
 * ChatApplication.java
 *
 * Chat server and client simulation: Message records, chat room, user
 * management, broadcasting messages using producer-consumer pattern
 * with BlockingQueue.
 */
public class ChatApplication {

    // ═══════════════════════════════════════════════
    // Core Records
    // ═══════════════════════════════════════════════

    enum MessageType { JOIN, LEAVE, TEXT, SYSTEM, DIRECT }

    record Message(String messageId, String senderId, String senderName, String content,
                   MessageType type, String roomId, String targetUserId, Instant timestamp) {
        Message {
            timestamp = timestamp != null ? timestamp : Instant.now();
        }

        static Message text(String senderId, String senderName, String content, String roomId) {
            return new Message(UUID.randomUUID().toString(), senderId, senderName, content,
                MessageType.TEXT, roomId, null, Instant.now());
        }

        static Message direct(String senderId, String senderName, String content, String targetUserId) {
            return new Message(UUID.randomUUID().toString(), senderId, senderName, content,
                MessageType.DIRECT, null, targetUserId, Instant.now());
        }

        static Message system(String content, String roomId) {
            return new Message(UUID.randomUUID().toString(), "SYSTEM", "System", content,
                MessageType.SYSTEM, roomId, null, Instant.now());
        }

        static Message join(String userId, String userName, String roomId) {
            return new Message(UUID.randomUUID().toString(), userId, userName, userName + " joined",
                MessageType.JOIN, roomId, null, Instant.now());
        }

        static Message leave(String userId, String userName, String roomId) {
            return new Message(UUID.randomUUID().toString(), userId, userName, userName + " left",
                MessageType.LEAVE, roomId, null, Instant.now());
        }
    }

    record ChatUser(String userId, String userName, String status) {
        ChatUser withStatus(String newStatus) {
            return new ChatUser(userId, userName, newStatus);
        }
    }

    record ChatRoom(String roomId, String name, String topic, Instant createdAt) {}

    // ═══════════════════════════════════════════════
    // Chat Server
    // ═══════════════════════════════════════════════

    static final class ChatServer {
        private final ConcurrentHashMap<String, ChatUser> users = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, ChatRoom> rooms = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Set<String>> usersInRoom = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Set<String>> roomsForUser = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, BlockingQueue<Message>> userQueues = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<Message>> messageHistory = new ConcurrentHashMap<>();
        private final AtomicInteger userCounter = new AtomicInteger(0);
        private final AtomicInteger roomCounter = new AtomicInteger(0);
        private final int historySize;

        ChatServer(int historySize) {
            this.historySize = historySize;
            // Create default room
            createRoom("General", "General discussion");
        }

        // ─── User Management ───

        public ChatUser registerUser(String userName) {
            String userId = "user-" + userCounter.incrementAndGet();
            var user = new ChatUser(userId, userName, "online");
            users.put(userId, user);
            userQueues.put(userId, new LinkedBlockingQueue<>(100));
            roomsForUser.put(userId, ConcurrentHashMap.newKeySet());
            broadcastSystem(userName + " registered");
            return user;
        }

        public ChatUser registerUser(String userId, String userName) {
            var user = new ChatUser(userId, userName, "online");
            users.put(userId, user);
            userQueues.put(userId, new LinkedBlockingQueue<>(100));
            roomsForUser.put(userId, ConcurrentHashMap.newKeySet());
            return user;
        }

        public Optional<ChatUser> getUser(String userId) {
            return Optional.ofNullable(users.get(userId));
        }

        public void setUserStatus(String userId, String status) {
            users.computeIfPresent(userId, (k, v) -> v.withStatus(status));
        }

        public List<ChatUser> getOnlineUsers() {
            return users.values().stream()
                .filter(u -> "online".equals(u.status()))
                .collect(Collectors.toList());
        }

        // ─── Room Management ───

        public ChatRoom createRoom(String name, String topic) {
            String roomId = "room-" + roomCounter.incrementAndGet();
            var room = new ChatRoom(roomId, name, topic, Instant.now());
            rooms.put(roomId, room);
            usersInRoom.put(roomId, ConcurrentHashMap.newKeySet());
            messageHistory.put(roomId, new CopyOnWriteArrayList<>());
            return room;
        }

        public Optional<ChatRoom> getRoom(String roomId) {
            return Optional.ofNullable(rooms.get(roomId));
        }

        public List<ChatRoom> listRooms() {
            return rooms.values().stream()
                .sorted(Comparator.comparing(ChatRoom::name))
                .collect(Collectors.toList());
        }

        // ─── Join/Leave ───

        public void joinRoom(String userId, String roomId) {
            var user = users.get(userId);
            var room = rooms.get(roomId);
            if (user == null) throw new IllegalArgumentException("User not found");
            if (room == null) throw new IllegalArgumentException("Room not found");

            usersInRoom.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
            roomsForUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(roomId);

            var joinMsg = Message.join(userId, user.userName(), roomId);
            broadcastToRoom(roomId, joinMsg);
            addToHistory(roomId, joinMsg);
        }

        public void leaveRoom(String userId, String roomId) {
            var user = users.get(userId);
            var roomUsers = usersInRoom.get(roomId);
            if (roomUsers != null) roomUsers.remove(userId);
            var userRooms = roomsForUser.get(userId);
            if (userRooms != null) userRooms.remove(roomId);

            if (user != null) {
                var leaveMsg = Message.leave(userId, user.userName(), roomId);
                broadcastToRoom(roomId, leaveMsg);
                addToHistory(roomId, leaveMsg);
            }
        }

        // ─── Messaging ───

        public void sendMessage(String userId, String roomId, String content) {
            var user = users.get(userId);
            if (user == null) throw new IllegalArgumentException("User not found");

            var roomUsers = usersInRoom.get(roomId);
            if (roomUsers == null || !roomUsers.contains(userId)) {
                throw new IllegalStateException("User not in room: " + roomId);
            }

            var msg = Message.text(userId, user.userName(), content, roomId);
            broadcastToRoom(roomId, msg);
            addToHistory(roomId, msg);
        }

        public void sendDirectMessage(String fromUserId, String toUserId, String content) {
            var from = users.get(fromUserId);
            var to = users.get(toUserId);
            if (from == null) throw new IllegalArgumentException("Sender not found");
            if (to == null) throw new IllegalArgumentException("Recipient not found");

            var msg = Message.direct(fromUserId, from.userName(), content, toUserId);
            // Send to both sender and recipient
            var queue = userQueues.get(toUserId);
            if (queue != null) queue.offer(msg);
            var fromQueue = userQueues.get(fromUserId);
            if (fromQueue != null) fromQueue.offer(msg);
        }

        public void broadcastSystem(String content) {
            var msg = Message.system(content, "SYSTEM");
            for (var entry : userQueues.entrySet()) {
                entry.getValue().offer(msg);
            }
        }

        private void broadcastToRoom(String roomId, Message msg) {
            var roomUsers = usersInRoom.get(roomId);
            if (roomUsers == null) return;
            for (var uid : roomUsers) {
                var queue = userQueues.get(uid);
                if (queue != null) queue.offer(msg);
            }
        }

        private void addToHistory(String roomId, Message msg) {
            var history = messageHistory.get(roomId);
            if (history != null) {
                history.add(msg);
                if (history.size() > historySize) {
                    history.remove(0);
                }
            }
        }

        // ─── Client Polling ───

        public List<Message> pollMessages(String userId, long timeoutMillis) throws InterruptedException {
            var queue = userQueues.get(userId);
            if (queue == null) return List.of();

            var msgs = new ArrayList<Message>();
            Message first = queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
            if (first != null) msgs.add(first);
            queue.drainTo(msgs);
            return msgs;
        }

        public List<Message> getMessageHistory(String roomId, int limit) {
            var history = messageHistory.get(roomId);
            if (history == null) return List.of();
            return history.subList(Math.max(0, history.size() - limit), history.size());
        }

        public int getOnlineCount() { return (int) users.values().stream().filter(u -> "online".equals(u.status())).count(); }
        public int getTotalUsers() { return users.size(); }
        public int getTotalRooms() { return rooms.size(); }
    }

    // ═══════════════════════════════════════════════
    // Chat Client Simulator
    // ═══════════════════════════════════════════════

    static final class ChatClient implements AutoCloseable {
        private final String userId;
        private final String userName;
        private final ChatServer server;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final List<Message> receivedMessages = new CopyOnWriteArrayList<>();
        private Thread pollThread;

        ChatClient(ChatServer server, String userId, String userName) {
            this.server = server;
            this.userId = userId;
            this.userName = userName;
        }

        public String getUserId() { return userId; }
        public String getUserName() { return userName; }

        public void startPolling() {
            pollThread = Thread.ofVirtual().start(() -> {
                while (running.get()) {
                    try {
                        var msgs = server.pollMessages(userId, 500);
                        for (var msg : msgs) {
                            receivedMessages.add(msg);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        public void joinRoom(String roomId) { server.joinRoom(userId, roomId); }
        public void leaveRoom(String roomId) { server.leaveRoom(userId, roomId); }

        public void sendMessage(String roomId, String content) {
            server.sendMessage(userId, roomId, content);
        }

        public void sendDirect(String toUserId, String content) {
            server.sendDirectMessage(userId, toUserId, content);
        }

        public List<Message> getReceivedMessages() { return List.copyOf(receivedMessages); }
        public int getMessageCount() { return receivedMessages.size(); }

        @Override
        public void close() {
            running.set(false);
            if (pollThread != null) pollThread.interrupt();
        }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        System.out.println("=== Chat Application ===\n");

        ChatServer server = new ChatServer(100);

        // ─── Create Rooms ───
        System.out.println("--- Rooms ---");
        server.createRoom("java", "Java programming discussion");
        server.createRoom("random", "Random topics");
        System.out.println("  Created rooms: " + server.listRooms().stream().map(ChatRoom::name).toList());

        // ─── Register Users ───
        System.out.println("\n--- User Registration ---");
        var alice = server.registerUser("alice");
        var bob = server.registerUser("bob");
        var charlie = server.registerUser("charlie");
        System.out.println("  Registered: " + alice.userName() + " (" + alice.userId() + ")");
        System.out.println("  Registered: " + bob.userName() + " (" + bob.userId() + ")");
        System.out.println("  Registered: " + charlie.userName() + " (" + charlie.userId() + ")");

        // ─── Create Clients ───
        System.out.println("\n--- Client Connections ---");
        var clientAlice = new ChatClient(server, alice.userId(), "Alice");
        var clientBob = new ChatClient(server, bob.userId(), "Bob");
        var clientCharlie = new ChatClient(server, charlie.userId(), "Charlie");
        clientAlice.startPolling();
        clientBob.startPolling();
        clientCharlie.startPolling();
        Thread.sleep(200);
        System.out.println("  Clients connected and polling");

        // ─── Join Rooms ───
        System.out.println("\n--- Join Rooms ---");
        clientAlice.joinRoom("room-0"); // General
        clientAlice.joinRoom("room-1"); // java
        clientBob.joinRoom("room-0");  // General
        clientBob.joinRoom("room-2");  // random
        clientCharlie.joinRoom("room-1"); // java
        Thread.sleep(200);

        // ─── Send Messages ───
        System.out.println("\n--- Messages ---");
        clientAlice.sendMessage("room-0", "Hello everyone!");
        clientBob.sendMessage("room-0", "Hi Alice!");
        clientAlice.sendMessage("room-1", "Anyone know Java 21 features?");
        clientCharlie.sendMessage("room-1", "Virtual threads are great!");
        Thread.sleep(200);

        // ─── Direct Messages ───
        System.out.println("\n--- Direct Messages ---");
        clientAlice.sendDirect(bob.userId(), "Hey Bob, check your email");
        Thread.sleep(200);

        // ─── Display Messages ───
        System.out.println("\n--- Message Logs ---");
        System.out.println("Alice received " + clientAlice.getMessageCount() + " messages:");
        for (var msg : clientAlice.getReceivedMessages().subList(0,
                Math.min(5, clientAlice.getMessageCount()))) {
            String prefix = switch (msg.type()) {
                case TEXT -> "[" + msg.roomId() + "] " + msg.senderName();
                case DIRECT -> "[DM from " + msg.senderName() + "]";
                case JOIN -> "[JOIN]";
                case LEAVE -> "[LEAVE]";
                case SYSTEM -> "[SYSTEM]";
            };
            System.out.println("  " + prefix + ": " + msg.content());
        }

        // ─── Room History ───
        System.out.println("\n--- General Room History ---");
        for (var msg : server.getMessageHistory("room-0", 10)) {
            System.out.printf("  [%s] %s: %s%n", msg.type(), msg.senderName(), msg.content());
        }

        // ─── Virtual Thread Concurrent Chat ───
        System.out.println("\n--- Concurrent Virtual Thread Chat ---");
        var vtServer = new ChatServer(50);
        vtServer.createRoom("chat", "Virtual thread chat");
        var vtClients = new ChatClient[20];
        var vtMsgCount = new AtomicInteger(0);

        for (int i = 0; i < 20; i++) {
            var user = vtServer.registerUser("vt-user-" + i);
            var client = new ChatClient(vtServer, user.userId(), "VTUser" + i);
            client.startPolling();
            vtClients[i] = client;
        }

        // Join all to room
        for (var c : vtClients) c.joinRoom("room-0");
        Thread.sleep(200);

        // Send messages concurrently
        var sendThreads = new Thread[20];
        for (int i = 0; i < 20; i++) {
            int id = i;
            sendThreads[i] = Thread.ofVirtual().start(() -> {
                for (int j = 0; j < 5; j++) {
                    vtClients[id].sendMessage("room-0", "Message " + j + " from VTUser" + id);
                    vtMsgCount.incrementAndGet();
                }
            });
        }
        for (var t : sendThreads) t.join();
        Thread.sleep(500);

        System.out.println("  Total messages sent: " + vtMsgCount.get());
        System.out.println("  Total messages received by all: " +
            Arrays.stream(vtClients).mapToInt(ChatClient::getMessageCount).sum());

        // ─── Cleanup ───
        for (var c : vtClients) c.close();
        clientAlice.close();
        clientBob.close();
        clientCharlie.close();

        System.out.println("\n=== Chat Application Complete ===");
    }
}
