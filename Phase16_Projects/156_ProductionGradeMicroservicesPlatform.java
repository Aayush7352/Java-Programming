package phase16.projects;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

final class ProductionGradeMicroservicesPlatform {

    public static record ServiceInstance(String serviceId, String serviceName, String host,
                                          int port, String version, Map<String, String> metadata,
                                          Instant registeredAt) {
        public ServiceInstance {
            Objects.requireNonNull(serviceId);
            Objects.requireNonNull(serviceName);
            Objects.requireNonNull(host);
            Objects.requireNonNull(version);
            registeredAt = registeredAt != null ? registeredAt : Instant.now();
            metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        }

        public String getAddress() {
            return "%s:%d".formatted(host, port);
        }
    }

    public static final class ServiceRegistry {
        private final Map<String, Map<String, ServiceInstance>> services = new ConcurrentHashMap<>();
        private final Map<String, Instant> heartbeats = new ConcurrentHashMap<>();
        private final ScheduledExecutorService healthChecker = Executors.newSingleThreadScheduledExecutor();
        private final AtomicLong instanceCounter = new AtomicLong(0);
        private final List<String> eventLog = new CopyOnWriteArrayList<>();
        private volatile boolean running = true;

        public ServiceRegistry() {
            healthChecker.scheduleAtFixedRate(this::checkHealth, 5, 5, TimeUnit.SECONDS);
        }

        public ServiceInstance register(String serviceName, String host, int port, String version,
                                         Map<String, String> metadata) {
            var serviceId = "%s-%d".formatted(serviceName, instanceCounter.incrementAndGet());
            var instance = new ServiceInstance(serviceId, serviceName, host, port, version, metadata, Instant.now());
            services.computeIfAbsent(serviceName, k -> new ConcurrentHashMap<>())
                    .put(serviceId, instance);
            heartbeats.put(serviceId, Instant.now());
            eventLog.add("[%s] Registered %s (%s)".formatted(Instant.now(), serviceId, instance.getAddress()));
            return instance;
        }

        public boolean deregister(String serviceId) {
            for (var entry : services.entrySet()) {
                if (entry.getValue().remove(serviceId) != null) {
                    heartbeats.remove(serviceId);
                    eventLog.add("[%s] Deregistered %s".formatted(Instant.now(), serviceId));
                    return true;
                }
            }
            return false;
        }

        public void heartbeat(String serviceId) {
            heartbeats.put(serviceId, Instant.now());
        }

        public Optional<ServiceInstance> getInstance(String serviceId) {
            return services.values().stream()
                    .map(m -> m.get(serviceId))
                    .filter(Objects::nonNull)
                    .findFirst();
        }

        public List<ServiceInstance> getInstances(String serviceName) {
            var instances = services.get(serviceName);
            if (instances == null) return List.of();
            return List.copyOf(instances.values());
        }

        public List<ServiceInstance> getAllInstances() {
            return services.values().stream()
                    .flatMap(m -> m.values().stream())
                    .collect(Collectors.toUnmodifiableList());
        }

        public Set<String> getServiceNames() {
            return Set.copyOf(services.keySet());
        }

        private void checkHealth() {
            if (!running) return;
            var now = Instant.now();
            var threshold = Duration.ofSeconds(15);
            for (var entry : services.entrySet()) {
                var instances = entry.getValue();
                var iterator = instances.entrySet().iterator();
                while (iterator.hasNext()) {
                    var instanceEntry = iterator.next();
                    var lastHeartbeat = heartbeats.get(instanceEntry.getKey());
                    if (lastHeartbeat != null && Duration.between(lastHeartbeat, now).compareTo(threshold) > 0) {
                        eventLog.add("[%s] %s failed health check, removing".formatted(now, instanceEntry.getKey()));
                        iterator.remove();
                        heartbeats.remove(instanceEntry.getKey());
                    }
                }
            }
        }

        public List<String> getEventLog() { return List.copyOf(eventLog); }

        public void shutdown() {
            running = false;
            healthChecker.shutdown();
        }

