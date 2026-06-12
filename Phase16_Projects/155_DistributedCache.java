package phase16.projects;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

final class DistributedCache {

    public static record CacheEntry<V>(String key, V value, Instant createdAt,
                                        Instant expiresAt, long version) {
        public CacheEntry {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            Objects.requireNonNull(createdAt);
        }

        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }

        public CacheEntry<V> withVersion(long newVersion) {
            return new CacheEntry<>(key, value, createdAt, expiresAt, newVersion);
        }
    }

    public static record CacheNode(String nodeId, String host, int port,
                                    long capacityBytes, long usedBytes) {
        public CacheNode {
            Objects.requireNonNull(nodeId);
            Objects.requireNonNull(host);
            if (capacityBytes <= 0) throw new IllegalArgumentException("Capacity must be positive");
        }

        public boolean hasCapacity(long bytes) {
            return usedBytes + bytes <= capacityBytes;
        }

        public CacheNode withUsedBytes(long newUsed) {
            return new CacheNode(nodeId, host, port, capacityBytes, newUsed);
        }

        public double usageRatio() {
            return (double) usedBytes / capacityBytes;
        }
    }

    public static enum NodeStatus { ONLINE, OFFLINE, RECOVERING, DEGRADED }

    public static final class ConsistentHashRing {
        private final int virtualNodes;
        private final TreeMap<Long, String> ring = new TreeMap<>();
        private final Map<String, CacheNode> nodes = new ConcurrentHashMap<>();
        private final Lock lock = new ReentrantLock();

        public ConsistentHashRing(int virtualNodes) {
            this.virtualNodes = virtualNodes > 0 ? virtualNodes : 100;
        }

        public void addNode(CacheNode node) {
            lock.lock();
            try {
                nodes.put(node.nodeId(), node);
                for (int i = 0; i < virtualNodes; i++) {
                    var hash = hash(node.nodeId() + "-vn-" + i);
                    ring.put(hash, node.nodeId());
                }
            } finally {
                lock.unlock();
            }
        }

        public void removeNode(String nodeId) {
            lock.lock();
            try {
                nodes.remove(nodeId);
                ring.entrySet().removeIf(e -> e.getValue().equals(nodeId));
            } finally {
                lock.unlock();
            }
        }

        public Optional<CacheNode> getNode(String key) {
            lock.lock();
            try {
                if (ring.isEmpty()) return Optional.empty();
                var hash = hash(key);
                var entry = ring.ceilingEntry(hash);
                if (entry == null) entry = ring.firstEntry();
                var nodeId = entry.getValue();
                return Optional.ofNullable(nodes.get(nodeId));
            } finally {
                lock.unlock();
            }
        }

        public List<CacheNode> getReplicationNodes(String key, int replicationFactor) {
            lock.lock();
            try {
                if (ring.isEmpty()) return List.of();
                var hash = hash(key);
                var result = new LinkedHashSet<String>();
                var entry = ring.ceilingEntry(hash);
                if (entry == null) entry = ring.firstEntry();

                var currentEntry = entry;
                for (int i = 0; i < ring.size() && result.size() < replicationFactor; i++) {
                    if (currentEntry == null) currentEntry = ring.firstEntry();
                    var nodeId = currentEntry.getValue();
                    if (nodes.containsKey(nodeId)) {
                        result.add(nodeId);
                    }
                    currentEntry = ring.higherEntry(currentEntry.getKey());
                    if (currentEntry == null) currentEntry = ring.firstEntry();
                }

                return result.stream().map(nodes::get).filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableList());
            } finally {
                lock.unlock();
            }
        }

        public Collection<CacheNode> getAllNodes() { return List.copyOf(nodes.values()); }
        public int nodeCount() { return nodes.size(); }

        private static long hash(String key) {
            try {
                var digest = MessageDigest.getInstance("MD5");
                var hashBytes = digest.digest(key.getBytes());
                long hash = 0;
                for (int i = 0; i < 8; i++) {
                    hash = (hash << 8) | (hashBytes[i] & 0xFF);
                }
                return hash & Long.MAX_VALUE;
            } catch (NoSuchAlgorithmException e) {
                return (long) key.hashCode() & Long.MAX_VALUE;
            }
        }
    }

    public static final class CacheStorage {
        private final String nodeId;
        private final Map<String, CacheEntry<?>> store = new ConcurrentHashMap<>();
        private final Map<String, Set<String>> keyTags = new ConcurrentHashMap<>();
        private final AtomicLong versionCounter = new AtomicLong(0);
        private final AtomicLong currentBytes = new AtomicLong(0);
        private final long maxBytes;
        private final Lock lock = new ReentrantLock();

        public CacheStorage(String nodeId, long maxBytes) {
            this.nodeId = Objects.requireNonNull(nodeId);
            this.maxBytes = maxBytes;
        }

        @SuppressWarnings("unchecked")
        public <V> Optional<CacheEntry<V>> get(String key) {
            var entry = (CacheEntry<V>) store.get(key);
            if (entry == null) return Optional.empty();
            if (entry.isExpired()) {
                store.remove(key);
                return Optional.empty();
            }
            return Optional.of(entry);
        }

        public <V> CacheEntry<V> put(String key, V value, long ttlSeconds) {
            var expiresAt = ttlSeconds > 0 ? Instant.now().plusSeconds(ttlSeconds) : null;
            var version = versionCounter.incrementAndGet();
            var entry = new CacheEntry<>(key, value, Instant.now(), expiresAt, version);
            store.put(key, entry);
            return entry;
        }

        public boolean remove(String key) {
            return store.remove(key) != null;
        }

        public void addTag(String key, String tag) {
            keyTags.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet()).add(key);
        }

        public List<String> getKeysByTag(String tag) {
            var keys = keyTags.get(tag);
            return keys != null ? List.copyOf(keys) : List.of();
        }

        public void clear() { store.clear(); keyTags.clear(); }

        public long size() { return store.size(); }
        public boolean isEmpty() { return store.isEmpty(); }

        public Map<String, CacheEntry<?>> getAll() { return Map.copyOf(store); }

        @SuppressWarnings("unchecked")
        public <V> CacheEntry<V> replicateFrom(String key, V value, long ttlSeconds, long version) {
            var expiresAt = ttlSeconds > 0 ? Instant.now().plusSeconds(ttlSeconds) : null;
            var entry = new CacheEntry<>(key, value, Instant.now(), expiresAt, version);
            store.put(key, entry);
            return entry;
        }
    }

    public static final class DistributedCacheManager {
        private final ConsistentHashRing ring;
        private final Map<String, CacheStorage> storages = new ConcurrentHashMap<>();
        private final int replicationFactor;
        private final ScheduledExecutorService maintenanceExecutor = Executors.newSingleThreadScheduledExecutor();
        private final Map<String, NodeStatus> nodeStatuses = new ConcurrentHashMap<>();
        private final List<String> failureLog = new CopyOnWriteArrayList<>();
        private volatile boolean running = true;

        public DistributedCacheManager(int virtualNodes, int replicationFactor) {
            this.ring = new ConsistentHashRing(virtualNodes);
            this.replicationFactor = replicationFactor;
            maintenanceExecutor.scheduleAtFixedRate(this::performMaintenance, 10, 10, TimeUnit.SECONDS);
        }

        public CacheNode addNode(String nodeId, long capacityBytes) {
            var node = new CacheNode(nodeId, "10.0.0." + (ring.nodeCount() + 1),
                    6379 + ring.nodeCount(), capacityBytes, 0);
            var storage = new CacheStorage(nodeId, capacityBytes);
            storages.put(nodeId, storage);
            ring.addNode(node);
            nodeStatuses.put(nodeId, NodeStatus.ONLINE);
            return node;
        }

        public void markNodeOffline(String nodeId) {
            nodeStatuses.put(nodeId, NodeStatus.OFFLINE);
            ring.removeNode(nodeId);
            failureLog.add("[%s] Node %s marked OFFLINE".formatted(Instant.now(), nodeId));
            redistributeData(nodeId);
        }

        public void recoverNode(String nodeId) {
            nodeStatuses.put(nodeId, NodeStatus.RECOVERING);
            var node = ring.getAllNodes().stream()
                    .filter(n -> n.nodeId().equals(nodeId))
                    .findFirst().orElse(null);
            if (node != null) {
                ring.addNode(node);
            }
            nodeStatuses.put(nodeId, NodeStatus.ONLINE);
            failureLog.add("[%s] Node %s recovered and ONLINE".formatted(Instant.now(), nodeId));
        }

        private void redistributeData(String failedNodeId) {
            System.out.println("  Redistributing data from failed node: " + failedNodeId);
        }

        @SuppressWarnings("unchecked")
        public <V> boolean put(String key, V value, long ttlSeconds) {
            var primaryNode = ring.getNode(key);
            if (primaryNode.isEmpty()) return false;

            var primaryStorage = storages.get(primaryNode.get().nodeId());
            if (primaryStorage == null) return false;

            var entry = primaryStorage.put(key, value, ttlSeconds);

            var replicas = ring.getReplicationNodes(key, replicationFactor);
            for (var replica : replicas) {
                if (!replica.nodeId().equals(primaryNode.get().nodeId())) {
                    var replicaStorage = storages.get(replica.nodeId());
                    if (replicaStorage != null) {
                        replicaStorage.replicateFrom(key, value, ttlSeconds, entry.version());
                    }
                }
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        public <V> Optional<CacheEntry<V>> get(String key) {
            var primaryNode = ring.getNode(key);
            if (primaryNode.isPresent()) {
                var storage = storages.get(primaryNode.get().nodeId());
                if (storage != null) {
                    var result = storage.<V>get(key);
                    if (result.isPresent()) return result;
                }
            }

            var replicas = ring.getReplicationNodes(key, replicationFactor);
            for (var replica : replicas) {
                var storage = storages.get(replica.nodeId());
                if (storage != null) {
                    var result = storage.<V>get(key);
                    if (result.isPresent()) return result;
                }
            }
            return Optional.empty();
        }

        public boolean remove(String key) {
            var primaryNode = ring.getNode(key);
            if (primaryNode.isPresent()) {
                var storage = storages.get(primaryNode.get().nodeId());
                if (storage != null) storage.remove(key);
            }
            var replicas = ring.getReplicationNodes(key, replicationFactor);
            for (var replica : replicas) {
                var storage = storages.get(replica.nodeId());
                if (storage != null) storage.remove(key);
            }
            return true;
        }

        private void performMaintenance() {
            if (!running) return;
            for (var storage : storages.values()) {
                var allEntries = storage.getAll();
                for (var entry : allEntries.entrySet()) {
                    if (entry.getValue().isExpired()) {
                        storage.remove(entry.getKey());
                    }
                }
            }
        }

        public NodeStatus getNodeStatus(String nodeId) {
            return nodeStatuses.getOrDefault(nodeId, NodeStatus.OFFLINE);
        }

        public List<String> getFailureLog() { return List.copyOf(failureLog); }
        public int nodeCount() { return ring.nodeCount(); }
        public long totalEntries() {
            return storages.values().stream().mapToLong(CacheStorage::size).sum();
        }

        public void shutdown() {
            running = false;
            maintenanceExecutor.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Distributed Cache ===%n".formatted());

        var cache = new DistributedCacheManager(150, 3);

        System.out.println("--- Adding Nodes ---");
        var node1 = cache.addNode("node-a", 1024 * 1024 * 100);
        var node2 = cache.addNode("node-b", 1024 * 1024 * 100);
        var node3 = cache.addNode("node-c", 1024 * 1024 * 100);
        var node4 = cache.addNode("node-d", 1024 * 1024 * 100);
        System.out.println("  Added %d nodes".formatted(cache.nodeCount()));

        System.out.println("%n--- Put & Get Values ---%n".formatted());
        cache.put("user:1001", "{\"name\":\"Alice\",\"email\":\"alice@example.com\"}", 3600);
        cache.put("user:1002", "{\"name\":\"Bob\",\"email\":\"bob@example.com\"}", 3600);
        cache.put("session:abc123", "{\"userId\":1001,\"token\":\"tok_abc\"}", 1800);
        cache.put("config:app", "{\"theme\":\"dark\",\"lang\":\"en\"}", 0);
        cache.put("product:5001", "{\"name\":\"Laptop\",\"price\":1299.99}", 7200);
        cache.put("product:5002", "{\"name\":\"Mouse\",\"price\":49.99}", 7200);

        var val1 = cache.<String>get("user:1001");
        val1.ifPresentOrElse(
            e -> System.out.println("  user:1001 = " + e.value()),
            () -> System.out.println("  user:1001 not found")
        );

        var val2 = cache.<String>get("config:app");
        val2.ifPresent(e -> System.out.println("  config:app = " + e.value()));

        System.out.println("%n--- Remove a Key ---%n".formatted());
        cache.remove("session:abc123");
        var removed = cache.<String>get("session:abc123");
        System.out.println("  session:abc123 after remove: " + (removed.isEmpty() ? "not found (correct)" : "found"));

        System.out.println("%n--- Cache Entry Info ---%n".formatted());
        val1.ifPresent(e ->
            System.out.println("  Version: %d, Created: %s, Expires: %s, Expired: %s"
                    .formatted(e.version(), e.createdAt(), e.expiresAt(), e.isExpired())));

        System.out.println("%n--- Node Failure & Recovery ---%n".formatted());
        System.out.println("  Initial status of node-b: " + cache.getNodeStatus("node-b"));
        cache.markNodeOffline("node-b");
        System.out.println("  After failure: " + cache.getNodeStatus("node-b"));
        System.out.println("  Nodes now: " + cache.nodeCount());

        var valAfterFail = cache.<String>get("user:1001");
        System.out.println("  Get user:1001 after node-b failure: " +
                (valAfterFail.isPresent() ? valAfterFail.get().value() : "not found"));

        cache.recoverNode("node-b");
        System.out.println("  After recovery: " + cache.getNodeStatus("node-b"));
        System.out.println("  Nodes now: " + cache.nodeCount());

        System.out.println("%n--- Failure Log ---%n".formatted());
        cache.getFailureLog().forEach(log -> System.out.println("  " + log));

        System.out.println("%n--- Virtual Threads: Concurrent Operations ---%n".formatted());
        var latch = new CountDownLatch(20);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10; i++) {
                final int idx = i;
                executor.submit(() -> {
                    var key = "concurrent:key:" + idx;
                    var value = "{\"data\":\"vt-test-" + idx + "\",\"ts\":" + System.currentTimeMillis() + "}";
                    cache.put(key, value, 600);
                    latch.countDown();
                });
                executor.submit(() -> {
                    var key = "concurrent:key:" + (idx % 10);
                    var val = cache.<String>get(key);
                    if (val.isPresent()) {
                        System.out.println("  [VT-%d] Got: %s".formatted(idx, val.get().value()));
                    }
                    latch.countDown();
                });
            }
        }
        latch.await(5, TimeUnit.SECONDS);

        System.out.println("%n--- Pattern Matching on CacheEntries ---%n".formatted());
        for (int i = 0; i < 3; i++) {
            var key = "concurrent:key:" + i;
            var entry = cache.<String>get(key);
            entry.ifPresent(e -> {
                switch (e) {
                    case CacheEntry<String> ce when ce.isExpired() ->
                        System.out.println("  Expired: " + ce.key());
                    case CacheEntry<String> ce when ce.value().contains("laptop") || ce.value().contains("Laptop") ->
                        System.out.println("  Product entry: " + ce.key());
                    case CacheEntry<?> ce ->
                        System.out.println("  Active cache: " + ce.key() + " (v" + ce.version() + ")");
                }
            });
        }

        System.out.println("%n--- Consistent Hash Verification ---%n".formatted());
        var testKeys = List.of("user:1001", "user:1002", "product:5001", "config:app", "order:1", "order:2");
        for (var key : testKeys) {
            var node = cache.get(key);
            System.out.println("  %s -> %s".formatted(key, node.isPresent() ? "found" : "not found"));
        }

        System.out.println("%nFinal Stats: %d nodes, %d total entries"
                .formatted(cache.nodeCount(), cache.totalEntries()));
        cache.shutdown();
        System.out.println("=== Done ===");
    }
}
