package phase16.projects;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/**
 * DistributedCache.java
 *
 * Distributed cache: Consistent hashing, data partitioning, replication,
 * failure detection, cache node management.
 * Self-contained JDK-only implementation.
 */
public class DistributedCache {

    // ═══════════════════════════════════════════════
    // Records
    // ═══════════════════════════════════════════════

    record CacheEntry(String key, String value, long expiryEpochMs, Instant createdAt) {
        public boolean isExpired() {
            return expiryEpochMs > 0 && System.currentTimeMillis() > expiryEpochMs;
        }
        public boolean isExpiredAt(long time) { return expiryEpochMs > 0 && time > expiryEpochMs; }

        CacheEntry touch() {
            return new CacheEntry(key, value, expiryEpochMs, Instant.now());
        }
    }

    record CacheNode(String nodeId, String host, int port, boolean isAlive,
                     Instant lastHeartbeat, int load) {
        CacheNode withAlive(boolean alive) {
            return new CacheNode(nodeId, host, port, alive, Instant.now(), load);
        }
        CacheNode withLoad(int l) {
            return new CacheNode(nodeId, host, port, isAlive, lastHeartbeat, l);
        }
        CacheNode withHeartbeat() {
            return new CacheNode(nodeId, host, port, isAlive, Instant.now(), load);
        }
    }

    record PartitionRange(int start, int end) {}

    record CacheStats(long totalEntries, long hits, long misses, long evictions, int nodeCount) {}

    // ═══════════════════════════════════════════════
    // Consistent Hashing
    // ═══════════════════════════════════════════════

    static final class ConsistentHashRing {
        private final TreeMap<Long, String> ring = new TreeMap<>();
        private final int virtualNodesPerPhysical;
        private final HashFunction hashFn;

        ConsistentHashRing(int virtualNodesPerPhysical) {
            this.virtualNodesPerPhysical = virtualNodesPerPhysical;
            this.hashFn = new HashFunction();
        }

        public void addNode(String nodeId) {
            for (int i = 0; i < virtualNodesPerPhysical; i++) {
                long hash = hashFn.hash(nodeId + ":vn:" + i);
                ring.put(hash, nodeId);
            }
        }

        public void removeNode(String nodeId) {
            ring.entrySet().removeIf(e -> e.getValue().equals(nodeId));
        }

        public String getNode(String key) {
            if (ring.isEmpty()) return null;
            long hash = hashFn.hash(key);
            var entry = ring.ceilingEntry(hash);
            if (entry == null) {
                entry = ring.firstEntry();
            }
            return entry.getValue();
        }

        public List<String> getNodes(String key, int count) {
            if (ring.isEmpty()) return List.of();
            long hash = hashFn.hash(key);
            var result = new LinkedHashSet<String>();

            var entry = ring.ceilingEntry(hash);
            if (entry == null) entry = ring.firstEntry();

            // Walk the ring
            var tailMap = ring.tailMap(entry.getKey(), true);
            for (var e : tailMap.entrySet()) {
                result.add(e.getValue());
                if (result.size() >= count) break;
            }
            if (result.size() < count) {
                for (var e : ring.headMap(entry.getKey(), false).entrySet()) {
                    result.add(e.getValue());
                    if (result.size() >= count) break;
                }
            }
            return List.copyOf(result);
        }

        public int getNodeCount() {
            return (int) ring.values().stream().distinct().count();
        }

        public Set<String> getAllNodes() {
            return Set.copyOf(ring.values());
        }

        public int getVirtualNodeCount() { return ring.size(); }
    }

    static final class HashFunction {
        public long hash(String key) {
            try {
                var md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(key.getBytes());
                long hash = 0;
                for (int i = 0; i < 8; i++) {
                    hash = (hash << 8) | (digest[i] & 0xFF);
                }
                return hash & Long.MAX_VALUE;
            } catch (Exception e) {
                return key.hashCode() & Long.MAX_VALUE;
            }
        }
    }

    // ═══════════════════════════════════════════════
    // Cache Node Storage
    // ═══════════════════════════════════════════════

    static final class CacheNodeStorage {
        private final String nodeId;
        private final ConcurrentHashMap<String, CacheEntry> store = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Long> accessCount = new ConcurrentHashMap<>();
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong evictions = new AtomicLong(0);
        private final int maxEntries;

        CacheNodeStorage(String nodeId, int maxEntries) {
            this.nodeId = nodeId;
            this.maxEntries = maxEntries;
        }

        public void put(String key, String value, long ttlSeconds) {
            long expiry = ttlSeconds > 0 ? System.currentTimeMillis() + ttlSeconds * 1000 : Long.MAX_VALUE;
            var entry = new CacheEntry(key, value, expiry, Instant.now());
            store.put(key, entry);
            accessCount.put(key, 0L);
            evictIfNeeded();
        }

