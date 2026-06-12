package phase15.systems;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;

class _138_CachingExample {

    sealed interface CacheEntry permits ValidEntry, ExpiredEntry {}
    record ValidEntry(Object value, long expiryEpochMs) implements CacheEntry {}
    record ExpiredEntry() implements CacheEntry {}

    public static class Cache<K, V> {
        private final ConcurrentHashMap<K, V> store = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<K, Long> ttlMap = new ConcurrentHashMap<>();
        private final LinkedHashMap<K, V> lruMap;
        private final int maxSize;

        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();
        private final AtomicLong evictions = new AtomicLong();

        public Cache(int maxSize) {
            this.maxSize = maxSize;
            this.lruMap = new LinkedHashMap<>(16, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    boolean evict = size() > Cache.this.maxSize;
                    if (evict) evictions.incrementAndGet();
                    return evict;
                }
            };
        }

        public void put(K key, V value, long ttlMillis) {
            store.put(key, value);
            ttlMap.put(key, System.currentTimeMillis() + ttlMillis);
            synchronized (lruMap) { lruMap.put(key, value); }
        }

        public Optional<V> get(K key) {
            Long expiry = ttlMap.get(key);
            if (expiry != null && System.currentTimeMillis() > expiry) {
                store.remove(key);
                ttlMap.remove(key);
                misses.incrementAndGet();
                return Optional.empty();
            }
            V val = store.get(key);
            if (val != null) {
                hits.incrementAndGet();
                synchronized (lruMap) { lruMap.get(key); }
                return Optional.of(val);
            }
            misses.incrementAndGet();
            return Optional.empty();
        }

        public boolean containsKey(K key) {
            Long expiry = ttlMap.get(key);
            if (expiry != null && System.currentTimeMillis() > expiry) {
                store.remove(key);
                ttlMap.remove(key);
                return false;
            }
            return store.containsKey(key);
        }

        public void remove(K key) {
            store.remove(key);
            ttlMap.remove(key);
            synchronized (lruMap) { lruMap.remove(key); }
        }

        public void clear() {
            store.clear();
            ttlMap.clear();
            synchronized (lruMap) { lruMap.clear(); }
        }

        public int size() { return store.size(); }

        public Map<String, Long> stats() {
            return Map.of(
                "hits", hits.get(),
                "misses", misses.get(),
                "evictions", evictions.get(),
                "size", (long) store.size()
            );
        }
    }

    public static void main(String[] args) throws Exception {
        var cache = new Cache<String, String>(3);

        cache.put("a", "apple", 500);
        cache.put("b", "banana", 2000);
        cache.put("c", "cherry", 2000);

        System.out.println("Get a: " + cache.get("a").orElse("MISS"));
        System.out.println("Stats: " + cache.stats());

        cache.put("d", "date", 2000); // evicts LRU
        System.out.println("After adding d, has b: " + cache.containsKey("b"));
        System.out.println("Stats: " + cache.stats());

        Thread.sleep(600);
        System.out.println("After TTL expiry, has a: " + cache.containsKey("a"));
        System.out.println("Get a: " + cache.get("a").orElse("MISS"));
        System.out.println("Stats: " + cache.stats());

        cache.put("e", "elderberry", 2000);
        cache.put("f", "fig", 2000);
        System.out.println("Stats after more inserts: " + cache.stats());
    }
}
