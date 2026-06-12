package phase15.systems;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

class _141_KafkaConsumerExample {

    public record ConsumerRecord(String topic, int partition, long offset, String key, String value) {}

    @FunctionalInterface
    public interface Deserializer<T> {
        T deserialize(byte[] data);
    }

    public static class StringDeserializer implements Deserializer<String> {
        public String deserialize(byte[] data) { return new String(data); }
    }

    public static class KafkaBroker {
        private final Map<Integer, List<ConsumerRecord>> logs = new ConcurrentHashMap<>();
        private final Map<Integer, AtomicLong> offsets = new ConcurrentHashMap<>();
        private final AtomicLong globalOffset = new AtomicLong();

        public int append(String topic, int partition, String key, String value) {
            int offset = (int) globalOffset.getAndIncrement();
            var rec = new ConsumerRecord(topic, partition, offset, key, value);
            logs.computeIfAbsent(partition, k -> new CopyOnWriteArrayList<>()).add(rec);
            offsets.computeIfAbsent(partition, k -> new AtomicLong(0));
            return offset;
        }

        public List<ConsumerRecord> poll(int partition, long offset, int maxRecords) {
            var log = logs.get(partition);
            if (log == null || offset >= log.size()) return List.of();
            int start = (int) offset;
            int end = Math.min(start + maxRecords, log.size());
            return log.subList(start, end);
        }

        public long currentOffset(int partition) {
            var off = offsets.get(partition);
            return off == null ? 0 : off.get();
        }
    }

    public static class KafkaConsumer {
        private final KafkaBroker broker;
        private final String groupId;
        private final Map<Integer, Long> offsets = new ConcurrentHashMap<>();
        private final Random rand = new Random();

        public KafkaConsumer(KafkaBroker broker, String groupId) {
            this.broker = broker;
            this.groupId = groupId;
        }

        public List<ConsumerRecord> poll(int partition, int maxRecords) {
            long offset = offsets.getOrDefault(partition, 0L);
            var records = broker.poll(partition, offset, maxRecords);
            if (!records.isEmpty()) {
                long lastOffset = records.getLast().offset();
                offsets.put(partition, lastOffset + 1);
                System.out.println("[Consumer group=" + groupId + "] polled " + records.size()
                    + " records from partition " + partition + " (offset " + offset + "->" + (lastOffset + 1) + ")");
            }
            return records;
        }

        public void commit() {
            System.out.println("[Consumer group=" + groupId + "] committed offsets: " + offsets);
        }

        public Map<Integer, Long> offsets() { return Map.copyOf(offsets); }
        public String groupId() { return groupId; }
    }

    public static void main(String[] args) throws Exception {
        var broker = new KafkaBroker();
        var deserializer = new StringDeserializer();

        // produce some records
        for (int i = 0; i < 5; i++) {
            broker.append("orders", 0, "k" + i, "order-" + i);
        }
        for (int i = 0; i < 3; i++) {
            broker.append("orders", 1, "k" + i, "invoice-" + i);
        }

        // consumer group A
        var c1 = new KafkaConsumer(broker, "group-A");
        var c2 = new KafkaConsumer(broker, "group-A");

        // poll loop
        for (int round = 0; round < 2; round++) {
            System.out.println("\n--- Poll round " + round + " ---");
            var r1 = c1.poll(0, 3);
            r1.forEach(r -> System.out.println("  c1 read: " + r.value()));
            var r2 = c2.poll(1, 10);
            r2.forEach(r -> System.out.println("  c2 read: " + r.value()));
            c1.commit();
            c2.commit();
        }

        System.out.println("\nFinal offsets:");
        System.out.println("  c1: " + c1.offsets());
        System.out.println("  c2: " + c2.offsets());

        // New consumer re-reads from beginning
        var c3 = new KafkaConsumer(broker, "group-B");
        var fresh = c3.poll(0, 10);
        System.out.println("\nNew consumer group-B partition 0: " + fresh.size() + " records");
        fresh.forEach(r -> System.out.println("  " + r.value()));
    }
}