        public int instanceCount() {
            return services.values().stream().mapToInt(Map::size).sum();
        }
    }

    public static sealed interface LoadBalancer permits RoundRobinBalancer, LeastConnectionsBalancer {
        Optional<ServiceInstance> selectInstance(List<ServiceInstance> instances);
    }

    public static final class RoundRobinBalancer implements LoadBalancer {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Optional<ServiceInstance> selectInstance(List<ServiceInstance> instances) {
            if (instances.isEmpty()) return Optional.empty();
            var idx = Math.abs(counter.getAndIncrement()) % instances.size();
            return Optional.of(instances.get(idx));
        }
    }

    public static final class LeastConnectionsBalancer implements LoadBalancer {
        private final Map<String, AtomicInteger> connections = new ConcurrentHashMap<>();

        public void incrementConnections(String serviceId) {
            connections.computeIfAbsent(serviceId, k -> new AtomicInteger(0)).incrementAndGet();
        }

        public void decrementConnections(String serviceId) {
            var conn = connections.get(serviceId);
            if (conn != null && conn.get() > 0) conn.decrementAndGet();
        }

        public int getConnectionCount(String serviceId) {
            return connections.getOrDefault(serviceId, new AtomicInteger(0)).get();
        }

        @Override
        public Optional<ServiceInstance> selectInstance(List<ServiceInstance> instances) {
            return instances.stream()
                    .min(Comparator.comparingInt(i -> getConnectionCount(i.serviceId())));
        }
    }

    public static enum CircuitState { CLOSED, HALF_OPEN, OPEN }

    public static final class CircuitBreaker {
        private final String name;
        private final int failureThreshold;
        private final long recoveryTimeoutMs;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private volatile CircuitState state = CircuitState.CLOSED;
        private volatile Instant lastFailureTime = Instant.now();
        private final Lock lock = new ReentrantLock();

        public CircuitBreaker(String name, int failureThreshold, long recoveryTimeoutMs) {
            this.name = Objects.requireNonNull(name);
            this.failureThreshold = failureThreshold;
            this.recoveryTimeoutMs = recoveryTimeoutMs;
        }

        public boolean isAllowed() {
            if (state == CircuitState.CLOSED) return true;
            if (state == CircuitState.HALF_OPEN) return true;
            if (state == CircuitState.OPEN) {
                if (Duration.between(lastFailureTime, Instant.now()).toMillis() >= recoveryTimeoutMs) {
                    lock.lock();
                    try {
                        if (state == CircuitState.OPEN) {
                            state = CircuitState.HALF_OPEN;
                            System.out.println("  [CB] %s transitioning to HALF_OPEN".formatted(name));
                        }
                    } finally {
                        lock.unlock();
                    }
                    return true;
                }
                return false;
            }
            return true;
        }

        public void recordSuccess() {
            if (state != CircuitState.CLOSED) {
                lock.lock();
                try {
                    state = CircuitState.CLOSED;
                    failureCount.set(0);
                    System.out.println("  [CB] %s reset to CLOSED".formatted(name));
                } finally {
                    lock.unlock();
                }
            }
            failureCount.set(0);
        }

        public void recordFailure() {
            lastFailureTime = Instant.now();
            var count = failureCount.incrementAndGet();
            if (count >= failureThreshold) {
                lock.lock();
                try {
                    state = CircuitState.OPEN;
                    System.out.println("  [CB] %s OPEN (failures: %d)".formatted(name, count));
                } finally {
                    lock.unlock();
                }
            }
        }

        public CircuitState getState() { return state; }
        public String getName() { return name; }
        public int getFailureCount() { return failureCount.get(); }
    }

    public static final class RetryPolicy {
        private final int maxRetries;
        private final long baseDelayMs;
        private final double backoffMultiplier;

        public RetryPolicy(int maxRetries, long baseDelayMs, double backoffMultiplier) {
            this.maxRetries = maxRetries;
            this.baseDelayMs = baseDelayMs;
            this.backoffMultiplier = backoffMultiplier;
        }

