package phase16.projects;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

final class ChatApplication {

    public static record User(String userId, String username, String displayName, Instant createdAt) {
        public User {
            Objects.requireNonNull(userId);
            Objects.requireNonNull(username);
            Objects.requireNonNull(displayName);
            createdAt = createdAt != null ? createdAt : Instant.now();
        }

        public User(String userId, String username, String displayName) {
            this(userId, username, displayName, Instant.now());
        }
    }

    public static enum MessageType { CHAT, SYSTEM, JOIN, LEAVE, DM }

    public static record Message(String messageId, MessageType type, String senderId,
                                  String senderName, String content, Instant timestamp,
                                  String roomId) {
        public Message {
            Objects.requireNonNull(messageId);
            Objects.requireNonNull(type);
            Objects.requireNonNull(content);
            Objects.requireNonNull(timestamp);
        }

        public String formattedTimestamp() {
            var ldt = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
            return ldt.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }

        public String formatted() {
            return "[%s] <%s> %s".formatted(formattedTimestamp(), senderName, content);
        }
    }

    public static final class ChatRoom {
        private final String roomId;
        private final String name;
        private final Set<String> members = ConcurrentHashMap.newKeySet();
        private final List<Message> messages = Collections.synchronizedList(new ArrayList<>());
        private final AtomicLong messageCounter = new AtomicLong(0);

        public ChatRoom(String roomId, String name) {
            this.roomId = Objects.requireNonNull(roomId);
            this.name = Objects.requireNonNull(name);
        }

        public void join(User user) {
            members.add(user.userId());
            addSystemMessage("%s joined the room".formatted(user.displayName()));
        }

        public void leave(User user) {
            members.remove(user.userId());
            addSystemMessage("%s left the room".formatted(user.displayName()));
        }

        public boolean isMember(String userId) {
            return members.contains(userId);
        }

        public Message sendMessage(User sender, String content) {
            if (!members.contains(sender.userId())) {
                throw new IllegalStateException("User not in room: " + sender.userId());
            }
            var msg = new Message(
                    "MSG-" + roomId + "-" + messageCounter.incrementAndGet(),
                    MessageType.CHAT, sender.userId(), sender.displayName(),
                    content, Instant.now(), roomId);
            messages.add(msg);
            return msg;
        }

        public Message sendDirectMessage(User sender, User recipient, String content) {
            if (!members.contains(sender.userId()) || !members.contains(recipient.userId())) {
                throw new IllegalStateException("Both users must be in the room for DM");
            }
            var msg = new Message(
                    "DM-" + roomId + "-" + messageCounter.incrementAndGet(),
                    MessageType.DM, sender.userId(), sender.displayName(),
                    "@%s: %s".formatted(recipient.username(), content), Instant.now(), roomId);
            messages.add(msg);
            return msg;
        }

        private void addSystemMessage(String content) {
            messages.add(new Message(
                    "SYS-" + roomId + "-" + messageCounter.incrementAndGet(),
                    MessageType.SYSTEM, "system", "System", content, Instant.now(), roomId));
        }

        public List<Message> getRecentMessages(int count) {
            synchronized (messages) {
                var size = messages.size();
                var start = Math.max(0, size - count);
                return List.copyOf(messages.subList(start, size));
            }
        }

        public List<Message> getMessagesSince(String messageId) {
            synchronized (messages) {
                var found = false;
                var result = new ArrayList<Message>();
                for (var msg : messages) {
                    if (found) result.add(msg);
                    else if (msg.messageId().equals(messageId)) found = true;
                }
                return List.copyOf(result);
            }
        }

        public List<User> getMembers(Map<String, User> userRegistry) {
            return members.stream()
                    .map(userRegistry::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableList());
        }

        public String getRoomId() { return roomId; }
        public String getName() { return name; }
        public int memberCount() { return members.size(); }
        public int messageCount() { return messages.size(); }
    }

    public static final class ChatServer {
        private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();
        private final Map<String, User> users = new ConcurrentHashMap<>();
        private final AtomicInteger userCounter = new AtomicInteger(0);
        private final AtomicInteger roomCounter = new AtomicInteger(0);
        private final BlockingQueue<Runnable> messageQueue = new LinkedBlockingQueue<>();
        private volatile boolean running = false;
        private final ExecutorService workers;

        public ChatServer(int workerCount) {
            this.workers = Executors.newVirtualThreadPerTaskExecutor();
        }

        public User registerUser(String username, String displayName) {
            var userId = "U-" + userCounter.incrementAndGet();
            if (users.values().stream().anyMatch(u -> u.username().equals(username))) {
                throw new IllegalArgumentException("Username already taken: " + username);
            }
            var user = new User(userId, username, displayName);
            users.put(userId, user);
            return user;
        }

        public ChatRoom createRoom(String name) {
            var roomId = "R-" + roomCounter.incrementAndGet();
            var room = new ChatRoom(roomId, name);
            rooms.put(roomId, room);
            return room;
        }

        public ChatRoom getRoom(String roomId) {
            return rooms.get(roomId);
        }

        public Optional<ChatRoom> findRoomByName(String name) {
            return rooms.values().stream()
                    .filter(r -> r.getName().equals(name))
                    .findFirst();
        }

        public void start() {
            running = true;
            for (int i = 0; i < 3; i++) {
                workers.submit(this::processMessageQueue);
            }
        }

        public void stop() {
            running = false;
        }