        public Optional<String> get(String key) {
            var entry = store.get(key);
            if (entry == null) {
                misses.incrementAndGet();
                return Optional.empty();
            }
            if (entry.isExpired()) {
                store.remove(key);
                accessCount.remove(key);
                evictions.incrementAndGet();
                misses.incrementAndGet();
                return Optional.empty();
            }
            hits.incrementAndGet();
            accessCount.merge(key, 1L, Long::sum);
            store.put(key, entry.touch()); // update access time
            return Optional.of(entry.value());
        }

        public boolean remove(String key) {
            var removed = store.remove(key);
            accessCount.remove(key);
            return removed != null;
        }

        public int size() { return store.size(); }

        public boolean containsKey(String key) {
            var entry = store.get(key);
            return entry != null && !entry.isExpired();
        }

        public Set<String> keySet() { return store.keySet(); }

        public Map<String, CacheEntry> getAll() { return new HashMap<>(store); }

        public void putAll(Map<String, CacheEntry> entries) {
            store.putAll(entries);
        }

        private void evictIfNeeded() {
            if (store.size() <= maxEntries) return;
            // Evict LRU (by access count)
            var lruKey = accessCount.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
            lruKey.ifPresent(key -> {
                store.remove(key);
                accessCount.remove(key);
                evictions.incrementAndGet();
            });
        }

        public CacheNodeStats stats() {
            return new CacheNodeStats(nodeId, store.size(), hits.get(), misses.get(), evictions.get());
        }

        public void clear() {
            store.clear();
            accessCount.clear();
        }
    }

    record CacheNodeStats(String nodeId, int size, long hits, long misses, long evictions) {
        public double hitRate() {
            long total = hits + misses;
            return total == 0 ? 0 : (double) hits / total * 100;
        }
    }

    // ═══════════════════════════════════════════════
    // Distributed Cache Manager
    // ═══════════════════════════════════════════════

    static final class DistributedCacheManager {
        private final ConsistentHashRing ring;
        private final ConcurrentHashMap<String, CacheNodeStorage> nodes = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, CacheNode> nodeInfo = new ConcurrentHashMap<>();
        private final int replicationFactor;
        private final AtomicLong globalHits = new AtomicLong(0);
        private final AtomicLong globalMisses = new AtomicLong(0);
        private final ScheduledExecutorService healthChecker = Executors.newSingleThreadScheduledExecutor();
        private final AtomicBoolean running = new AtomicBoolean(true);

        DistributedCacheManager(int virtualNodes, int replicationFactor) {
            this.ring = new ConsistentHashRing(virtualNodes);
            this.replicationFactor = replicationFactor;
            startHealthCheck();
        }

        public CacheNode addNode(String nodeId, String host, int port, int maxEntries) {
            var storage = new CacheNodeStorage(nodeId, maxEntries);
            var node = new CacheNode(nodeId, host, port, true, Instant.now(), 0);
            nodes.put(nodeId, storage);
            nodeInfo.put(nodeId, node);
            ring.addNode(nodeId);
            redistributeData(nodeId);
            return node;
        }

        public void removeNode(String nodeId) {
            var storage = nodes.get(nodeId);
            if (storage == null) return;

            // Redistribute data to other nodes before removing
            var allData = storage.getAll();
            ring.removeNode(nodeId);
            nodes.remove(nodeId);
            nodeInfo.remove(nodeId);

            for (var entry : allData.entrySet()) {
                String targetNode = ring.getNode(entry.getKey());
                if (targetNode != null) {
                    var targetStorage = nodes.get(targetNode);
                    if (targetStorage != null) {
                        targetStorage.put(entry.getKey(), entry.getValue().value(),
                            (entry.getValue().expiryEpochMs() - System.currentTimeMillis()) / 1000);
                    }
                }
            }
        }

        public void put(String key, String value, long ttlSeconds) {
            var targetNodes = ring.getNodes(key, replicationFactor);
            for (var nodeId : targetNodes) {
                var storage = nodes.get(nodeId);
                if (storage != null) {
                    storage.put(key, value, ttlSeconds);
                    nodeInfo.computeIfPresent(nodeId, (k, n) -> n.withLoad(storage.size()));
                }
            }
        }

        public Optional<String> get(String key) {
            var targetNodes = ring.getNodes(key, replicationFactor);
            for (var nodeId : targetNodes) {
                var storage = nodes.get(nodeId);
                if (storage != null) {
                    var result = storage.get(key);
                    if (result.isPresent()) {
                        globalHits.incrementAndGet();
                        return result;
                    }
                }
            }
            globalMisses.incrementAndGet();
            return Optional.empty();
        }