        public RetryResult execute(String operationName, Callable<Object> operation) {
            var lastError = "";
            var totalDuration = 0L;
            var attempts = 0;

            for (int i = 0; i <= maxRetries; i++) {
                attempts++;
                var start = System.currentTimeMillis();
                try {
                    var result = operation.call();
                    totalDuration += System.currentTimeMillis() - start;
                    return new RetryResult(true, result, lastError, attempts, totalDuration);
                } catch (Exception e) {
                    totalDuration += System.currentTimeMillis() - start;
                    lastError = e.getMessage();
                    if (i < maxRetries) {
                        var delay = (long) (baseDelayMs * Math.pow(backoffMultiplier, i));
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            return new RetryResult(false, null, lastError, attempts, totalDuration);
        }
    }

    public static record RetryResult(boolean success, Object result, String lastError,
                                      int attempts, long totalDurationMs) {
    }

    public static record MetricsSnapshot(String serviceName, long requestCount,
                                          long successCount, long failureCount,
                                          double avgLatencyMs, long timestamp) {
    }

    public static final class MetricsCollector {
        private final String serviceName;
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        private final Lock lock = new ReentrantLock();

        public MetricsCollector(String serviceName) {
            this.serviceName = Objects.requireNonNull(serviceName);
        }

        public void recordRequest(long latencyMs, boolean success) {
            requestCount.incrementAndGet();
            if (success) successCount.incrementAndGet();
            else failureCount.incrementAndGet();
            latencies.offer(latencyMs);
            if (latencies.size() > 1000) latencies.poll();
        }

        public MetricsSnapshot getSnapshot() {
            var avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
            return new MetricsSnapshot(serviceName, requestCount.get(), successCount.get(),
                    failureCount.get(), avg, System.currentTimeMillis());
        }

        public double getSuccessRate() {
            var total = requestCount.get();
            return total > 0 ? (double) successCount.get() / total * 100 : 100.0;
        }
    }

    public static final class MicroserviceGateway {
        private final ServiceRegistry registry;
        private final Map<String, LoadBalancer> balancers = new ConcurrentHashMap<>();
        private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
        private final Map<String, RetryPolicy> retryPolicies = new ConcurrentHashMap<>();
        private final Map<String, MetricsCollector> metrics = new ConcurrentHashMap<>();
        private final AtomicLong requestIdCounter = new AtomicLong(0);

        public MicroserviceGateway(ServiceRegistry registry) {
            this.registry = registry;
        }

        public void registerLoadBalancer(String serviceName, LoadBalancer balancer) {
            balancers.put(serviceName, balancer);
        }

        public void registerCircuitBreaker(String serviceName, CircuitBreaker cb) {
            circuitBreakers.put(serviceName, cb);
        }

        public void registerRetryPolicy(String serviceName, RetryPolicy policy) {
            retryPolicies.put(serviceName, policy);
        }

        public MetricsCollector getOrCreateMetrics(String serviceName) {
            return metrics.computeIfAbsent(serviceName, MetricsCollector::new);
        }

        public String callService(String serviceName, String requestPayload) {
            var requestId = "REQ-" + requestIdCounter.incrementAndGet();
            var metricsCollector = getOrCreateMetrics(serviceName);
            var circuitBreaker = circuitBreakers.get(serviceName);
            var retryPolicy = retryPolicies.get(serviceName);
            var balancer = balancers.getOrDefault(serviceName, new RoundRobinBalancer());
            var startTime = System.currentTimeMillis();

            if (circuitBreaker != null && !circuitBreaker.isAllowed()) {
                metricsCollector.recordRequest(System.currentTimeMillis() - startTime, false);
                return "{\"error\":\"circuit_breaker_open\",\"service\":\"%s\",\"requestId\":\"%s\"}"
                        .formatted(serviceName, requestId);
            }

            try {
                var instances = registry.getInstances(serviceName);
                if (instances.isEmpty()) {
                    metricsCollector.recordRequest(System.currentTimeMillis() - startTime, false);
                    return "{\"error\":\"no_instances\",\"service\":\"%s\",\"requestId\":\"%s\"}"
                            .formatted(serviceName, requestId);
                }

                var instanceOpt = balancer.selectInstance(instances);
                if (instanceOpt.isEmpty()) {
                    metricsCollector.recordRequest(System.currentTimeMillis() - startTime, false);
                    return "{\"error\":\"no_instance_selected\",\"service\":\"%s\",\"requestId\":\"%s\"}"
                            .formatted(serviceName, requestId);
                }

                var instance = instanceOpt.get();

                Callable<Object> operation = () -> {
                    Thread.sleep(ThreadLocalRandom.current().nextLong(10, 100));
                    return "{\"result\":\"ok\",\"service\":\"%s\",\"instance\":\"%s\",\"requestId\":\"%s\",\"payload\":%s}"
                            .formatted(serviceName, instance.serviceId(), requestId, requestPayload);
                };

                if (retryPolicy != null) {
                    var result = retryPolicy.execute(serviceName, operation);
                    if (result.success()) {
                        metricsCollector.recordRequest(result.totalDurationMs(), true);
                        if (circuitBreaker != null) circuitBreaker.recordSuccess();
                        return (String) result.result();
                    } else {
                        metricsCollector.recordRequest(result.totalDurationMs(), false);
                        if (circuitBreaker != null) circuitBreaker.recordFailure();
                        return "{\"error\":\"%s\",\"service\":\"%s\",\"requestId\":\"%s\",\"attempts\":%d}"
                                .formatted(result.lastError(), serviceName, requestId, result.attempts());
                    }
                } else {
                    var result = operation.call();
                    metricsCollector.recordRequest(System.currentTimeMillis() - startTime, true);
                    if (circuitBreaker != null) circuitBreaker.recordSuccess();
                    return (String) result;
                }
            } catch (Exception e) {
                metricsCollector.recordRequest(System.currentTimeMillis() - startTime, false);
                if (circuitBreaker != null) circuitBreaker.recordFailure();
                return "{\"error\":\"%s\",\"service\":\"%s\",\"requestId\":\"%s\"}"
                        .formatted(e.getMessage(), serviceName, requestId);
            }
        }

        public Map<String, MetricsSnapshot> getAllMetrics() {
            return metrics.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            e -> e.getValue().getSnapshot()));
        }

        public Map<String, CircuitState> getCircuitStates() {
            return circuitBreakers.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            e -> e.getValue().getState()));
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Production-Grade Microservices Platform ===%n".formatted());

