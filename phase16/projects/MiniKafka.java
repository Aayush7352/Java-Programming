package phase16.projects;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/**
 * MiniKafka.java
 *
 * Mini Kafka: Topic, Partition, Producer sends to partition, Consumer group
 * with offset tracking, basic pub-sub messaging system.
 */
public class MiniKafka {

    // ═══════════════════════════════════════════════
    // Core Records
    // ═══════════════════════════════════════════════

    record Message(String key, String value, long offset, Instant timestamp) {}

    record TopicPartition(String topic, int partition) {
        public String toString() { return topic + "-" + partition; }
    }

    record ProducerRecord(String topic, String key, String value) {}

    record ConsumerRecord(String topic, int partition, long offset, String key, String value, Instant timestamp) {}

    enum OffsetResetStrategy { EARLIEST, LATEST }

    // ═══════════════════════════════════════════════
    // Topic & Partition
    // ═══════════════════════════════════════════════

    static final class Partition {
        private final int partitionId;
        private final CopyOnWriteArrayList<Message> messages = new CopyOnWriteArrayList<>();
        private final AtomicLong offsetCounter = new AtomicLong(0);

        Partition(int partitionId) { this.partitionId = partitionId; }

        public long append(String key, String value) {
            long offset = offsetCounter.getAndIncrement();
            messages.add(new Message(key, value, offset, Instant.now()));
            return offset;
        }

        public Optional<Message> read(long offset) {
            if (offset < 0 || offset >= messages.size()) return Optional.empty();
            return Optional.of(messages.get((int) offset));
        }

        public List<Message> readRange(long startOffset, int maxRecords) {
            if (startOffset >= messages.size()) return List.of();
            int from = (int) Math.max(0, startOffset);
            int to = (int) Math.min(messages.size(), from + maxRecords);
            return new ArrayList<>(messages.subList(from, to));
        }

        public long getLatestOffset() { return messages.size(); }
        public int getMessageCount() { return messages.size(); }
        public int getPartitionId() { return partitionId; }
    }

    static final class Topic {
        private final String name;
        private final List<Partition> partitions;
        private final int replicationFactor;

        Topic(String name, int numPartitions) {
            this(name, numPartitions, 1);
        }

        Topic(String name, int numPartitions, int replicationFactor) {
            this.name = name;
            this.replicationFactor = replicationFactor;
            this.partitions = new ArrayList<>();
            for (int i = 0; i < numPartitions; i++) {
                partitions.add(new Partition(i));
            }
        }

        public String getName() { return name; }
        public int getNumPartitions() { return partitions.size(); }
        public Partition getPartition(int id) { return partitions.get(id); }
        public List<Partition> getAllPartitions() { return List.copyOf(partitions); }
        public int getReplicationFactor() { return replicationFactor; }

        public long append(int partitionId, String key, String value) {
            return partitions.get(partitionId).append(key, value);
        }

        public long append(String key, String value) {
            int partition = Math.abs(key.hashCode()) % partitions.size();
            return partitions.get(partition).append(key, value);
        }

        public int getTotalMessages() {
            return partitions.stream().mapToInt(Partition::getMessageCount).sum();
        }
    }

    // ═══════════════════════════════════════════════
    // Offset Store
    // ═══════════════════════════════════════════════

    static final class OffsetStore {
        private final ConcurrentHashMap<String, ConcurrentHashMap<TopicPartition, Long>> groupOffsets = new ConcurrentHashMap<>();

        public void commit(String groupId, TopicPartition tp, long offset) {
            groupOffsets.computeIfAbsent(groupId, k -> new ConcurrentHashMap<>()).put(tp, offset);
        }

        public long getCommitted(String groupId, TopicPartition tp) {
            var map = groupOffsets.get(groupId);
            return map == null ? -1 : map.getOrDefault(tp, -1L);
        }

        public Map<TopicPartition, Long> getAllOffsets(String groupId) {
            var map = groupOffsets.get(groupId);
            return map == null ? Map.of() : new HashMap<>(map);
        }
    }

    // ═══════════════════════════════════════════════
    // Mini Kafka Broker
    // ═══════════════════════════════════════════════

    static final class MiniKafkaBroker {
        private final ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();
        private final OffsetStore offsetStore = new OffsetStore();

        public Topic createTopic(String name, int partitions) {
            var topic = new Topic(name, partitions);
            topics.put(name, topic);
            return topic;
        }

