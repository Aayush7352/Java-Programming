package phase16.projects;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

final class MiniKafka {

    public static record Record(String key, String value, String topic, int partition,
                                 long offset, Instant timestamp) {
        public Record {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            Objects.requireNonNull(topic);
            Objects.requireNonNull(timestamp);
        }
    }

    public static final class Partition {
        private final int partitionId;
        private final String topic;
        private final List<Record> records = new ArrayList<>();
        private final Lock lock = new ReentrantLock();
        private final AtomicLong offsetCounter = new AtomicLong(0);

        public Partition(int partitionId, String topic) {
            this.partitionId = partitionId;
            this.topic = Objects.requireNonNull(topic);
        }

        public Record append(String key, String value) {
            lock.lock();
            try {
                var offset = offsetCounter.getAndIncrement();
                var record = new Record(key, value, topic, partitionId, offset, Instant.now());
                records.add(record);
                return record;
            } finally {
                lock.unlock();
            }
        }

        public List<Record> readFromOffset(long offset, int maxRecords) {
            lock.lock();
            try {
                if (offset >= records.size()) return List.of();
                var start = (int) offset;
                var end = Math.min(start + maxRecords, records.size());
                return List.copyOf(records.subList(start, end));
            } finally {
                lock.unlock();
            }
        }

        public long getCurrentOffset() { return offsetCounter.get(); }
        public int getPartitionId() { return partitionId; }
        public int recordCount() { lock.lock(); try { return records.size(); } finally { lock.unlock(); } }

        @Override
        public String toString() {
            return "Partition[%d-%s] records=%d offset=%d".formatted(partitionId, topic, recordCount(), getCurrentOffset());
        }
    }

    public static final class Topic {
        private final String name;
        private final List<Partition> partitions;
        private final int replicationFactor;

        public Topic(String name, int partitionCount, int replicationFactor) {
            this.name = Objects.requireNonNull(name);
            this.replicationFactor = replicationFactor;
            this.partitions = new ArrayList<>();
            for (int i = 0; i < partitionCount; i++) {
                partitions.add(new Partition(i, name));
            }
        }

        public Partition getPartition(int id) {
            if (id < 0 || id >= partitions.size())
                throw new IllegalArgumentException("Invalid partition: " + id);
            return partitions.get(id);
        }

        public Partition getPartitionForKey(String key) {
            var hash = Math.abs(key.hashCode());
            return partitions.get(hash % partitions.size());
        }

        public List<Partition> getAllPartitions() { return List.copyOf(partitions); }
        public int partitionCount() { return partitions.size(); }
        public String getName() { return name; }
        public int getReplicationFactor() { return replicationFactor; }
    }

    public static sealed interface ProducerRecordResult permits SuccessResult, FailureResult {
        boolean isSuccess();
    }

    public static record SuccessResult(Record record) implements ProducerRecordResult {
        @Override public boolean isSuccess() { return true; }
    }

    public static record FailureResult(String errorMessage) implements ProducerRecordResult {
        @Override public boolean isSuccess() { return false; }
    }

    public static final class Producer {
        private final String producerId;
        private final Map<String, Topic> topicRegistry;
        private final AtomicLong produceCounter = new AtomicLong(0);

        public Producer(String producerId, Map<String, Topic> topicRegistry) {
            this.producerId = Objects.requireNonNull(producerId);
            this.topicRegistry = topicRegistry;
        }

        public ProducerRecordResult produce(String topicName, String key, String value) {
            var topic = topicRegistry.get(topicName);
            if (topic == null)
                return new FailureResult("Topic not found: " + topicName);

            try {
                var partition = topic.getPartitionForKey(key);
                var record = partition.append(key, value);
                produceCounter.incrementAndGet();
                return new SuccessResult(record);
            } catch (Exception e) {
                return new FailureResult(e.getMessage());
            }
        }

        public ProducerRecordResult produceToPartition(String topicName, int partitionId, String key, String value) {
            var topic = topicRegistry.get(topicName);
            if (topic == null)
                return new FailureResult("Topic not found: " + topicName);
            try {
                var partition = topic.getPartition(partitionId);
                var record = partition.append(key, value);
                produceCounter.incrementAndGet();
                return new SuccessResult(record);
            } catch (Exception e) {
                return new FailureResult(e.getMessage());
            }
        }

        public String getProducerId() { return producerId; }
        public long getProducedCount() { return produceCounter.get(); }
    }

    public static final class ConsumerGroup {
        private final String groupId;
        private final Map<String, Topic> topicRegistry;
        private final Map<String, Map<Integer, Long>> offsets = new ConcurrentHashMap<>();
        private final AtomicLong consumedCounter = new AtomicLong(0);

        public ConsumerGroup(String groupId, Map<String, Topic> topicRegistry) {
            this.groupId = Objects.requireNonNull(groupId);
            this.topicRegistry = topicRegistry;
        }