        var registry = new ServiceRegistry();
        var gateway = new MicroserviceGateway(registry);

        System.out.println("--- Service Registration ---");
        var userSvc1 = registry.register("user-service", "10.0.1.10", 8081, "1.0.0",
                Map.of("region", "us-east", "env", "prod"));
        var userSvc2 = registry.register("user-service", "10.0.1.11", 8081, "1.0.0",
                Map.of("region", "us-east", "env", "prod"));
        var orderSvc1 = registry.register("order-service", "10.0.2.10", 8082, "2.1.0",
                Map.of("region", "us-west", "env", "prod"));
        var orderSvc2 = registry.register("order-service", "10.0.2.11", 8082, "2.1.0",
                Map.of("region", "us-west", "env", "prod"));
        var paymentSvc = registry.register("payment-service", "10.0.3.10", 8083, "1.5.0",
                Map.of("region", "eu-west", "env", "prod"));
        var notificationSvc = registry.register("notification-service", "10.0.4.10", 8084, "0.9.0",
                Map.of("region", "us-east", "env", "staging"));

        System.out.println("  Registered %d instances across %d services"
                .formatted(registry.instanceCount(), registry.getServiceNames().size()));

        System.out.println("%n--- Service Discovery ---%n".formatted());
        var userInstances = registry.getInstances("user-service");
        System.out.println("  user-service instances: " + userInstances.size());
        userInstances.forEach(i ->
            System.out.println("    %s at %s (v%s)".formatted(i.serviceId(), i.getAddress(), i.version())));

        System.out.println("%n--- Load Balancers ---%n".formatted());
        gateway.registerLoadBalancer("user-service", new RoundRobinBalancer());
        gateway.registerLoadBalancer("order-service", new LeastConnectionsBalancer());