        public Topic createTopic(String name, int partitions, int replicationFactor) {
            var topic = new Topic(name, partitions, replicationFactor);
            topics.put(name, topic);
            return topic;
        }

        public Optional<Topic> getTopic(String name) {
            return Optional.ofNullable(topics.get(name));
        }

        public List<String> listTopics() {
            return List.copyOf(topics.keySet());
        }

        // Producer API
        public long produce(String topicName, String key, String value) {
            var topic = getTopic(topicName)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicName));
            return topic.append(key, value);
        }

        public long produceToPartition(String topicName, int partition, String key, String value) {
            var topic = getTopic(topicName)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicName));
            return topic.append(partition, key, value);
        }

        // Consumer API
        public List<ConsumerRecord> poll(String groupId, String topicName, int maxRecords) {
            var topic = getTopic(topicName)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicName));
            var results = new ArrayList<ConsumerRecord>();

            for (int p = 0; p < topic.getNumPartitions(); p++) {
                var tp = new TopicPartition(topicName, p);
                long committed = offsetStore.getCommitted(groupId, tp);
                long startOffset = committed < 0 ? 0 : committed;

                var partition = topic.getPartition(p);
                var messages = partition.readRange(startOffset, maxRecords);
                for (var msg : messages) {
                    results.add(new ConsumerRecord(topicName, p, msg.offset(), msg.key(), msg.value(), msg.timestamp()));
                }
            }

            results.sort(Comparator.comparingLong(ConsumerRecord::offset));
            return results.subList(0, Math.min(results.size(), maxRecords));
        }

        public void commitOffset(String groupId, String topicName, int partition, long offset) {
            offsetStore.commit(groupId, new TopicPartition(topicName, partition), offset + 1);
        }

        public OffsetStore getOffsetStore() { return offsetStore; }
    }

    // ═══════════════════════════════════════════════
    // Producer
    // ═══════════════════════════════════════════════

    static final class Producer {
        private final MiniKafkaBroker broker;
        private final AtomicLong messagesSent = new AtomicLong(0);

        Producer(MiniKafkaBroker broker) { this.broker = broker; }

        public long send(String topic, String key, String value) {
            messagesSent.incrementAndGet();
            return broker.produce(topic, key, value);
        }

        public long sendToPartition(String topic, int partition, String key, String value) {
            messagesSent.incrementAndGet();
            return broker.produceToPartition(topic, partition, key, value);
        }

        public long getMessagesSent() { return messagesSent.get(); }
    }

    // ═══════════════════════════════════════════════
    // Consumer
    // ═══════════════════════════════════════════════

    static final class Consumer {
        private final MiniKafkaBroker broker;
        private final String groupId;
        private final AtomicLong messagesConsumed = new AtomicLong(0);

        Consumer(MiniKafkaBroker broker, String groupId) {
            this.broker = broker;
            this.groupId = groupId;
        }

        public List<ConsumerRecord> poll(String topic, int maxRecords) {
            var records = broker.poll(groupId, topic, maxRecords);
            messagesConsumed.addAndGet(records.size());

            // Auto-commit
            for (var r : records) {
                broker.commitOffset(groupId, r.topic(), r.partition(), r.offset());
            }
            return records;
        }

        public long getMessagesConsumed() { return messagesConsumed.get(); }
        public String getGroupId() { return groupId; }
    }

    // ═══════════════════════════════════════════════
    // Consumer Group
    // ═══════════════════════════════════════════════

    static final class ConsumerGroup {
        private final String groupId;
        private final List<Consumer> consumers = new CopyOnWriteArrayList<>();

        ConsumerGroup(String groupId) { this.groupId = groupId; }

        public Consumer createConsumer(MiniKafkaBroker broker) {
            var consumer = new Consumer(broker, groupId);
            consumers.add(consumer);
            return consumer;
        }

        public List<Consumer> getConsumers() { return List.copyOf(consumers); }
        public String getGroupId() { return groupId; }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        System.out.println("=== Mini Kafka ===\n");

        MiniKafkaBroker broker = new MiniKafkaBroker();

        // ─── Create Topics ───
        System.out.println("--- Topics ---");
        broker.createTopic("orders", 3);
        broker.createTopic("notifications", 2);
        broker.createTopic("events", 4);
        System.out.println("  Topics: " + broker.listTopics());
        System.out.println("  orders: 3 partitions");
        System.out.println("  notifications: 2 partitions");
        System.out.println("  events: 4 partitions");

        // ─── Produce Messages ───
        System.out.println("\n--- Producing ---");
        Producer producer = new Producer(broker);

        // Produce to orders topic
        for (int i = 0; i < 10; i++) {
            long offset = producer.send("orders", "key-" + i, "order-" + i + ":{\"item\":\"product" + i % 3 + "\"}");
            if (i == 0) System.out.println("  First order offset: " + offset);
        }
        System.out.println("  Sent 10 messages to 'orders'");

        // Produce with specific partition
        producer.sendToPartition("orders", 0, "direct-key", "direct-value");
        System.out.println("  Sent 1 message to partition 0");

        // Produce to notifications
        for (int i = 0; i < 5; i++) {
            producer.send("notifications", "notif-" + i, "{\"type\":\"alert\",\"msg\":\"Notification " + i + "\"}");
        }
        System.out.println("  Sent 5 messages to 'notifications'");

        System.out.println("  Total produced: " + producer.getMessagesSent());

        // ─── Consume Messages ───
        System.out.println("\n--- Consuming (Group: order-processors) ---");
        var group = new ConsumerGroup("order-processors");
        var consumer1 = group.createConsumer(broker);
        var consumer2 = group.createConsumer(broker);

        var records1 = consumer1.poll("orders", 5);
        System.out.println("  Consumer1 consumed " + records1.size() + " records:");
        for (var r : records1) {
            System.out.printf("    [%s-%d@%d] %s: %s%n", r.topic(), r.partition(), r.offset(), r.key(), r.value());
        }

        var records2 = consumer1.poll("orders", 10);
        System.out.println("  Consumer1 poll 2: " + records2.size() + " more records");

        // ─── Consumer Group Offset Tracking ───
        System.out.println("\n--- Offset Tracking ---");
        var offsets = broker.getOffsetStore().getAllOffsets("order-processors");
        for (var entry : offsets.entrySet()) {
            System.out.println("  " + entry.getKey() + " committed offset: " + entry.getValue());
        }

        // ─── New consumer in same group starts from committed ───
        System.out.println("\n--- New Consumer Same Group ---");
        var consumer3 = group.createConsumer(broker);
        var records3 = consumer3.poll("orders", 100);
        System.out.println("  Consumer3 (same group) polled: " + records3.size() + " records (should be rest from committed)");
        for (var r : records3) {
            System.out.printf("    [%s-%d@%d] %s%n", r.topic(), r.partition(), r.offset(), r.value());
        }

        // ─── Notifications Consumer ───
        System.out.println("\n--- Notifications Consumer ---");
        var notifConsumer = new Consumer(broker, "notif-service");
        var notifs = notifConsumer.poll("notifications", 10);
        System.out.println("  Consumed " + notifs.size() + " notifications:");
        for (var n : notifs) {
            System.out.println("    " + n.value());
        }

        // ─── Virtual Thread Producers and Consumers ───
        System.out.println("\n--- Virtual Threads (Concurrent) ---");
        broker.createTopic("stream", 6);
        var vtProducer = new Producer(broker);
        var vtConsumer = new Consumer(broker, "stream-group");
        var produceCount = new AtomicInteger(0);

        var producers = new Thread[6];
        for (int i = 0; i < 6; i++) {
            int id = i;
            producers[i] = Thread.ofVirtual().start(() -> {
                for (int j = 0; j < 50; j++) {
                    vtProducer.send("stream", "p" + id, "data-" + id + "-" + j);
                    produceCount.incrementAndGet();
                }
            });
        }
        for (var t : producers) t.join();

        Thread.sleep(200);
        var consumed = vtConsumer.poll("stream", 500);
        System.out.println("  Produced: " + produceCount.get());
        System.out.println("  Consumed: " + consumed.size());

        // ─── Topic Stats ───
        System.out.println("\n--- Topic Stats ---");
        for (var topicName : broker.listTopics()) {
            var t = broker.getTopic(topicName).orElseThrow();
            System.out.printf("  %-15s %d partitions, %d messages%n",
                t.getName(), t.getNumPartitions(), t.getTotalMessages());
        }

        System.out.println("\n=== Mini Kafka Complete ===");
    }
}
