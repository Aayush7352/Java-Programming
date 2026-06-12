package phase15.systems;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/**
 * KafkaConsumerExample.java
 *
 * Simulates Kafka consumer concepts: poll loop, ConsumerRecord, Deserializer,
 * consumer groups, offset commit. Self-contained JDK-only simulation.
 */
public class KafkaConsumerExample {

    // ──────────────────────────────────────────────
    // Core Records
    // ──────────────────────────────────────────────

    record ConsumerRecord<K, V>(String topic, int partition, long offset, K key, V value, long timestamp) {}

    record TopicPartition(String topic, int partition) {
        public String toString() { return topic + "-" + partition; }
    }

    // ──────────────────────────────────────────────
    // Deserializer
    // ──────────────────────────────────────────────

    @FunctionalInterface
    interface Deserializer<T> {
        T deserialize(String topic, byte[] data);
    }

    static final class StringDeserializer implements Deserializer<String> {
        public String deserialize(String topic, byte[] data) {
            return data == null || data.length == 0 ? null : new String(data);
        }
    }

    // ──────────────────────────────────────────────
    // Simulated Broker (reuses the concept)
    // ──────────────────────────────────────────────

    static final class SimBroker {
        private final ConcurrentHashMap<TopicPartition, List<ProducerRecord>> logs = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Integer> topicPartitions = new ConcurrentHashMap<>();
        private final AtomicLong globalOffset = new AtomicLong(0);

        record ProducerRecord(byte[] key, byte[] value, long timestamp) {}

        public void createTopic(String topic, int partitions) {
            topicPartitions.put(topic, partitions);
            for (int i = 0; i < partitions; i++) {
                logs.put(new TopicPartition(topic, i), new CopyOnWriteArrayList<>());
            }
        }

        public int partitionsForTopic(String topic) {
            return topicPartitions.getOrDefault(topic, 0);
        }

        public long append(String topic, int partition, byte[] key, byte[] value) {
            var tp = new TopicPartition(topic, partition);
            var log = logs.get(tp);
            if (log == null) throw new RuntimeException("Partition not found: " + tp);
            long offset = globalOffset.getAndIncrement();
            log.add(new ProducerRecord(key, value, System.currentTimeMillis()));
            return offset;
        }

        public List<ProducerRecord> readLog(String topic, int partition) {
            return logs.getOrDefault(new TopicPartition(topic, partition), List.of());
        }
    }

    // ──────────────────────────────────────────────
    // Offset Store (simulates __consumer_offsets)
    // ──────────────────────────────────────────────

    static final class OffsetStore {
        private final ConcurrentHashMap<String, ConcurrentHashMap<TopicPartition, Long>> offsets = new ConcurrentHashMap<>();

        public void commit(String groupId, TopicPartition tp, long offset) {
            offsets.computeIfAbsent(groupId, k -> new ConcurrentHashMap<>()).put(tp, offset);
        }

        public long getCommitted(String groupId, TopicPartition tp) {
            var map = offsets.get(groupId);
            if (map == null) return -1;
            return map.getOrDefault(tp, -1L);
        }
    }

    // ──────────────────────────────────────────────
    // Kafka Consumer
    // ──────────────────────────────────────────────

    static final class KafkaConsumer<K, V> {
        private final SimBroker broker;
        private final Deserializer<K> keyDeserializer;
        private final Deserializer<V> valueDeserializer;
        private final String groupId;
        private final OffsetStore offsetStore;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final Map<TopicPartition, Long> currentPositions = new ConcurrentHashMap<>();
        private final Map<TopicPartition, Long> pendingCommits = new ConcurrentHashMap<>();
        private Set<String> subscribedTopics = Set.of();

        KafkaConsumer(SimBroker broker, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer,
                      String groupId, OffsetStore offsetStore) {
            this.broker = broker;
            this.keyDeserializer = keyDeserializer;
            this.valueDeserializer = valueDeserializer;
            this.groupId = groupId;
            this.offsetStore = offsetStore;
        }

        public void subscribe(String... topics) {
            this.subscribedTopics = Set.of(topics);
            for (var topic : topics) {
                int partitions = broker.partitionsForTopic(topic);
                for (int p = 0; p < partitions; p++) {
                    var tp = new TopicPartition(topic, p);
                    long committed = offsetStore.getCommitted(groupId, tp);
                    currentPositions.put(tp, committed < 0 ? 0 : committed);
                }
            }
        }