        System.out.println("  RR select: " + gateway.callService("user-service", "{\"action\":\"getUser\",\"id\":1}"));
        System.out.println("  RR select: " + gateway.callService("user-service", "{\"action\":\"getUser\",\"id\":2}"));

        System.out.println("%n--- Circuit Breaker Demo ---%n".formatted());
        gateway.registerCircuitBreaker("payment-service",
                new CircuitBreaker("payment-cb", 3, 2000));

        gateway.registerRetryPolicy("payment-service",
                new RetryPolicy(2, 100, 2.0));

        for (int i = 0; i < 5; i++) {
            var result = gateway.callService("payment-service",
                    "{\"action\":\"process\",\"amount\":99.99}");
            System.out.println("  Attempt %d: %s".formatted(i + 1,
                    result.length() > 60 ? result.substring(0, 60) + "..." : result));
        }

        System.out.println("  Circuit states: " + gateway.getCircuitStates());

        System.out.println("%n--- Retry with Backoff ---%n".formatted());
        var retryPolicy = new RetryPolicy(3, 200, 2.0);
        var retryResult = retryPolicy.execute("test-op", () -> {
            throw new RuntimeException("Simulated transient failure");
        });
        System.out.println("  Retry result: success=%s, attempts=%d, totalMs=%d, error=%s"
                .formatted(retryResult.success(), retryResult.attempts(),
                        retryResult.totalDurationMs(), retryResult.lastError()));

        System.out.println("%n--- Health Check / Heartbeat ---%n".formatted());
        registry.heartbeat(userSvc1.serviceId());
        registry.heartbeat(userSvc2.serviceId());
        registry.heartbeat(orderSvc1.serviceId());
        System.out.println("  Heartbeats sent for 3 instances");

        System.out.println("%n--- Deregister ---%n".formatted());
        var deregistered = registry.deregister(notificationSvc.serviceId());
        System.out.println("  Deregistered notification-service: " + deregistered);
        System.out.println("  Instances now: " + registry.instanceCount());

        System.out.println("%n--- Concurrent Load via Virtual Threads ---%n".formatted());
        var latch = new CountDownLatch(20);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 20; i++) {
                final int idx = i;
                executor.submit(() -> {
                    var svc = idx % 3 == 0 ? "user-service" :
                             (idx % 3 == 1 ? "order-service" : "payment-service");
                    var result = gateway.callService(svc,
                            "{\"request\":%d,\"data\":\"test\"}".formatted(idx));
                    latch.countDown();
                });
            }
        }
        latch.await(10, TimeUnit.SECONDS);

        System.out.println("%n--- Metrics ---%n".formatted());
        var metrics = gateway.getAllMetrics();
        metrics.forEach((name, snap) ->
            System.out.println("  %s: %,d req, %.1f%% success, avg %.1fms"
                    .formatted(name, snap.requestCount(), snap.successCount() * 100.0 / Math.max(1, snap.requestCount()), snap.avgLatencyMs())));

        System.out.println("%n--- Pattern Matching on LB Strategies ---%n".formatted());
        for (var entry : Map.of("user-service", (LoadBalancer) new RoundRobinBalancer(),
                                "order-service", (LoadBalancer) new LeastConnectionsBalancer()).entrySet()) {
            switch (entry.getValue()) {
                case RoundRobinBalancer rr ->
                    System.out.println("  %s uses Round Robin".formatted(entry.getKey()));
                case LeastConnectionsBalancer lc ->
                    System.out.println("  %s uses Least Connections".formatted(entry.getKey()));
            }
        }

        System.out.println("%n--- Registry Event Log ---%n".formatted());
        registry.getEventLog().forEach(log -> System.out.println("  " + log));

        registry.shutdown();
        System.out.println("%nFinal Stats: %d services, %d instances, %d total requests"
                .formatted(registry.getServiceNames().size(), registry.instanceCount(),
                        metrics.values().stream().mapToLong(MetricsSnapshot::requestCount).sum()));
        System.out.println("=== Done ===");
    }
}
