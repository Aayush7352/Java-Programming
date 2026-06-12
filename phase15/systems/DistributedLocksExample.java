package phase15.systems;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * DistributedLocksExample.java
 *
 * Simulates distributed locks using Redis SETNX concept and ZooKeeper
 * ephemeral node concept. Includes a reentrant distributed lock.
 *
 * Self-contained JDK-only simulation.
 */
public class DistributedLocksExample {

    // ──────────────────────────────────────────────
    // Lock Storage (simulates Redis / ZooKeeper)
    // ──────────────────────────────────────────────

    static final class LockStore {
        private final ConcurrentHashMap<String, LockRecord> locks = new ConcurrentHashMap<>();

        record LockRecord(String ownerId, String lockId, int depth, long expiryEpochMs) {
            boolean isExpired() { return expiryEpochMs > 0 && System.currentTimeMillis() > expiryEpochMs; }
        }

        // SETNX-style: acquire if not exists, with TTL
        public boolean tryAcquire(String resourceId, String ownerId, long ttlMillis) {
            long expiry = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : Long.MAX_VALUE;
            var newRecord = new LockRecord(ownerId, resourceId, 1, expiry);
            var existing = locks.putIfAbsent(resourceId, newRecord);
            if (existing == null) return true;
            if (existing.isExpired()) {
                boolean replaced = locks.replace(resourceId, existing, newRecord);
                if (replaced) return true;
            }
            return false;
        }

        // Reentrant acquire
        public boolean tryAcquireReentrant(String resourceId, String ownerId, long ttlMillis) {
            long expiry = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : Long.MAX_VALUE;
            var existing = locks.get(resourceId);
            if (existing != null && existing.ownerId().equals(ownerId) && !existing.isExpired()) {
                locks.put(resourceId, new LockRecord(ownerId, resourceId, existing.depth() + 1, expiry));
                return true;
            }
            return tryAcquire(resourceId, ownerId, ttlMillis);
        }

        // Release
        public boolean release(String resourceId, String ownerId) {
            var existing = locks.get(resourceId);
            if (existing == null) return false;
            if (!existing.ownerId().equals(ownerId)) return false;

            if (existing.depth() > 1) {
                locks.put(resourceId, new LockRecord(ownerId, resourceId, existing.depth() - 1, existing.expiryEpochMs()));
                return true;
            }
            return locks.remove(resourceId, existing);
        }

        // ZooKeeper-style: ephemeral sequential node
        public String createEphemeralSequential(String resourceId, String ownerId) {
            String nodePath = resourceId + "/lock-" + ownerId + "-" + System.nanoTime();
            var record = new LockRecord(ownerId, nodePath, 1, Long.MAX_VALUE);
            locks.put(nodePath, record);
            return nodePath;
        }

        public List<String> getChildren(String resourceId) {
            return locks.keySet().stream()
                .filter(k -> k.startsWith(resourceId + "/"))
                .sorted()
                .toList();
        }

        public boolean deleteNode(String nodePath) {
            return locks.remove(nodePath) != null;
        }

        public boolean isHeld(String resourceId, String ownerId) {
            var existing = locks.get(resourceId);
            return existing != null && existing.ownerId().equals(ownerId) && !existing.isExpired();
        }
    }

    // ──────────────────────────────────────────────
    // Distributed Lock (Redis-style)
    // ──────────────────────────────────────────────

    static final class DistributedLock implements AutoCloseable {
        private final String resourceId;
        private final String ownerId;
        private final LockStore store;
        private final long ttlMillis;
        private volatile boolean acquired = false;

        DistributedLock(LockStore store, String resourceId, String ownerId, long ttlMillis) {
            this.store = store;
            this.resourceId = resourceId;
            this.ownerId = ownerId;
            this.ttlMillis = ttlMillis;
        }

        public boolean tryLock() {
            acquired = store.tryAcquire(resourceId, ownerId, ttlMillis);
            return acquired;
        }

