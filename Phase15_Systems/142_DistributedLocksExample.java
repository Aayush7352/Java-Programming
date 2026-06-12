package phase15.systems;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;

class _142_DistributedLocksExample {

    public static class RedisLikeStore {
        private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Long> ttl = new ConcurrentHashMap<>();

        public boolean setnx(String key, String value, long ttlSeconds) {
            var success = store.putIfAbsent(key, value) == null;
            if (success) ttl.put(key, System.currentTimeMillis() + ttlSeconds * 1000);
            return success;
        }

        public String get(String key) {
            var expiry = ttl.get(key);
            if (expiry != null && System.currentTimeMillis() > expiry) {
                store.remove(key);
                ttl.remove(key);
                return null;
            }
            return store.get(key);
        }

        public void del(String key) {
            store.remove(key);
            ttl.remove(key);
        }

        public boolean expire(String key, long ttlSeconds) {
            if (!store.containsKey(key)) return false;
            ttl.put(key, System.currentTimeMillis() + ttlSeconds * 1000);
            return true;
        }
    }

    public static class DistributedLock {
        private final RedisLikeStore redis;
        private final String lockKey;
        private final String lockValue;
        private final long defaultTtlSeconds;
        private final AtomicInteger reentrantCount = new AtomicInteger(0);
        private volatile String owner;

        public DistributedLock(RedisLikeStore redis, String lockKey, String lockValue, long defaultTtlSeconds) {
            this.redis = redis;
            this.lockKey = lockKey;
            this.lockValue = lockValue;
            this.defaultTtlSeconds = defaultTtlSeconds;
        }

        public boolean tryLock() {
            if (isHeldByCurrentThread()) {
                reentrantCount.incrementAndGet();
                return true;
            }
            boolean acquired = redis.setnx(lockKey, lockValue, defaultTtlSeconds);
            if (acquired) {
                owner = Thread.currentThread().getName();
                reentrantCount.set(1);
            }
            return acquired;
        }

        public boolean tryLock(long timeoutMs) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                if (tryLock()) return true;
                Thread.sleep(50);
            }
            return false;
        }

        public void unlock() {
            if (!isHeldByCurrentThread()) {
                throw new IllegalStateException("Lock not held by current thread");
            }
            if (reentrantCount.decrementAndGet() == 0) {
                redis.del(lockKey);
                owner = null;
            }
        }

        public boolean isHeldByCurrentThread() {
            return Thread.currentThread().getName().equals(owner) && reentrantCount.get() > 0;
        }

        public int holdCount() { return reentrantCount.get(); }

        public void refresh() {
            if (isHeldByCurrentThread()) {
                redis.expire(lockKey, defaultTtlSeconds);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        var redis = new RedisLikeStore();
        var lock = new DistributedLock(redis, "lock:resource1", "instance-1", 10);

        System.out.println("=== Distributed Lock (SETNX-based) ===\n");

        // Acquire lock
        boolean acquired = lock.tryLock();
        System.out.println("Lock acquired: " + acquired);

        // Reentrant
        if (acquired) {
            boolean reentered = lock.tryLock();
            System.out.println("Re-entrant acquired: " + reentered + " (hold count: " + lock.holdCount() + ")");

            // Another instance cannot acquire
            var lock2 = new DistributedLock(redis, "lock:resource1", "instance-2", 10);
            boolean acquired2 = lock2.tryLock();
            System.out.println("Lock2 acquired (should be false): " + acquired2);

            lock.unlock();
            System.out.println("After unlock, hold count: " + lock.holdCount());

            lock.unlock();
            System.out.println("After second unlock, hold count: " + lock.holdCount());

            acquired2 = lock2.tryLock();
            System.out.println("Lock2 acquired after full release: " + acquired2);
            lock2.unlock();
        }

        // Try with timeout
        var lock3 = new DistributedLock(redis, "lock:resource1", "instance-3", 10);
        boolean got = lock3.tryLock();
        System.out.println("\nLock3 acquired directly: " + got);
        if (got) lock3.unlock();

        // Simulate TTL expiry
        var lock4 = new DistributedLock(redis, "lock:expire-test", "instance-4", 1);
        lock4.tryLock();
        System.out.println("\nLock4 acquired with 1s TTL");
        Thread.sleep(1500);
        var lock5 = new DistributedLock(redis, "lock:expire-test", "instance-5", 10);
        boolean expired = lock5.tryLock();
        System.out.println("Lock5 acquired after TTL expiry: " + expired);
        if (expired) lock5.unlock();

        System.out.println("\nDone.");
    }
}
