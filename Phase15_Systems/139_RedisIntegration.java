package phase15.systems;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.*;
import java.util.function.BiConsumer;

class _139_RedisIntegration {

    public record RedisValue(String value, long expiryEpochMs) {}

    public static class RedisStore {
        private final ConcurrentHashMap<String, RedisValue> store = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<Subscriber>> subscriptions = new ConcurrentHashMap<>();

        public String set(String key, String value) {
            store.put(key, new RedisValue(value, Long.MAX_VALUE));
            return "OK";
        }

        public Optional<String> get(String key) {
            var rv = store.get(key);
            if (rv == null) return Optional.empty();
            if (System.currentTimeMillis() > rv.expiryEpochMs()) {
                store.remove(key);
                return Optional.empty();
            }
            return Optional.of(rv.value());
        }

        public long expire(String key, long ttlSeconds) {
            var rv = store.get(key);
            if (rv == null) return 0;
            store.put(key, new RedisValue(rv.value(), System.currentTimeMillis() + ttlSeconds * 1000));
            return 1;
        }

        public boolean setnx(String key, String value) {
            var rv = new RedisValue(value, Long.MAX_VALUE);
            return store.putIfAbsent(key, rv) == null;
        }

        public long publish(String channel, String message) {
            var subs = subscriptions.get(channel);
            if (subs == null) return 0;
            var copy = List.copyOf(subs);
            for (var s : copy) s.executor().submit(() -> s.handler().accept(channel, message));
            return copy.size();
        }

        public void subscribe(String channel, Subscriber sub) {
            subscriptions.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(sub);
        }

        public void unsubscribe(String channel, Subscriber sub) {
            var list = subscriptions.get(channel);
            if (list != null) list.remove(sub);
        }

        public long del(String key) {
            return store.remove(key) != null ? 1 : 0;
        }
    }

    public record Subscriber(ExecutorService executor, BiConsumer<String, String> handler) {}

    public static class ConnectionPool {
        private final BlockingQueue<RedisStore> pool;
        private final int maxSize;
        private final AtomicInteger created = new AtomicInteger();

        public ConnectionPool(int maxSize) {
            this.maxSize = maxSize;
            this.pool = new LinkedBlockingQueue<>();
        }

        public RedisStore borrow() throws InterruptedException {
            RedisStore conn = pool.poll();
            if (conn == null && created.get() < maxSize) {
                conn = new RedisStore();
                created.incrementAndGet();
            } else if (conn == null) {
                conn = pool.take();
            }
            return conn;
        }

        public void release(RedisStore conn) {
            pool.offer(conn);
        }

        public int available() { return pool.size(); }
    }

    public static void main(String[] args) throws Exception {
        var redis = new RedisStore();

        // SET / GET
        redis.set("name", "phase15");
        System.out.println("GET name: " + redis.get("name").orElse("nil"));

        // EXPIRE
        redis.expire("name", 1);
        System.out.println("After EXPIRE, GET: " + redis.get("name").orElse("nil"));
        Thread.sleep(1100);
        System.out.println("After 1s, GET: " + redis.get("name").orElse("nil (expired)"));

        // SETNX
        System.out.println("SETNX lock 1st: " + redis.setnx("lock:resource", "holder1"));
        System.out.println("SETNX lock 2nd: " + redis.setnx("lock:resource", "holder2"));

        // PUB/SUB
        var exec = Executors.newVirtualThreadPerTaskExecutor();
        var latch = new CountDownLatch(2);
        var sub1 = new Subscriber(exec, (ch, msg) -> {
            System.out.println("Sub1 got [" + ch + "]: " + msg);
            latch.countDown();
        });
        var sub2 = new Subscriber(exec, (ch, msg) -> {
            System.out.println("Sub2 got [" + ch + "]: " + msg);
            latch.countDown();
        });
        redis.subscribe("news", sub1);
        redis.subscribe("news", sub2);
        redis.publish("news", "hello from phase15!");
        latch.await(3, TimeUnit.SECONDS);

        // Connection pool
        var pool = new ConnectionPool(2);
        var c1 = pool.borrow();
        c1.set("poolKey", "poolVal");
        pool.release(c1);
        var c2 = pool.borrow();
        System.out.println("Pool GET poolKey: " + c2.get("poolKey").orElse("nil"));
        pool.release(c2);

        exec.shutdown();
        System.out.println("Done.");
    }
}
