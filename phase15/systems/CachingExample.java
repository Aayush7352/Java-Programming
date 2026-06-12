package phase15.systems;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.*;

public class CachingExample {

    record CacheEntry<V>(V value, long expiryTime) {}

    record CacheStats(long hits, long misses, long evictions, long size) {
        public double hitRate() {
            long total = hits + misses;
            return total == 0 ? 0.0 : (double) hits / total * 100;
        }
    }

    static class LRUCache<K, V> {
        private final ConcurrentHashMap<K, CacheEntry<V>> map = new ConcurrentHashMap<>();
        private final LinkedBlockingDeque<K> accessOrder = new LinkedBlockingDeque<>();
        private final int maxSize;
        private final long ttlMillis;
        private final ReentrantLock lock = new ReentrantLock();
        private long hits, misses, evictions;

        public LRUCache(int maxSize, long ttlMillis) {
            this.maxSize = maxSize;
            this.ttlMillis = ttlMillis;
        }

        public void put(K key, V value) {
            long expiry = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : Long.MAX_VALUE;
            map.put(key, new CacheEntry<>(value, expiry));
            accessOrder.addLast(key);
            evictIfNeeded();
        }

        public Optional<V> get(K key) {
            CacheEntry<V> entry = map.get(key);
            if (entry == null) {
                lock.lock();
                try { misses++; } finally { lock.unlock(); }
                return Optional.empty();
            }
            if (System.currentTimeMillis() > entry.expiryTime) {
                map.remove(key);
                lock.lock();
                try { evictions++; misses++; } finally { lock.unlock(); }
                return Optional.empty();
            }
            accessOrder.remove(key);
            accessOrder.addLast(key);
            lock.lock();
            try { hits++; } finally { lock.unlock(); }
            return Optional.of(entry.value());
        }

        public boolean remove(K key) {
            CacheEntry<V> removed = map.remove(key);
            if (removed != null) {
                accessOrder.remove(key);
                lock.lock();
                try { evictions++; } finally { lock.unlock(); }
                return true;
            }
            return false;
        }

        private void evictIfNeeded() {
            while (map.size() > maxSize) {
                K oldest = accessOrder.pollFirst();
                if (oldest != null && map.remove(oldest) != null) {
                    lock.lock();
                    try { evictions++; } finally { lock.unlock(); }
                } else break;
            }
        }

        public CacheStats stats() {
            lock.lock();
            try {
                return new CacheStats(hits, misses, evictions, map.size());
            } finally { lock.unlock(); }
        }

        public void clear() {
            map.clear();
            accessOrder.clear();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Caching Example (LRU + TTL) ===\n");

        LRUCache<String, String> cache = new LRUCache<>(3, 2000);

        cache.put("A", "Alpha");
        cache.put("B", "Beta");
        cache.put("C", "Gamma");
        System.out.println("Inserted A, B, C into cache (maxSize=3, TTL=2s)");

        System.out.println("\n--- Cache Hits ---");
        for (var key : List.of("A", "B", "C", "D")) {
            cache.get(key).ifPresentOrElse(
                v -> System.out.println("  GET " + key + " -> " + v),
                () -> System.out.println("  GET " + key + " -> MISS")
            );
        }

        System.out.println("\n--- LRU Eviction ---");
        cache.put("D", "Delta");
        cache.get("A").ifPresentOrElse(
            v -> System.out.println("  GET A -> " + v),
            () -> System.out.println("  GET A -> MISS (evicted as LRU)")
        );
        cache.get("D").ifPresentOrElse(
            v -> System.out.println("  GET D -> " + v),
            () -> System.out.println("  GET D -> MISS")
        );

        System.out.println("\n--- TTL Expiration ---");
        Thread.sleep(2100);
        cache.get("B").ifPresentOrElse(
            v -> System.out.println("  GET B -> " + v),
            () -> System.out.println("  GET B -> MISS (expired by TTL)")
        );

        System.out.println("\n--- Cache Statistics ---");
        CacheStats stats = cache.stats();
        System.out.println("  Size: " + stats.size());
        System.out.println("  Hits: " + stats.hits());
        System.out.println("  Misses: " + stats.misses());
        System.out.println("  Evictions: " + stats.evictions());
        System.out.printf("  Hit Rate: %.2f%%%n", stats.hitRate());

        System.out.println("\n--- Concurrent Access ---");
        LRUCache<Integer, String> shared = new LRUCache<>(100, 5000);
        var threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            int id = i;
            threads[i] = Thread.ofVirtual().start(() -> {
                for (int j = 0; j < 100; j++) {
                    shared.put(id * 100 + j, "val-" + id + "-" + j);
                    shared.get(id * 100 + j);
                }
            });
        }
        for (var t : threads) t.join();
        System.out.println("  After 10 virtual threads x 100 ops each:");
        System.out.println("  Stats: " + shared.stats());

        System.out.println("\n=== Caching Example Complete ===");
    }
}