        public boolean tryLock(long timeoutMillis) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMillis;
            while (!acquired && System.currentTimeMillis() < deadline) {
                acquired = store.tryAcquire(resourceId, ownerId, ttlMillis);
                if (!acquired) Thread.sleep(50);
            }
            return acquired;
        }

        public void lock() {
            while (!tryLock()) {
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }

        public void unlock() {
            if (acquired) {
                store.release(resourceId, ownerId);
                acquired = false;
            }
        }

        public boolean isHeld() { return acquired; }

        @Override
        public void close() { unlock(); }
    }

    // ──────────────────────────────────────────────
    // Reentrant Distributed Lock
    // ──────────────────────────────────────────────

    static final class ReentrantDistributedLock implements AutoCloseable {
        private final String resourceId;
        private final String ownerId;
        private final LockStore store;
        private final long ttlMillis;
        private final AtomicInteger holdCount = new AtomicInteger(0);

        ReentrantDistributedLock(LockStore store, String resourceId, String ownerId, long ttlMillis) {
            this.store = store;
            this.resourceId = resourceId;
            this.ownerId = ownerId;
            this.ttlMillis = ttlMillis;
        }

        public boolean tryLock() {
            boolean acquired = store.tryAcquireReentrant(resourceId, ownerId, ttlMillis);
            if (acquired) holdCount.incrementAndGet();
            return acquired;
        }

        public void lock() {
            while (!tryLock()) {
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }

        public void unlock() {
            if (holdCount.get() > 0) {
                store.release(resourceId, ownerId);
                holdCount.decrementAndGet();
            }
        }

        public int getHoldCount() { return holdCount.get(); }

        @Override
        public void close() {
            while (holdCount.get() > 0) unlock();
        }
    }

    // ──────────────────────────────────────────────
    // ZooKeeper-style Distributed Lock
    // ──────────────────────────────────────────────

    static final class ZKDistributedLock implements AutoCloseable {
        private final LockStore store;
        private final String resourcePath;
        private final String ownerId;
        private String nodePath;

        ZKDistributedLock(LockStore store, String resourcePath, String ownerId) {
            this.store = store;
            this.resourcePath = resourcePath;
            this.ownerId = ownerId;
        }

        public boolean tryLock() {
            nodePath = store.createEphemeralSequential(resourcePath, ownerId);
            var children = store.getChildren(resourcePath);
            return children.getFirst().equals(nodePath);
        }

        public void lock() {
            while (!tryLock()) {
                try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            }
        }

        public void unlock() {
            if (nodePath != null) {
                store.deleteNode(nodePath);
                nodePath = null;
            }
        }

        @Override
        public void close() { unlock(); }
    }

    // ──────────────────────────────────────────────
    // Shared Resource (for testing)
    // ──────────────────────────────────────────────

    static final class SharedCounter {
        private int count = 0;
        public void increment() { count++; }
        public int getCount() { return count; }
    }

    // ──────────────────────────────────────────────
    // Demo
    // ──────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        System.out.println("=== Distributed Locks Example ===\n");

        LockStore store = new LockStore();

        // --- Basic SETNX Lock ---
        System.out.println("--- Basic Distributed Lock (SETNX) ---");
        var lock1 = new DistributedLock(store, "resource:db-write", "instance-1", 5000);
        var lock2 = new DistributedLock(store, "resource:db-write", "instance-2", 5000);

        System.out.println("  Instance-1 tryLock: " + lock1.tryLock());
        System.out.println("  Instance-2 tryLock (should fail): " + lock2.tryLock());
        lock1.unlock();
        System.out.println("  Instance-1 unlocked");
        System.out.println("  Instance-2 tryLock (should succeed): " + lock2.tryLock());
        lock2.unlock();