        public void subscribe(String topicName) {
            var topic = topicRegistry.get(topicName);
            if (topic == null) throw new IllegalArgumentException("Topic not found: " + topicName);
            var partitionOffsets = new ConcurrentHashMap<Integer, Long>();
            for (var partition : topic.getAllPartitions()) {
                partitionOffsets.put(partition.getPartitionId(), 0L);
            }
            offsets.put(topicName, partitionOffsets);
        }

        public List<Record> poll(String topicName, int maxRecords) {
            var partitionOffsets = offsets.get(topicName);
            if (partitionOffsets == null) return List.of();

            var topic = topicRegistry.get(topicName);
            if (topic == null) return List.of();

            var results = new ArrayList<Record>();
            for (var partition : topic.getAllPartitions()) {
                var currentOffset = partitionOffsets.getOrDefault(partition.getPartitionId(), 0L);
                var records = partition.readFromOffset(currentOffset, maxRecords);
                for (var record : records) {
                    results.add(record);
                    partitionOffsets.put(partition.getPartitionId(), record.offset() + 1);
                    consumedCounter.incrementAndGet();
                }
            }

            results.sort(Comparator.comparingLong(Record::offset));
            return results.stream().limit(maxRecords).collect(Collectors.toUnmodifiableList());
        }

        public void commitOffset(String topicName, int partitionId, long offset) {
            var partitionOffsets = offsets.get(topicName);
            if (partitionOffsets != null) {
                partitionOffsets.put(partitionId, offset);
            }
        }

        public long getCurrentOffset(String topicName, int partitionId) {
            var partitionOffsets = offsets.get(topicName);
            if (partitionOffsets == null) return -1;
            return partitionOffsets.getOrDefault(partitionId, -1L);
        }