        public boolean remove(String key) {
            var targetNodes = ring.getNodes(key, replicationFactor);
            boolean removed = false;
            for (var nodeId : targetNodes) {
                var storage = nodes.get(nodeId);
                if (storage != null && storage.remove(key)) {
                    removed = true;
                }
            }
            return removed;
        }

        public CacheStats getStats() {
            long totalEntries = nodes.values().stream().mapToInt(CacheNodeStorage::size).sum();
            long totalHits = globalHits.get();
            long totalMisses = globalMisses.get();
            long totalEvictions = nodes.values().stream().mapToLong(s -> s.stats().evictions()).sum();
            return new CacheStats(totalEntries, totalHits, totalMisses, totalEvictions, nodes.size());
        }

        public List<CacheNodeStats> getNodeStats() {
            return nodes.entrySet().stream()
                .map(e -> e.getValue().stats())
                .collect(Collectors.toList());
        }

        public List<CacheNode> getAllNodes() {
            return nodeInfo.values().stream()
                .sorted(Comparator.comparing(CacheNode::nodeId))
                .collect(Collectors.toList());
        }

        public Set<String> getKeysOnNode(String nodeId) {
            var storage = nodes.get(nodeId);
            return storage == null ? Set.of() : storage.keySet();
        }

        public int getNodeCount() { return nodes.size(); }
        public int getVirtualNodeCount() { return ring.getVirtualNodeCount(); }

        private void redistributeData(String newNodeId) {
            // Simple redistribution: rehash all keys that now belong to new node
            for (var entry : nodes.entrySet()) {
                if (entry.getKey().equals(newNodeId)) continue;
                var storage = entry.getValue();
                var keysToMove = new ArrayList<String>();
                for (var key : storage.keySet()) {
                    String owner = ring.getNode(key);
                    if (owner != null && owner.equals(newNodeId)) {
                        keysToMove.add(key);
                    }
                }
                for (var key : keysToMove) {
                    var val = storage.get(key);
                    storage.remove(key);
                    if (val.isPresent()) {
                        var targetStorage = nodes.get(newNodeId);
                        if (targetStorage != null) {
                            targetStorage.put(key, val.get(), 3600);
                        }
                    }
                }
            }
        }

        private void startHealthCheck() {
            healthChecker.scheduleAtFixedRate(() -> {
                if (!running.get()) return;
                for (var entry : nodeInfo.entrySet()) {
                    var node = entry.getValue();
                    // Simulate health check: mark alive with heartbeat
                    nodeInfo.put(entry.getKey(), node.withHeartbeat().withAlive(true));
                }
            }, 1, 5, TimeUnit.SECONDS);
        }

        // Simulate node failure
        public void markNodeFailed(String nodeId) {
            nodeInfo.computeIfPresent(nodeId, (k, n) -> n.withAlive(false));
        }

        public void recoverNode(String nodeId) {
            nodeInfo.computeIfPresent(nodeId, (k, n) -> n.withAlive(true));
        }

        public boolean isNodeAlive(String nodeId) {
            var node = nodeInfo.get(nodeId);
            return node != null && node.isAlive();
        }

        public void shutdown() {
            running.set(false);
            healthChecker.shutdown();
        }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        System.out.println("=== Distributed Cache ===\n");

        // ─── Consistent Hash Ring Test ───
        System.out.println("--- Consistent Hashing ---");
        var hashRing = new ConsistentHashRing(3);
        hashRing.addNode("node-A");
        hashRing.addNode("node-B");
        hashRing.addNode("node-C");

        System.out.println("  Virtual nodes: " + hashRing.getVirtualNodeCount());
        System.out.println("  Physical nodes: " + hashRing.getNodeCount());

        // Show key distribution
        var nodeCount = new HashMap<String, Integer>();
        for (int i = 0; i < 1000; i++) {
            String node = hashRing.getNode("key-" + i);
            nodeCount.merge(node, 1, Integer::sum);
        }
        System.out.println("  Key distribution (1000 keys):");
        nodeCount.forEach((node, count) ->
            System.out.println("    " + node + ": " + count + " keys"));

        // ─── Distributed Cache ───
        System.out.println("\n--- Distributed Cache Setup ---");
        var cache = new DistributedCacheManager(3, 2); // 2 replicas

        cache.addNode("cache-1", "10.0.0.1", 6379, 1000);
        cache.addNode("cache-2", "10.0.0.2", 6379, 1000);
        cache.addNode("cache-3", "10.0.0.3", 6379, 1000);
        cache.addNode("cache-4", "10.0.0.4", 6379, 1000);
        System.out.println("  Added 4 cache nodes");
        System.out.println("  Virtual nodes: " + cache.getVirtualNodeCount());

