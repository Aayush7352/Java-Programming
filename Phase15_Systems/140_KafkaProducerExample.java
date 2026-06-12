package phase15.systems;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

class _140_KafkaProducerExample {

    public record TopicPartition(String topic, int partition) {}

    public sealed interface Record permits ProducerRecord, RecordMetadata {}

    public record ProducerRecord(String topic, int partition, String key, String value) implements Record {}

    public record RecordMetadata(String topic, int partition, long offset, long timestamp) implements Record {}

    @FunctionalInterface
    public interface Serializer<T> {
        byte[] serialize(T data);
    }

    public static class StringSerializer implements Serializer<String> {
        public byte[] serialize(String data) { return data == null ? new byte[0] : data.getBytes(); }
    }

    public static class KafkaBroker {
        private final Map<TopicPartition, List<ProducerRecord>> logs = new ConcurrentHashMap<>();
        private final Map<TopicPartition, AtomicLong> offsets = new ConcurrentHashMap<>();
        private final int numPartitions;

        public KafkaBroker(int numPartitions) { this.numPartitions = numPartitions; }

        public int partitions() { return numPartitions; }

        public RecordMetadata append(ProducerRecord rec) {
            var tp = new TopicPartition(rec.topic(), rec.partition());
            var off = offsets.computeIfAbsent(tp, k -> new AtomicLong(0));
            var log = logs.computeIfAbsent(tp, k -> new CopyOnWriteArrayList<>());
            log.add(rec);
            long offset = off.getAndIncrement();
            return new RecordMetadata(rec.topic(), rec.partition(), offset, System.currentTimeMillis());
        }

        public List<ProducerRecord> read(String topic, int partition, long offset) {
            var tp = new TopicPartition(topic, partition);
            var log = logs.get(tp);
            if (log == null || offset >= log.size()) return List.of();
            return log.subList((int) offset, log.size());
        }

        public long currentOffset(String topic, int partition) {
            var off = offsets.get(new TopicPartition(topic, partition));
            return off == null ? 0 : off.get();
        }
    }

    public static class KafkaProducer {
        private final KafkaBroker broker;
        private final Serializer<String> serializer;
        private final String acksMode;
        private final Random rand = new Random();

        public KafkaProducer(KafkaBroker broker, Serializer<String> serializer, String acksMode) {
            this.broker = broker;
            this.serializer = serializer;
            this.acksMode = acksMode;
        }

        public RecordMetadata send(String topic, String key, String value) {
            return send(topic, null, key, value);
        }

        public RecordMetadata send(String topic, Integer partition, String key, String value) {
            int p = partition != null ? partition : Math.abs(key.hashCode()) % broker.partitions();
            var rec = new ProducerRecord(topic, p, key, value);
            serializer.serialize(rec.value());
            var meta = broker.append(rec);
            simulateAck();
            return meta;
        }

        private void simulateAck() {
            if ("all".equals(acksMode)) {
                try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    public static void main(String[] args) {
        var broker = new KafkaBroker(3);
        var serializer = new StringSerializer();
        var producer = new KafkaProducer(broker, serializer, "all");

        var metas = new ArrayList<RecordMetadata>();
        for (int i = 0; i < 10; i++) {
            var meta = producer.send("orders", "key-" + i, "order-" + i);
            metas.add(meta);
            System.out.println("Sent: " + meta);
        }

        System.out.println("\nReading from partition 0:");
        for (int i = 0; i < broker.partitions(); i++) {
            var records = broker.read("orders", 0, 0);
            for (var r : records) {
                System.out.println("  Read: " + r);
            }
        }

        System.out.println("\nCurrent offset of partition 0: " + broker.currentOffset("orders", 0));
        System.out.println("Total partitions: " + broker.partitions());
    }
}