        public Map<String, Map<Integer, Long>> getOffsets() {
            return offsets.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            e -> Map.copyOf(e.getValue())));
        }

        public void seekToBeginning(String topicName) {
            var partitionOffsets = offsets.get(topicName);
            if (partitionOffsets != null) {
                partitionOffsets.replaceAll((k, v) -> 0L);
            }
        }

        public String getGroupId() { return groupId; }
        public long getConsumedCount() { return consumedCounter.get(); }
    }

    public static final class KafkaBroker {
        private final String brokerId;
        private final Map<String, Topic> topics = new ConcurrentHashMap<>();
        private final List<Producer> producers = new CopyOnWriteArrayList<>();
        private final List<ConsumerGroup> consumerGroups = new CopyOnWriteArrayList<>();
        private final AtomicInteger topicCounter = new AtomicInteger(0);

        public KafkaBroker(String brokerId) {
            this.brokerId = Objects.requireNonNull(brokerId);
        }

        public Topic createTopic(String name, int partitions, int replicationFactor) {
            var topic = new Topic(name, partitions, replicationFactor);
            topics.put(name, topic);
            return topic;
        }

        public Producer createProducer(String producerId) {
            var producer = new Producer(producerId, topics);
            producers.add(producer);
            return producer;
        }

        public ConsumerGroup createConsumerGroup(String groupId) {
            var group = new ConsumerGroup(groupId, topics);
            consumerGroups.add(group);
            return group;
        }

        public Topic getTopic(String name) { return topics.get(name); }
        public List<Topic> getAllTopics() { return List.copyOf(topics.values()); }
        public List<Producer> getAllProducers() { return List.copyOf(producers); }
        public List<ConsumerGroup> getAllConsumerGroups() { return List.copyOf(consumerGroups); }
        public String getBrokerId() { return brokerId; }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Mini Kafka ===%n".formatted());

        var broker = new KafkaBroker("broker-1");

        var ordersTopic = broker.createTopic("orders", 3, 2);
        var eventsTopic = broker.createTopic("user-events", 2, 1);
        var logsTopic = broker.createTopic("logs", 1, 1);

        System.out.println("--- Topics ---");
        broker.getAllTopics().forEach(t ->
            System.out.println("  %s: %d partitions, RF=%d".formatted(t.getName(), t.partitionCount(), t.getReplicationFactor())));

        var producer1 = broker.createProducer("producer-app-1");
        var producer2 = broker.createProducer("producer-app-2");

        System.out.println("%n--- Producing Messages ---%n".formatted());
        var results = new ArrayList<ProducerRecordResult>();
        results.add(producer1.produce("orders", "order-1001", "{\"item\":\"laptop\",\"qty\":1,\"price\":1299.99}"));
        results.add(producer1.produce("orders", "order-1002", "{\"item\":\"mouse\",\"qty\":2,\"price\":49.99}"));
        results.add(producer1.produce("user-events", "user-42", "{\"event\":\"login\",\"ip\":\"10.0.0.1\"}"));
        results.add(producer2.produce("orders", "order-1003", "{\"item\":\"keyboard\",\"qty\":1,\"price\":89.99}"));
        results.add(producer1.produce("logs", "app", "{\"level\":\"INFO\",\"msg\":\"Server started\"}"));
        results.add(producer2.produce("user-events", "user-99", "{\"event\":\"purchase\",\"item\":\"headphones\"}"));

        for (var result : results) {
            switch (result) {
                case SuccessResult sr ->
                    System.out.println("  Produced to %s[%d] @ offset %d: %s"
                            .formatted(sr.record().topic(), sr.record().partition(), sr.record().offset(), sr.record().value()));
                case FailureResult fr ->
                    System.out.println("  Failed: " + fr.errorMessage());
            }
        }

        System.out.println("%n--- Consumer Groups ---%n".formatted());
        var group1 = broker.createConsumerGroup("order-processors");
        var group2 = broker.createConsumerGroup("analytics-consumers");

        group1.subscribe("orders");
        group1.subscribe("user-events");
        group2.subscribe("user-events");
        group2.subscribe("logs");

        System.out.println("%n--- Polling from Consumer Group 1 (order-processors) ---%n".formatted());
        var polled1 = group1.poll("orders", 10);
        polled1.forEach(r ->
            System.out.println("  [G1] %s[%d]@%d: %s".formatted(r.topic(), r.partition(), r.offset(), r.value())));

        System.out.println("%n--- Polling from Consumer Group 2 (analytics-consumers) ---%n".formatted());
        var polled2 = group2.poll("user-events", 10);
        polled2.forEach(r ->
            System.out.println("  [G2] %s[%d]@%d: %s".formatted(r.topic(), r.partition(), r.offset(), r.value())));

        System.out.println("%n--- Offset Tracking ---%n".formatted());
        System.out.println("  Group 1 offsets:");
        group1.getOffsets().forEach((topic, parts) ->
            parts.forEach((part, offset) ->
                System.out.println("    %s[%d] -> offset %d".formatted(topic, part, offset))));

        System.out.println("%n--- Producing More & Re-consuming ---%n".formatted());
        producer1.produce("orders", "order-1004", "{\"item\":\"monitor\",\"qty\":1,\"price\":399.99}");
        producer2.produce("orders", "order-1005", "{\"item\":\"webcam\",\"qty\":1,\"price\":129.99}");

        var polled3 = group1.poll("orders", 10);
        System.out.println("  Group 1 new messages:");
        polled3.forEach(r ->
            System.out.println("    %s[%d]@%d: %s".formatted(r.topic(), r.partition(), r.offset(), r.value())));

        System.out.println("%n--- Seek to Beginning ---%n".formatted());
        group1.seekToBeginning("orders");
        System.out.println("  After seek, offset for orders[0]: " + group1.getCurrentOffset("orders", 0));

        var replay = group1.poll("orders", 3);
        System.out.println("  Replayed messages:");
        replay.forEach(r ->
            System.out.println("    %s[%d]@%d: %s".formatted(r.topic(), r.partition(), r.offset(), r.value())));

        System.out.println("%n--- Virtual Threads: Concurrent Producers ---%n".formatted());
        var concurrentLatch = new CountDownLatch(10);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10; i++) {
                final int idx = i;
                executor.submit(() -> {
                    var p = broker.createProducer("vt-producer-" + idx);
                    var result = p.produce("logs", "vt-thread",
                            "{\"thread\":%d,\"ts\":%d}".formatted(idx, System.currentTimeMillis()));
                    if (result instanceof SuccessResult sr) {
                        System.out.println("  [VT-%d] Produced to %s[%d]@%d".formatted(
                                idx, sr.record().topic(), sr.record().partition(), sr.record().offset()));
                    }
                    concurrentLatch.countDown();
                });
            }
        }
        concurrentLatch.await(5, TimeUnit.SECONDS);

        var logTopic = broker.getTopic("logs");
        var logPartition = logTopic.getPartition(0);
        System.out.println("%n  Log partition records: %d".formatted(logPartition.recordCount()));

        System.out.println("%n--- Pattern Matching on Results ---%n".formatted());
        for (var r : results) {
            switch (r) {
                case SuccessResult(var record) when record.topic().equals("orders") ->
                    System.out.println("  Order event: partition=%d offset=%d".formatted(record.partition(), record.offset()));
                case SuccessResult(var record) ->
                    System.out.println("  Other event: topic=%s key=%s".formatted(record.topic(), record.key()));
                case FailureResult(var err) ->
                    System.out.println("  Error: " + err);
            }
        }

        System.out.println("%nFinal Stats: %d topics, %d producers, %d consumer groups, %d total messages"
                .formatted(broker.getAllTopics().size(),
                        broker.getAllProducers().size(),
                        broker.getAllConsumerGroups().size(),
                        producer1.getProducedCount() + producer2.getProducedCount()));
        System.out.println("=== Done ===");
    }
}