        // --- Reentrant Lock ---
        System.out.println("\n--- Reentrant Distributed Lock ---");
        var reLock = new ReentrantDistributedLock(store, "resource:reentrant", "instance-1", 5000);
        System.out.println("  Acquire #1: " + reLock.tryLock() + " (hold=" + reLock.getHoldCount() + ")");
        System.out.println("  Acquire #2 (reentrant): " + reLock.tryLock() + " (hold=" + reLock.getHoldCount() + ")");
        System.out.println("  Acquire #3 (reentrant): " + reLock.tryLock() + " (hold=" + reLock.getHoldCount() + ")");
        reLock.unlock();
        System.out.println("  Release #1 (hold=" + reLock.getHoldCount() + ")");
        reLock.unlock();
        System.out.println("  Release #2 (hold=" + reLock.getHoldCount() + ")");
        reLock.unlock();
        System.out.println("  Release #3 (hold=" + reLock.getHoldCount() + ")");
        System.out.println("  Lock fully released: " + !store.isHeld("resource:reentrant", "instance-1"));

        // --- ZooKeeper-style Lock ---
        System.out.println("\n--- ZooKeeper Ephemeral Sequential Lock ---");
        var zkLock1 = new ZKDistributedLock(store, "/locks/myresource", "instance-1");
        var zkLock2 = new ZKDistributedLock(store, "/locks/myresource", "instance-2");

        System.out.println("  ZK Instance-1 tryLock: " + zkLock1.tryLock());
        System.out.println("  ZK Instance-2 tryLock (should fail): " + zkLock2.tryLock());
        zkLock1.unlock();
        System.out.println("  ZK Instance-1 unlocked");
        System.out.println("  ZK Instance-2 tryLock (should succeed): " + zkLock2.tryLock());
        zkLock2.unlock();

        // --- Concurrent Access with Locks ---
        System.out.println("\n--- Concurrent Protection ---");
        SharedCounter counter = new SharedCounter();
        var threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            String id = "worker-" + i;
            threads[i] = Thread.ofVirtual().start(() -> {
                for (int j = 0; j < 100; j++) {
                    var lock = new DistributedLock(store, "resource:counter", id, 1000);
                    lock.lock();
                    try {
                        counter.increment();
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }
        for (var t : threads) t.join();
        System.out.println("  Counter after 10 virtual threads x 100 increments: " + counter.getCount() + " (expected 1000)");

        // --- TTL-based lock expiration ---
        System.out.println("\n--- TTL Expiration ---");
        var ttlLock = new DistributedLock(store, "resource:ttl-test", "instance-1", 500);
        System.out.println("  Acquire lock with 500ms TTL: " + ttlLock.tryLock());
        System.out.println("  Is held: " + store.isHeld("resource:ttl-test", "instance-1"));
        Thread.sleep(600);
        System.out.println("  After 600ms, is held: " + store.isHeld("resource:ttl-test", "instance-1") + " (expired)");
        // Now another can acquire
        System.out.println("  Instance-2 acquires after expiry: " +
            new DistributedLock(store, "resource:ttl-test", "instance-2", 5000).tryLock());

        // --- Wait lock with timeout ---
        System.out.println("\n--- Try Lock with Timeout ---");
        var heldLock = new DistributedLock(store, "resource:timeout", "instance-1", 5000);
        heldLock.lock();
        System.out.println("  Instance-1 holds lock");

        long start = System.nanoTime();
        var timeoutLock = new DistributedLock(store, "resource:timeout", "instance-2", 5000);
        boolean acquired = timeoutLock.tryLock(300); // wait 300ms
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        System.out.println("  Instance-2 tryLock(300ms): " + acquired + " (waited " + elapsed + "ms)");
        heldLock.unlock();

        // Re-acquire after release
        boolean acquired2 = timeoutLock.tryLock(300);
        System.out.println("  After release, Instance-2 tryLock: " + acquired2);
        timeoutLock.unlock();

        System.out.println("\n=== Distributed Locks Example Complete ===");
    }
}