        // ─── Put and Get ───
        System.out.println("\n--- Put & Get ---");
        cache.put("user:1001", "{name:\"Alice\",age:30}", 3600);
        cache.put("user:1002", "{name:\"Bob\",age:25}", 3600);
        cache.put("session:abc123", "token-xyz", 1800);
        cache.put("config:app", "{\"debug\":true}", 0); // No TTL

        var val = cache.get("user:1001");
        System.out.println("  GET user:1001 -> " + val.orElse("NOT FOUND"));

        var val2 = cache.get("nonexistent");
        System.out.println("  GET nonexistent -> " + val2.orElse("NOT FOUND"));

        // ─── Data Distribution ───
        System.out.println("\n--- Data Distribution ---");
        for (var node : cache.getAllNodes()) {
            var keys = cache.getKeysOnNode(node.nodeId());
            System.out.println("  " + node.nodeId() + " (" + node.host() + "): " + keys.size() + " keys");
            for (var k : keys) {
                System.out.println("    " + k);
            }
        }

        // ─── Cache Stats ───
        System.out.println("\n--- Cache Statistics ---");
        var stats = cache.getStats();
        System.out.println("  Total entries: " + stats.totalEntries());
        System.out.println("  Hits: " + stats.hits());
        System.out.println("  Misses: " + stats.misses());
        System.out.println("  Evictions: " + stats.evictions());
        System.out.println("  Nodes: " + stats.nodeCount());

        for (var ns : cache.getNodeStats()) {
            System.out.printf("  %s: size=%d hits=%d misses=%d (hit rate: %.1f%%)%n",
                ns.nodeId(), ns.size(), ns.hits(), ns.misses(), ns.hitRate());
        }

        // ─── Node Failure & Recovery ───
        System.out.println("\n--- Node Failure Simulation ---");
        System.out.println("  Before failure:");
        var beforeVal = cache.get("user:1001");
        System.out.println("  GET user:1001 -> " + beforeVal.orElse("NOT FOUND"));

        cache.markNodeFailed("cache-1");
        System.out.println("  Marked cache-1 as failed");

        var duringFailure = cache.get("user:1001"); // Should still be available via replica
        System.out.println("  GET user:1001 after cache-1 failure -> " + duringFailure.orElse("NOT FOUND (replica works)"));

        cache.recoverNode("cache-1");
        System.out.println("  Recovered cache-1");

        // ─── Add & Remove Nodes (Rehashing) ───
        System.out.println("\n--- Dynamic Node Management ---");
        cache.addNode("cache-5", "10.0.0.5", 6379, 1000);
        System.out.println("  Added cache-5");

        var afterAdd = cache.get("user:1001");
        System.out.println("  GET user:1001 after add -> " + afterAdd.orElse("NOT FOUND"));

        cache.removeNode("cache-3");
        System.out.println("  Removed cache-3");

        var afterRemove = cache.get("user:1001");
        System.out.println("  GET user:1001 after remove -> " + afterRemove.orElse("NOT FOUND"));

        // ─── Concurrent Access ───
        System.out.println("\n--- Concurrent Access (Virtual Threads) ---");
        var vtCache = new DistributedCacheManager(3, 2);
        vtCache.addNode("vt-node-1", "10.0.0.1", 6379, 5000);
        vtCache.addNode("vt-node-2", "10.0.0.2", 6379, 5000);
        vtCache.addNode("vt-node-3", "10.0.0.3", 6379, 5000);
        var opsCount = new AtomicInteger(0);

        var vtThreads = new Thread[20];
        for (int i = 0; i < 20; i++) {
            int id = i;
            vtThreads[i] = Thread.ofVirtual().start(() -> {
                for (int j = 0; j < 100; j++) {
                    String key = "vt-key-" + (id * 100 + j);
                    vtCache.put(key, "value-" + id + "-" + j, 3600);
                    vtCache.get(key);
                    opsCount.incrementAndGet();
                }
            });
        }
        for (var t : vtThreads) t.join();

        var vtStats = vtCache.getStats();
        System.out.println("  Operations: " + opsCount.get());
        System.out.printf("  Hit rate: %.2f%%%n",
            (double) vtStats.hits() / (vtStats.hits() + vtStats.misses()) * 100);
        System.out.println("  Total entries: " + vtStats.totalEntries());

        vtCache.shutdown();
        cache.shutdown();

        System.out.println("\n=== Distributed Cache Complete ===");
    }
}
