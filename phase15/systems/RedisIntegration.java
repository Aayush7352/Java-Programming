package phase15.systems;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * RedisIntegration.java
 *
 * Simulates Redis concepts: Jedis/Lettuce-style commands (SET/GET/EXPIRE),
 * connection pool, and Pub/Sub messaging.
 *
 * This is a self-contained JDK-only simulation with no external dependencies.
 */
public class RedisIntegration {

    // ──────────────────────────────────────────────
    // Core Redis data structures
    // ──────────────────────────────────────────────

    static record RedisValue(String value, long expiryEpochMs) {
        public boolean isExpired() {
            return expiryEpochMs > 0 && System.currentTimeMillis() > expiryEpochMs;
        }
    }

    static final class RedisStore {
        private final ConcurrentHashMap<String, RedisValue> store = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Set<String>> sets = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<String>> lists = new ConcurrentHashMap<>();
        private final ScheduledExecutorService expirer = Executors.newSingleThreadScheduledExecutor();

        RedisStore() {
            expirer.scheduleAtFixedRate(this::cleanExpired, 1, 1, TimeUnit.SECONDS);
        }

        // String commands
        public void set(String key, String value) {
            store.put(key, new RedisValue(value, 0));
        }

        public void setex(String key, long ttlSeconds, String value) {
            long expiry = System.currentTimeMillis() + ttlSeconds * 1000;
            store.put(key, new RedisValue(value, expiry));
        }

        public Optional<String> get(String key) {
            RedisValue rv = store.get(key);
            if (rv == null) return Optional.empty();
            if (rv.isExpired()) {
                store.remove(key);
                return Optional.empty();
            }
            return Optional.of(rv.value());
        }

        public boolean expire(String key, long ttlSeconds) {
            RedisValue rv = store.get(key);
            if (rv == null) return false;
            store.put(key, new RedisValue(rv.value(), System.currentTimeMillis() + ttlSeconds * 1000));
            return true;
        }

        public long ttl(String key) {
            RedisValue rv = store.get(key);
            if (rv == null) return -2;
            if (rv.expiryEpochMs() == 0) return -1;
            long remaining = (rv.expiryEpochMs() - System.currentTimeMillis()) / 1000;
            return remaining < 0 ? -2 : remaining;
        }

        public boolean del(String... keys) {
            long count = 0;
            for (var k : keys) if (store.remove(k) != null) count++;
            return count > 0;
        }

        // Set commands (simplified)
        public long sadd(String key, String... members) {
            sets.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
            Set<String> s = sets.get(key);
            long added = 0;
            for (var m : members) if (s.add(m)) added++;
            return added;
        }

        public Set<String> smembers(String key) {
            return sets.getOrDefault(key, Set.of());
        }

        // List commands (simplified)
        public long lpush(String key, String... values) {
            lists.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
            List<String> list = lists.get(key);
            for (var v : values) list.addFirst(v);
            return values.length;
        }

        public Optional<String> rpop(String key) {
            List<String> list = lists.get(key);
            if (list == null || list.isEmpty()) return Optional.empty();
            return Optional.of(list.removeLast());
        }

        // SETNX for distributed locks
        public boolean setnx(String key, String value, long ttlSeconds) {
            long expiry = System.currentTimeMillis() + ttlSeconds * 1000;
            RedisValue newVal = new RedisValue(value, expiry);
            RedisValue existing = store.putIfAbsent(key, newVal);
            if (existing == null) return true;
            if (existing.isExpired()) {
                // Lock expired - try to replace
                boolean replaced = store.replace(key, existing, newVal);
                if (replaced) return true;
                // Someone else got it
                return false;
            }
            return false;
        }

        public boolean setnx(String key, String value) {
            return setnx(key, value, 30);
        }

        public boolean exists(String key) {
            RedisValue rv = store.get(key);
            if (rv == null) return false;
            if (rv.isExpired()) { store.remove(key); return false; }
            return true;
        }

        public long incr(String key) {
            var result = store.compute(key, (k, v) -> {
                long val = (v == null || v.isExpired()) ? 0 : Long.parseLong(v.value());
                return new RedisValue(String.valueOf(val + 1), v == null ? 0 : v.expiryEpochMs());
            });
            return Long.parseLong(result.value());
        }

        private void cleanExpired() {
            store.entrySet().removeIf(e -> e.getValue().isExpired());
        }

        void shutdown() { expirer.shutdown(); }
    }

    // ──────────────────────────────────────────────
    // Connection Pool
    // ──────────────────────────────────────────────

    static final class RedisConnectionPool {
        private final RedisStore store;
        private final BlockingQueue<String> connections;
        private final int maxSize;

