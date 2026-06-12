package phase15.systems;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * KafkaProducerExample.java
 *
 * Simulates Kafka producer concepts: ProducerRecord, Serializer, Partitioner,
 * acks, retries. Self-contained JDK-only simulation.
 */
public class KafkaProducerExample {

    // ──────────────────────────────────────────────
    // Core Records
    // ──────────────────────────────────────────────

    record ProducerRecord<K, V>(String topic, Integer partition, K key, V value, long timestamp) {
        ProducerRecord(String topic, K key, V value) {
            this(topic, null, key, value, System.currentTimeMillis());
        }
        ProducerRecord(String topic, V value) {
            this(topic, null, null, value, System.currentTimeMillis());
        }
    }

    record RecordMetadata(String topic, int partition, long offset, long timestamp) {}

    // ──────────────────────────────────────────────
    // Serializer
    // ──────────────────────────────────────────────

    @FunctionalInterface
    interface Serializer<T> {
        byte[] serialize(String topic, T data);
    }

    static final class StringSerializer implements Serializer<String> {
        public byte[] serialize(String topic, String data) {
            return data == null ? new byte[0] : data.getBytes();
        }
    }

    static final class ByteArraySerializer implements Serializer<byte[]> {
        public byte[] serialize(String topic, byte[] data) {
            return data == null ? new byte[0] : data.clone();
        }
    }

    // ──────────────────────────────────────────────
    // Partitioner
    // ──────────────────────────────────────────────