        public List<ConsumerRecord<K, V>> poll(Duration timeout) {
            if (!running.get()) return List.of();
            var results = new ArrayList<ConsumerRecord<K, V>>();
            long deadline = System.currentTimeMillis() + timeout.toMillis();

            for (var entry : currentPositions.entrySet()) {
                var tp = entry.getKey();
                long position = entry.getValue();
                var log = broker.readLog(tp.topic(), tp.partition());

                while ((int) position < log.size()) {
                    var record = log.get((int) position);
                    K key = keyDeserializer.deserialize(tp.topic(), record.key());
                    V value = valueDeserializer.deserialize(tp.topic(), record.value());
                    results.add(new ConsumerRecord<>(tp.topic(), tp.partition(), position, key, value, record.timestamp()));
                    position++;
                }
                currentPositions.put(tp, position);
            }

            // Simulate poll latency if no records
            if (results.isEmpty() && !timeout.isNegative() && !timeout.isZero()) {
                try { Thread.sleep(Math.min(100, timeout.toMillis())); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            return results;
        }

        public void commitSync() {
            for (var entry : currentPositions.entrySet()) {
                offsetStore.commit(groupId, entry.getKey(), entry.getValue());
            }
        }

        public void commitAsync() {
            commitSync(); // simplified
        }

        public void seek(TopicPartition tp, long offset) {
            currentPositions.put(tp, offset);
        }

        public long position(TopicPartition tp) {
            return currentPositions.getOrDefault(tp, 0L);
        }

        public Set<TopicPartition> assignment() {
            return currentPositions.keySet();
        }

        public void wakeup() {
            running.set(false);
        }

        public void close() {
            running.set(false);
            commitSync();
        }
    }

    // ──────────────────────────────────────────────
    // Consumer Group Rebalance Simulation
    // ──────────────────────────────────────────────

    static final class ConsumerGroupCoordinator {
        private final SimBroker broker;
        private final OffsetStore offsetStore;
        private final String groupId;
        private final List<KafkaConsumer<String, String>> consumers = new CopyOnWriteArrayList<>();
        private int nextId = 0;

        ConsumerGroupCoordinator(SimBroker broker, OffsetStore offsetStore, String groupId) {
            this.broker = broker;
            this.offsetStore = offsetStore;
            this.groupId = groupId;
        }

        public KafkaConsumer<String, String> createConsumer() {
            var consumer = new KafkaConsumer<>(broker, new StringDeserializer(), new StringDeserializer(),
                groupId + "-member-" + (nextId++), offsetStore);
            consumers.add(consumer);
            return consumer;
        }

        public List<KafkaConsumer<String, String>> consumers() { return List.copyOf(consumers); }
    }

    // ──────────────────────────────────────────────
    // Demo
    // ──────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        System.out.println("=== Kafka Consumer Example (Simulated) ===\n");

        SimBroker broker = new SimBroker();
        broker.createTopic("events", 3);

        // Produce some records
        for (int i = 0; i < 15; i++) {
            int partition = i % 3;
            broker.append("events", partition,
                ("key-" + i).getBytes(),
                ("{\"eventId\":" + i + ",\"type\":\"click\"}").getBytes());
        }
        System.out.println("Produced 15 records to events topic (3 partitions)\n");

        OffsetStore offsetStore = new OffsetStore();
        ConsumerGroupCoordinator coordinator = new ConsumerGroupCoordinator(broker, offsetStore, "click-processors");

        // --- Single Consumer ---
        System.out.println("--- Single Consumer Poll ---");
        var consumer1 = coordinator.createConsumer();
        consumer1.subscribe("events");

        var records = consumer1.poll(Duration.ofMillis(500));
        System.out.println("  Polled " + records.size() + " records:");
        for (var r : records.subList(0, Math.min(5, records.size()))) {
            System.out.println("    [" + r.topic() + "-" + r.partition() + "@" + r.offset() + "] " +
                "key=" + r.key() + " value=" + r.value());
        }
        consumer1.commitSync();
        System.out.println("  Committed offsets");

        // --- Seek and replay ---
        System.out.println("\n--- Seek to Beginning ---");
        var tp = new TopicPartition("events", 0);
        consumer1.seek(tp, 0);
        var replayed = consumer1.poll(Duration.ofMillis(200));
        System.out.println("  Replayed " + replayed.size() + " records from partition 0");

        // --- Multiple consumers in group ---
        System.out.println("\n--- Multiple Consumers in Group ---");
        var consumer2 = coordinator.createConsumer();
        consumer2.subscribe("events");

        var c1Records = consumer1.poll(Duration.ofMillis(200));
        var c2Records = consumer2.poll(Duration.ofMillis(200));
        System.out.println("  Consumer1 got: " + c1Records.size() + " records");
        System.out.println("  Consumer2 got: " + c2Records.size() + " records");

        // --- Offset commit verification ---
        System.out.println("\n--- Offset Tracking ---");
        for (var tp2 : consumer1.assignment()) {
            long pos = consumer1.position(tp2);
            long committed = offsetStore.getCommitted("click-processors-member-0", tp2);
            System.out.println("  " + tp2 + " position=" + pos + " committed=" + committed);
        }

        // --- Auto commit with poll ---
        System.out.println("\n--- Consumer Group Offset Consistency ---");
        var consumer3 = coordinator.createConsumer();
        consumer3.subscribe("events");
        var polled = consumer3.poll(Duration.ofMillis(300));
        System.out.println("  New consumer in group polled " + polled.size() + " records");

        // --- End-to-end with virtual threads ---
        System.out.println("\n--- Virtual Thread Consumer ---");
        var vtConsumer = new KafkaConsumer<>(broker, new StringDeserializer(), new StringDeserializer(),
            "vt-group", offsetStore);
        vtConsumer.subscribe("events");

        var vtThread = Thread.ofVirtual().start(() -> {
            for (int i = 0; i < 3 && vtConsumer.poll(Duration.ofMillis(200)).size() > 0; i++) {
                var batch = vtConsumer.poll(Duration.ofMillis(200));
                System.out.println("  VT Consumer polled " + batch.size() + " records");
                vtConsumer.commitAsync();
            }
        });
        vtThread.join(2000);
        vtConsumer.close();

        consumer1.close();
        consumer2.close();
        consumer3.close();

        System.out.println("\n=== Kafka Consumer Example Complete ===");
    }
}