        RedisConnectionPool(RedisStore store, int maxSize) {
            this.store = store;
            this.maxSize = maxSize;
            this.connections = new LinkedBlockingQueue<>(maxSize);
            for (int i = 0; i < maxSize; i++) connections.add("conn-" + i);
        }

        <T> T execute(Function<RedisStore, T> cmd) {
            String conn;
            try {
                conn = connections.poll(5, TimeUnit.SECONDS);
                if (conn == null) throw new RuntimeException("Connection pool exhausted");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            try {
                return cmd.apply(store);
            } finally {
                connections.offer(conn);
            }
        }
    }

    // ──────────────────────────────────────────────
    // Pub/Sub
    // ──────────────────────────────────────────────

    @FunctionalInterface
    interface MessageListener {
        void onMessage(String channel, String message);
    }

    static final class RedisPubSub {
        private final ConcurrentHashMap<String, List<MessageListener>> subscribers = new ConcurrentHashMap<>();
        private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        public void subscribe(String channel, MessageListener listener) {
            subscribers.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(listener);
        }

        public void unsubscribe(String channel, MessageListener listener) {
            var list = subscribers.get(channel);
            if (list != null) list.remove(listener);
        }

        public long publish(String channel, String message) {
            var list = subscribers.get(channel);
            if (list == null) return 0;
            for (var listener : list) {
                executor.submit(() -> listener.onMessage(channel, message));
            }
            return list.size();
        }

        void shutdown() {
            executor.shutdown();
        }
    }

    // ──────────────────────────────────────────────
    // Main demo
    // ──────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        System.out.println("=== Redis Integration (Simulated) ===\n");

        RedisStore store = new RedisStore();
        RedisConnectionPool pool = new RedisConnectionPool(store, 5);
        RedisPubSub pubsub = new RedisPubSub();

        // --- String commands ---
        System.out.println("--- String Commands ---");
        store.set("name", "Alice");
        store.setex("session", 10, "token-xyz");
        System.out.println("  SET name = Alice");
        System.out.println("  SETEX session = token-xyz (TTL=10s)");

        store.get("name").ifPresentOrElse(
            v -> System.out.println("  GET name -> " + v),
            () -> System.out.println("  GET name -> nil")
        );

        store.get("nonexistent").ifPresentOrElse(
            v -> System.out.println("  GET nonexistent -> " + v),
            () -> System.out.println("  GET nonexistent -> nil")
        );

        System.out.println("  TTL session: " + store.ttl("session") + "s remaining");
        store.expire("name", 60);
        System.out.println("  EXPIRE name 60 -> OK");

        store.incr("counter");
        store.incr("counter");
        store.incr("counter");
        System.out.println("  INCR counter x3 -> " + store.get("counter").orElse("nil"));

        // --- Set commands ---
        System.out.println("\n--- Set Commands ---");
        store.sadd("users:online", "alice", "bob", "charlie");
        store.sadd("users:online", "alice");
        System.out.println("  SMEMBERS users:online -> " + store.smembers("users:online"));

        // --- List commands ---
        System.out.println("\n--- List Commands ---");
        store.lpush("queue:tasks", "task1", "task2", "task3");
        System.out.println("  RPOP queue:tasks -> " + store.rpop("queue:tasks").orElse("nil"));

        // --- Connection pool ---
        System.out.println("\n--- Connection Pool ---");
        String result = pool.execute(s -> s.get("name").orElse("nil"));
        System.out.println("  Pool GET name -> " + result);

        // --- Pub/Sub ---
        System.out.println("\n--- Pub/Sub ---");
        pubsub.subscribe("news", (ch, msg) ->
            System.out.println("  [Subscriber 1] Received on '" + ch + "': " + msg));
        pubsub.subscribe("news", (ch, msg) ->
            System.out.println("  [Subscriber 2] Received on '" + ch + "': " + msg));

        System.out.println("  Publishing to 'news':");
        long subs = pubsub.publish("news", "Hello, Redis Pub/Sub!");
        System.out.println("  Message sent to " + subs + " subscriber(s)");

        // --- SETNX (Distributed Lock pattern) ---
        System.out.println("\n--- SETNX (Distributed Lock) ---");
        boolean locked = store.setnx("lock:resource1", "instance-1", 10);
        System.out.println("  Acquire lock:resource1 -> " + (locked ? "OK" : "FAIL"));
        boolean locked2 = store.setnx("lock:resource1", "instance-2", 10);
        System.out.println("  Second acquire attempt -> " + (locked2 ? "OK" : "FAIL (already locked)"));
        store.del("lock:resource1");
        boolean locked3 = store.setnx("lock:resource1", "instance-3", 10);
        System.out.println("  After DEL, acquire -> " + (locked3 ? "OK" : "FAIL"));

        Thread.sleep(500);
        pubsub.shutdown();
        store.shutdown();

        System.out.println("\n=== Redis Integration Complete ===");
    }
}
