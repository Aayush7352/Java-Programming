package phase15.systems;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * RateLimiterExample.java
 *
 * Demonstrates three rate limiting algorithms:
 * - Token Bucket
 * - Sliding Window Log
 * - Leaky Bucket
 *
 * Self-contained JDK-only implementation.
 */
public class RateLimiterExample {

    // ═══════════════════════════════════════════════
    // 1. Token Bucket Algorithm
    // ═══════════════════════════════════════════════

    static final class TokenBucketRateLimiter {
        private final long capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillTimestamp;
        private final Lock lock = new ReentrantLock();
        private final AtomicLong allowed = new AtomicLong(0);
        private final AtomicLong denied = new AtomicLong(0);

        TokenBucketRateLimiter(long capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.tokens = capacity;
            this.lastRefillTimestamp = System.nanoTime();
        }

        public boolean tryConsume() {
            return tryConsume(1);
        }

        public boolean tryConsume(long permits) {
            lock.lock();
            try {
                refill();
                if (tokens >= permits) {
                    tokens -= permits;
                    allowed.incrementAndGet();
                    return true;
                } else {
                    denied.incrementAndGet();
                    return false;
                }
            } finally {
                lock.unlock();
            }
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillTimestamp) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillRatePerSecond);
            lastRefillTimestamp = now;
        }

        public RateLimiterStats stats() {
            return new RateLimiterStats("TokenBucket", allowed.get(), denied.get(), tokens, capacity,
                allowed.get() + denied.get());
        }
    }

    // ═══════════════════════════════════════════════
    // 2. Sliding Window Log
    // ═══════════════════════════════════════════════

    static final class SlidingWindowLogRateLimiter {
        private final long maxRequests;
        private final long windowSizeMillis;
        private final Deque<Long> timestamps = new ConcurrentLinkedDeque<>();
        private final AtomicLong allowed = new AtomicLong(0);
        private final AtomicLong denied = new AtomicLong(0);

        SlidingWindowLogRateLimiter(long maxRequests, long windowSizeMillis) {
            this.maxRequests = maxRequests;
            this.windowSizeMillis = windowSizeMillis;
        }

        public boolean tryConsume() {
            long now = System.currentTimeMillis();
            long windowStart = now - windowSizeMillis;

            // Clean old entries
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                allowed.incrementAndGet();
                return true;
            } else {
                denied.incrementAndGet();
                return false;
            }
        }

        public RateLimiterStats stats() {
            return new RateLimiterStats("SlidingWindowLog", allowed.get(), denied.get(),
                maxRequests - timestamps.size(), maxRequests, allowed.get() + denied.get());
        }
    }

    // ═══════════════════════════════════════════════
    // 3. Leaky Bucket Algorithm
    // ═══════════════════════════════════════════════

    static final class LeakyBucketRateLimiter {
        private final long capacity;
        private final long leakRatePerSecond;
        private long water;
        private long lastLeakTimestamp;
        private final Lock lock = new ReentrantLock();
        private final AtomicLong allowed = new AtomicLong(0);
        private final AtomicLong denied = new AtomicLong(0);

        LeakyBucketRateLimiter(long capacity, long leakRatePerSecond) {
            this.capacity = capacity;
            this.leakRatePerSecond = leakRatePerSecond;
            this.water = 0;
            this.lastLeakTimestamp = System.nanoTime();
        }

        public boolean tryConsume() {
            lock.lock();
            try {
                leak();
                if (water < capacity) {
                    water++;
                    allowed.incrementAndGet();
                    return true;
                } else {
                    denied.incrementAndGet();
                    return false;
                }
            } finally {
                lock.unlock();
            }
        }

        private void leak() {
            long now = System.nanoTime();
            long elapsedNanos = now - lastLeakTimestamp;
            long leaked = (elapsedNanos * leakRatePerSecond) / 1_000_000_000L;
            if (leaked > 0) {
                water = Math.max(0, water - leaked);
                lastLeakTimestamp = now;
            }
        }

        public RateLimiterStats stats() {
            return new RateLimiterStats("LeakyBucket", allowed.get(), denied.get(), capacity - water, capacity,
                allowed.get() + denied.get());
        }
    }

    // ═══════════════════════════════════════════════
    // Common stats record
    // ═══════════════════════════════════════════════

    record RateLimiterStats(String algorithm, long allowed, long denied, double currentLevel, double maxLevel, long total) {
        public double allowRate() {
            return total == 0 ? 0 : (double) allowed / total * 100;
        }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        System.out.println("=== Rate Limiter Example ===\n");

        // ─── Token Bucket Demo ───
        System.out.println("--- Token Bucket (capacity=5, refill=2/sec) ---");
        var tokenBucket = new TokenBucketRateLimiter(5, 2);

        System.out.println("  Burst of 10 requests:");
        for (int i = 0; i < 10; i++) {
            boolean allowed = tokenBucket.tryConsume();
            System.out.println("    Request " + (i + 1) + ": " + (allowed ? "ALLOWED" : "DENIED"));
        }

        System.out.println("  Waiting 2 seconds for refill...");
        Thread.sleep(2000);

        System.out.println("  After refill, 5 more requests:");
        for (int i = 0; i < 5; i++) {
            System.out.println("    Request " + (i + 1) + ": " + (tokenBucket.tryConsume() ? "ALLOWED" : "DENIED"));
        }
        System.out.println("  Stats: " + tokenBucket.stats());

        // ─── Sliding Window Log Demo ───
        System.out.println("\n--- Sliding Window Log (max=3 per 2 seconds) ---");
        var slidingWindow = new SlidingWindowLogRateLimiter(3, 2000);

        System.out.println("  Rapid 5 requests:");
        for (int i = 0; i < 5; i++) {
            boolean allowed = slidingWindow.tryConsume();
            System.out.println("    Request " + (i + 1) + ": " + (allowed ? "ALLOWED" : "DENIED"));
        }

        System.out.println("  Waiting 2.1 seconds...");
        Thread.sleep(2100);

        System.out.println("  After window reset:");
        for (int i = 0; i < 3; i++) {
            System.out.println("    Request " + (i + 1) + ": " + (slidingWindow.tryConsume() ? "ALLOWED" : "DENIED"));
        }
        System.out.println("  Stats: " + slidingWindow.stats());

        // ─── Leaky Bucket Demo ───
        System.out.println("\n--- Leaky Bucket (capacity=5, leak=1/sec) ---");
        var leakyBucket = new LeakyBucketRateLimiter(5, 1);

        System.out.println("  Burst of 8 requests:");
        for (int i = 0; i < 8; i++) {
            boolean allowed = leakyBucket.tryConsume();
            System.out.println("    Request " + (i + 1) + ": " + (allowed ? "ALLOWED" : "DENIED"));
        }

        System.out.println("  Waiting 3 seconds (leak 3)...");
        Thread.sleep(3000);

        for (int i = 0; i < 3; i++) {
            System.out.println("    Request " + (i + 1) + ": " + (leakyBucket.tryConsume() ? "ALLOWED" : "DENIED"));
        }
        System.out.println("  Stats: " + leakyBucket.stats());

        // ─── Concurrent Demo with Virtual Threads ───
        System.out.println("\n--- Concurrent Token Bucket (10 VT, 20 requests each, rate=5/sec cap=5) ---");
        var concurrentBucket = new TokenBucketRateLimiter(5, 5);
        var vtThreads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            vtThreads[i] = Thread.ofVirtual().start(() -> {
                for (int j = 0; j < 20; j++) {
                    concurrentBucket.tryConsume();
                    try { Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50)); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        for (var t : vtThreads) t.join();

        var stats = concurrentBucket.stats();
        System.out.println("  Total requests: " + stats.total());
        System.out.println("  Allowed: " + stats.allowed());
        System.out.println("  Denied: " + stats.denied());
        System.out.printf("  Allow rate: %.2f%%%n", stats.allowRate());

        // ─── Summary ───
        System.out.println("\n--- Comparison ---");
        System.out.println("  TokenBucket: " + tokenBucket.stats());
        System.out.println("  SlidingWindow: " + slidingWindow.stats());
        System.out.println("  LeakyBucket: " + leakyBucket.stats());
        System.out.println("  ConcurrentBucket: " + stats);

        System.out.println("\n=== Rate Limiter Example Complete ===");
    }
}