        public void enqueueMessage(Runnable task) {
            try {
                messageQueue.offer(task, 1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void processMessageQueue() {
            while (running) {
                try {
                    var task = messageQueue.poll(500, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        task.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public void simulateChat(User user, String roomId, String... messages) {
            var room = rooms.get(roomId);
            if (room == null) throw new IllegalArgumentException("Room not found");

            for (var content : messages) {
                if (!running) break;
                enqueueMessage(() -> {
                    var msg = room.sendMessage(user, content);
                    System.out.println("  " + msg.formatted());
                });
                try { Thread.sleep(200); } catch (InterruptedException e) { break; }
            }
        }

        public List<ChatRoom> listRooms() { return List.copyOf(rooms.values()); }
        public List<User> listUsers() { return List.copyOf(users.values()); }
        public User getUser(String userId) { return users.get(userId); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Chat Application ===%n".formatted());

        var server = new ChatServer(5);
        server.start();

        System.out.println("--- User Registration ---");
        var alice = server.registerUser("alice92", "Alice Johnson");
        var bob = server.registerUser("bob_smith", "Bob Smith");
        var carol = server.registerUser("carol_w", "Carol Williams");
        var dave = server.registerUser("dave_dev", "Dave Developer");

        server.listUsers().forEach(u ->
            System.out.println("  %s (%s) - joined %s".formatted(u.displayName(), u.username(), u.createdAt())));

        System.out.println("%n--- Creating Rooms ---%n".formatted());
        var general = server.createRoom("general");
        var javaRoom = server.createRoom("java-discussion");
        var random = server.createRoom("random");

        System.out.println("  Rooms: " + server.listRooms().stream().map(ChatRoom::getName).collect(Collectors.joining(", ")));

        System.out.println("%n--- Joining Rooms ---%n".formatted());
        general.join(alice);
        general.join(bob);
        general.join(carol);
        javaRoom.join(alice);
        javaRoom.join(dave);
        random.join(bob);
        random.join(carol);
        random.join(dave);

        System.out.println("  Members in 'general': " +
                general.getMembers(server.listUsers().stream().collect(Collectors.toMap(User::userId, u -> u)))
                        .stream().map(User::displayName).collect(Collectors.joining(", ")));

        System.out.println("%n--- Sending Messages (Producer-Consumer via BlockingQueue) ---%n".formatted());
        var latch = new CountDownLatch(3);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                server.simulateChat(alice, general.getRoomId(),
                        "Hello everyone!",
                        "Has anyone seen the new Java 21 features?",
                        "Virtual threads are amazing!");
                latch.countDown();
            });

            executor.submit(() -> {
                server.simulateChat(bob, general.getRoomId(),
                        "Hey Alice! Yes, virtual threads are great.",
                        "Pattern matching for switch is also nice.");
                latch.countDown();
            });

            executor.submit(() -> {
                server.simulateChat(carol, general.getRoomId(),
                        "Hi all! I love the new record patterns.",
                        "And sealed classes are super useful for domain modeling.");
                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        Thread.sleep(1000);

        System.out.println("%n--- Message History (last 5) ---%n".formatted());
        var recent = general.getRecentMessages(5);
        for (var msg : recent) {
            var typeTag = switch (msg.type()) {
                case SYSTEM -> "[SYSTEM]";
                case DM -> "[DM]";
                case JOIN, LEAVE -> "[EVENT]";
                case CHAT -> "";
            };
            System.out.println("  %s %s".formatted(typeTag != null ? typeTag : "", msg.formatted()));
        }

        System.out.println("%n--- Direct Message ---%n".formatted());
        var dm = general.sendDirectMessage(alice, bob, "Hey Bob, check out the DM feature!");
        System.out.println("  " + dm.formatted());

        System.out.println("%n--- Leave Room ---%n".formatted());
        general.leave(carol);
        System.out.println("  Carol left. Members now: " +
                general.getMembers(server.listUsers().stream().collect(Collectors.toMap(User::userId, u -> u)))
                        .stream().map(User::displayName).collect(Collectors.joining(", ")));

        System.out.println("%n--- Pattern Matching on Messages ---%n".formatted());
        for (var msg : general.getRecentMessages(10)) {
            switch (msg) {
                case Message m when m.type() == MessageType.SYSTEM ->
                    System.out.println("  SYSTEM: " + m.content());
                case Message m when m.type() == MessageType.DM ->
                    System.out.println("  DM from %s: %s".formatted(m.senderName(), m.content()));
                case Message m when m.content().contains("Java") || m.content().contains("virtual") ->
                    System.out.println("  TECH: <%s> %s".formatted(m.senderName(), m.content()));
                case Message m ->
                    System.out.println("  CHAT: <%s> %s".formatted(m.senderName(), m.content()));
            }
        }

        System.out.println("%n--- Virtual Threads: Concurrent Room Activity ---%n".formatted());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 5; i++) {
                final int idx = i;
                executor.submit(() -> {
                    var user = server.registerUser("vt_user_" + idx, "VT User " + idx);
                    var room = server.createRoom("vt-room-" + idx);
                    room.join(user);
                    room.sendMessage(user, "Hello from virtual thread " + idx);
                    System.out.println("  [VT-%d] Room '%s' created, message sent".formatted(idx, room.getName()));
                });
            }
        }

        Thread.sleep(500);
        System.out.println("%nFinal Stats: %d users, %d rooms, %d total messages (in general)"
                .formatted(server.listUsers().size(), server.listRooms().size(), general.messageCount()));

        server.stop();
        System.out.println("=== Done ===");
    }
}