    @FunctionalInterface
    interface Partitioner {
        int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, int numPartitions);
    }

    static final class DefaultPartitioner implements Partitioner {
        public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, int numPartitions) {
            if (keyBytes != null && keyBytes.length > 0) {
                return Math.abs(Arrays.hashCode(keyBytes)) % numPartitions;
            }
            return ThreadLocalRandom.current().nextInt(numPartitions);
        }
    }

    static final class RoundRobinPartitioner implements Partitioner {
        private final AtomicInteger counter = new AtomicInteger(0);

        public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, int numPartitions) {
            return Math.abs(counter.getAndIncrement()) % numPartitions;
        }
    }

    // ──────────────────────────────────────────────
    // Producer Config
    // ──────────────────────────────────────────────

    static final class ProducerConfig {
        static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
        static final String ACKS_CONFIG = "acks";
        static final String RETRIES_CONFIG = "retries";
        static final String BATCH_SIZE_CONFIG = "batch.size";
        static final String LINGER_MS_CONFIG = "linger.ms";
        static final String MAX_IN_FLIGHT = "max.in.flight.requests.per.connection";
    }

    // ──────────────────────────────────────────────
    // Simulated Broker (Topic-Partition storage)
    // ──────────────────────────────────────────────

    static final class TopicPartition {
        final String topic;
        final int partition;

        TopicPartition(String topic, int partition) {
            this.topic = topic;
            this.partition = partition;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TopicPartition tp && tp.topic.equals(topic) && tp.partition == partition;
        }

        @Override
        public int hashCode() { return Objects.hash(topic, partition); }

        public String toString() { return topic + "-" + partition; }
    }

    static final class SimulatedBroker {
        private final ConcurrentHashMap<TopicPartition, List<ProducerRecord<byte[], byte[]>>> logs = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Integer> topicPartitions = new ConcurrentHashMap<>();
        private final AtomicLong globalOffset = new AtomicLong(0);

        public void createTopic(String topic, int partitions) {
            topicPartitions.put(topic, partitions);
            for (int i = 0; i < partitions; i++) {
                logs.put(new TopicPartition(topic, i), new CopyOnWriteArrayList<>());
            }
        }

        public int partitionsForTopic(String topic) {
            return topicPartitions.getOrDefault(topic, 0);
        }

        public synchronized RecordMetadata append(String topic, int partition, ProducerRecord<byte[], byte[]> record) {
            var tp = new TopicPartition(topic, partition);
            var log = logs.get(tp);
            if (log == null) throw new RuntimeException("Topic/partition not found: " + tp);
            long offset = globalOffset.getAndIncrement();
            log.add(record);
            return new RecordMetadata(topic, partition, offset, record.timestamp());
        }

        public List<ProducerRecord<byte[], byte[]>> readLog(String topic, int partition) {
            return logs.getOrDefault(new TopicPartition(topic, partition), List.of());
        }
    }

    // ──────────────────────────────────────────────
    // Kafka Producer
    // ──────────────────────────────────────────────

    static final class KafkaProducer<K, V> {
        private final SimulatedBroker broker;
        private final Serializer<K> keySerializer;
        private final Serializer<V> valueSerializer;
        private final Partitioner partitioner;
        private final String acks;
        private final int retries;
        private final AtomicLong recordsSent = new AtomicLong(0);
        private final AtomicLong recordsFailed = new AtomicLong(0);

        KafkaProducer(SimulatedBroker broker, Serializer<K> keySerializer, Serializer<V> valueSerializer, Map<String, Object> config) {
            this.broker = broker;
            this.keySerializer = keySerializer;
            this.valueSerializer = valueSerializer;
            this.partitioner = new DefaultPartitioner();
            this.acks = (String) config.getOrDefault("acks", "all");
            this.retries = (int) config.getOrDefault("retries", 3);
        }

        public RecordMetadata send(ProducerRecord<K, V> record) {
            int numPartitions = broker.partitionsForTopic(record.topic());
            if (numPartitions == 0) throw new RuntimeException("Topic not found: " + record.topic());

            int partition = record.partition() != null
                ? record.partition() % numPartitions
                : partitioner.partition(record.topic(), record.key(),
                    keySerializer.serialize(record.topic(), record.key()),
                    record.value(), valueSerializer.serialize(record.topic(), record.value()),
                    numPartitions);

            byte[] keyBytes = keySerializer.serialize(record.topic(), record.key());
            byte[] valBytes = valueSerializer.serialize(record.topic(), record.value());

            var wireRecord = new ProducerRecord<>(record.topic(), partition, keyBytes, valBytes, record.timestamp());

            // Retry loop
            int attempts = 0;
            while (attempts <= retries) {
                try {
                    RecordMetadata meta = broker.append(record.topic(), partition, wireRecord);
                    recordsSent.incrementAndGet();
                    return meta;
                } catch (Exception e) {
                    attempts++;
                    if (attempts > retries) {
                        recordsFailed.incrementAndGet();
                        throw new RuntimeException("Failed after " + retries + " retries: " + e.getMessage());
                    }
                    try { Thread.sleep(50L * attempts); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
            throw new RuntimeException("Unreachable");
        }

        public long getRecordsSent() { return recordsSent.get(); }
        public long getRecordsFailed() { return recordsFailed.get(); }
        public void flush() { /* no-op in sim */ }
        public void close() { /* no-op */ }
    }

    // ──────────────────────────────────────────────
    // Demo
    // ──────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        System.out.println("=== Kafka Producer Example (Simulated) ===\n");

        SimulatedBroker broker = new SimulatedBroker();
        broker.createTopic("orders", 3);
        broker.createTopic("notifications", 2);

        var config = new HashMap<String, Object>();
        config.put("acks", "all");
        config.put("retries", 3);

        var producer = new KafkaProducer<>(broker, new StringSerializer(), new StringSerializer(), config);

        System.out.println("--- Sending Records ---");
        var meta1 = producer.send(new ProducerRecord<>("orders", "order-1", "{\"id\":1,\"item\":\"laptop\"}"));
        System.out.println("  Sent: orders/partition=" + meta1.partition() + "/offset=" + meta1.offset());

        var meta2 = producer.send(new ProducerRecord<>("orders", "order-2", "{\"id\":2,\"item\":\"mouse\"}"));
        System.out.println("  Sent: orders/partition=" + meta2.partition() + "/offset=" + meta2.offset());

        // Send records with keys to same partition
        Map<String, RecordMetadata> keyed = new LinkedHashMap<>();
        for (int i = 0; i < 5; i++) {
            var meta = producer.send(new ProducerRecord<>("orders", "user:" + (i % 3), "{\"id\":" + (i + 10) + "}"));
            keyed.put("user:" + (i % 3), meta);
        }
        System.out.println("\n  Same key -> same partition:");
        keyed.forEach((k, m) -> System.out.println("    " + k + " -> partition=" + m.partition()));

        System.out.println("\n--- Partition Distribution ---");
        for (int p = 0; p < 3; p++) {
            var log = broker.readLog("orders", p);
            System.out.println("  Partition " + p + ": " + log.size() + " messages");
        }

        System.out.println("\n--- Producer Metrics ---");
        System.out.println("  Records Sent: " + producer.getRecordsSent());
        System.out.println("  Records Failed: " + producer.getRecordsFailed());

        // Async sends with virtual threads
        System.out.println("\n--- Async Sends (Virtual Threads) ---");
        var vtProducer = new KafkaProducer<>(broker, new StringSerializer(), new StringSerializer(), config);
        var futures = new java.util.ArrayList<CompletableFuture<RecordMetadata>>();
        for (int i = 0; i < 20; i++) {
            int id = i;
            var f = CompletableFuture.supplyAsync(() ->
                vtProducer.send(new ProducerRecord<>("notifications", "msg-" + id, "content-" + id)),
                Executors.newVirtualThreadPerTaskExecutor()
            );
            futures.add(f);
        }
        for (var f : futures) {
            var meta = f.get();
            System.out.println("  Sent: " + meta.topic() + "/p" + meta.partition() + "/off" + meta.offset());
        }

        producer.close();
        vtProducer.close();

        System.out.println("\n=== Kafka Producer Example Complete ===");
    }
}
