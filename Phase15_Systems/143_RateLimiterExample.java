package phase15.systems;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;

class _143_RateLimiterExample {

    // === Token Bucket ===
    public static class TokenBucket {
        private final long capacity;
        private final double refillRate; // tokens per second
        private double tokens;
        private long lastRefillNanos;

        public TokenBucket(long capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        public synchronized boolean tryConsume(int tokens) {
            refill();
            if (this.tokens >= tokens) {
                this.tokens -= tokens;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsed = (now - lastRefillNanos) / 1e9;
            tokens = Math.min(capacity, tokens + elapsed * refillRate);
            lastRefillNanos = now;
        }

        public synchronized double available() { refill(); return tokens; }
    }

    // === Sliding Window Log ===
    public static class SlidingWindowLog {
        private final long maxRequests;
        private final long windowNanos;
        private final ConcurrentLinkedDeque<Long> timestamps = new ConcurrentLinkedDeque<>();

        public SlidingWindowLog(long maxRequests, long windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowNanos = windowSeconds * 1_000_000_000L;
        }

        public synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            long cutoff = now - windowNanos;
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return true;
            }
            return false;
        }

        public synchronized int count() { return timestamps.size(); }
    }

    // === Leaky Bucket ===
    public static class LeakyBucket {
        private final long capacity;
        private final long leakRatePerSecond;
        private long water;
        private long lastLeakNanos;

        public LeakyBucket(long capacity, long leakRatePerSecond) {
            this.capacity = capacity;
            this.leakRatePerSecond = leakRatePerSecond;
            this.water = 0;
            this.lastLeakNanos = System.nanoTime();
        }

        public synchronized boolean tryAdd() {
            leak();
            if (water < capacity) {
                water++;
                return true;
            }
            return false;
        }

        private void leak() {
            long now = System.nanoTime();
            long elapsed = now - lastLeakNanos;
            long leaked = (long) (elapsed / 1e9 * leakRatePerSecond);
            if (leaked > 0) {
                water = Math.max(0, water - leaked);
                lastLeakNanos = now;
            }
        }

        public synchronized long waterLevel() { leak(); return water; }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Token Bucket ===");
        var tb = new TokenBucket(5, 2.0);
        for (int i = 0; i < 8; i++) {
            boolean ok = tb.tryConsume(1);
            System.out.println("  Request " + (i + 1) + ": " + (ok ? "ALLOWED" : "DENIED") + " (tokens: " + String.format("%.1f", tb.available()) + ")");
        }
        Thread.sleep(1500);
        System.out.println("  After 1.5s refill: tokens = " + String.format("%.1f", tb.available()));
        System.out.println("  Consume 1: " + tb.tryConsume(1));

        System.out.println("\n=== Sliding Window Log ===");
        var sw = new SlidingWindowLog(3, 1);
        for (int i = 0; i < 5; i++) {
            boolean ok = sw.tryAcquire();
            System.out.println("  Request " + (i + 1) + ": " + (ok ? "ALLOWED" : "DENIED") + " (window count: " + sw.count() + ")");
        }
        Thread.sleep(1100);
        System.out.println("  After 1.1s: " + (sw.tryAcquire() ? "ALLOWED" : "DENIED"));

        System.out.println("\n=== Leaky Bucket ===");
        var lb = new LeakyBucket(5, 3);
        for (int i = 0; i < 7; i++) {
            boolean ok = lb.tryAdd();
            System.out.println("  Request " + (i + 1) + ": " + (ok ? "ALLOWED" : "DENIED") + " (water: " + lb.waterLevel() + ")");
        }
        Thread.sleep(1000);
        System.out.println("  After 1s leak: water = " + lb.waterLevel());
        System.out.println("  Add 1: " + lb.tryAdd());

        System.out.println("\nDone.");
    }
}
