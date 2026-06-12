package phase16.projects;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * ProductionGradeMicroservicesPlatform.java
 *
 * Production microservices platform: Service registry (in-memory Eureka-like),
 * load balancer (round-robin + least connections), circuit breaker
 * (closed/open/half-open), retry mechanism, health check, metrics collection.
 */
public class ProductionGradeMicroservicesPlatform {

    // ═══════════════════════════════════════════════
    // Records & Enums
    // ═══════════════════════════════════════════════

    enum ServiceStatus { UP, DOWN, UNKNOWN }
    enum CircuitBreakerState { CLOSED, OPEN, HALF_OPEN }

    record ServiceInstance(String instanceId, String serviceName, String host, int port,
                           ServiceStatus status, Instant registeredAt, Instant lastHeartbeat) {
        ServiceInstance withStatus(ServiceStatus s) {
            return new ServiceInstance(instanceId, serviceName, host, port, s, registeredAt, lastHeartbeat);
        }
        ServiceInstance withHeartbeat() {
            return new ServiceInstance(instanceId, serviceName, host, port, status, registeredAt, Instant.now());
        }
    }

    record ServiceDefinition(String serviceName, String version, int requiredInstances) {}

    record HealthCheckResult(String instanceId, boolean healthy, String details, long responseTimeMs) {}

    record MetricsSnapshot(String instanceId, long requestCount, long successCount, long failureCount,
                           double avgResponseTime, long timestamp) {}

    record CircuitBreakerConfig(int failureThreshold, int successThreshold, long openTimeoutMs, long halfOpenMaxCalls) {}

    // ═══════════════════════════════════════════════
    // Service Registry
    // ═══════════════════════════════════════════════

    static final class ServiceRegistry {
        private final ConcurrentHashMap<String, ConcurrentHashMap<String, ServiceInstance>> instances = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, ServiceDefinition> definitions = new ConcurrentHashMap<>();
        private final ScheduledExecutorService healthChecker = Executors.newSingleThreadScheduledExecutor();

        ServiceRegistry() {
            healthChecker.scheduleAtFixedRate(this::checkHealth, 5, 10, TimeUnit.SECONDS);
        }

        public ServiceDefinition registerService(String serviceName, String version, int requiredInstances) {
            var def = new ServiceDefinition(serviceName, version, requiredInstances);
            definitions.put(serviceName, def);
            instances.put(serviceName, new ConcurrentHashMap<>());
            return def;
        }

        public ServiceInstance registerInstance(String serviceName, String host, int port) {
            var instancesMap = instances.get(serviceName);
            if (instancesMap == null) {
                throw new IllegalArgumentException("Service not registered: " + serviceName);
            }
            String instanceId = serviceName + ":" + host + ":" + port + ":" + UUID.randomUUID().toString().substring(0, 8);
            var instance = new ServiceInstance(instanceId, serviceName, host, port,
                ServiceStatus.UP, Instant.now(), Instant.now());
            instancesMap.put(instanceId, instance);
            return instance;
        }

        public boolean deregisterInstance(String instanceId) {
            for (var entry : instances.entrySet()) {
                if (entry.getValue().remove(instanceId) != null) return true;
            }
            return false;
        }

        public void heartbeat(String instanceId) {
            for (var entry : instances.entrySet()) {
                entry.getValue().computeIfPresent(instanceId, (k, v) -> v.withHeartbeat());
            }
        }

        public List<ServiceInstance> getInstances(String serviceName) {
            var map = instances.get(serviceName);
            if (map == null) return List.of();
            return map.values().stream()
                .filter(i -> i.status() == ServiceStatus.UP)
                .collect(Collectors.toList());
        }

        public List<ServiceInstance> getAllInstances() {
            return instances.values().stream()
                .flatMap(m -> m.values().stream())
                .collect(Collectors.toList());
        }

        public Optional<ServiceInstance> getInstance(String instanceId) {
            for (var entry : instances.entrySet()) {
                var inst = entry.getValue().get(instanceId);
                if (inst != null) return Optional.of(inst);
            }
            return Optional.empty();
        }

        public boolean isServiceAvailable(String serviceName) {
            return !getInstances(serviceName).isEmpty();
        }

        public List<String> getRegisteredServices() {
            return List.copyOf(definitions.keySet());
        }

        private void checkHealth() {
            for (var entry : instances.entrySet()) {
                for (var instEntry : entry.getValue().entrySet()) {
                    var inst = instEntry.getValue();
                    // If heartbeat is older than 30 seconds, mark as DOWN
                    if (inst.lastHeartbeat().isBefore(Instant.now().minusSeconds(30))) {
                        entry.getValue().put(instEntry.getKey(), inst.withStatus(ServiceStatus.DOWN));
                    }
                }
            }
        }

        public void shutdown() { healthChecker.shutdown(); }
    }

    // ═══════════════════════════════════════════════
    // Load Balancer
    // ═══════════════════════════════════════════════

    sealed interface LoadBalancingStrategy permits RoundRobinStrategy, LeastConnectionsStrategy {}

    static final class RoundRobinStrategy implements LoadBalancingStrategy {
        private final AtomicInteger counter = new AtomicInteger(0);

        public Optional<ServiceInstance> select(List<ServiceInstance> instances) {
            if (instances.isEmpty()) return Optional.empty();
            int idx = Math.abs(counter.getAndIncrement()) % instances.size();
            return Optional.of(instances.get(idx));
        }
    }

    static final class LeastConnectionsStrategy implements LoadBalancingStrategy {
        private final ConcurrentHashMap<String, AtomicInteger> connections = new ConcurrentHashMap<>();

        public Optional<ServiceInstance> select(List<ServiceInstance> instances) {
            return instances.stream()
                .min(Comparator.comparingInt(i -> connections.computeIfAbsent(i.instanceId(), k -> new AtomicInteger(0)).get()));
        }

        public void acquireConnection(String instanceId) {
            connections.computeIfAbsent(instanceId, k -> new AtomicInteger(0)).incrementAndGet();
        }

        public void releaseConnection(String instanceId) {
            var conn = connections.get(instanceId);
            if (conn != null && conn.get() > 0) conn.decrementAndGet();
        }

        public int getConnections(String instanceId) {
            return connections.getOrDefault(instanceId, new AtomicInteger(0)).get();
        }
    }

    static final class LoadBalancer {
        private final ServiceRegistry registry;
        private final ConcurrentHashMap<String, LoadBalancingStrategy> strategies = new ConcurrentHashMap<>();

        LoadBalancer(ServiceRegistry registry) {
            this.registry = registry;
        }

        public void setStrategy(String serviceName, LoadBalancingStrategy strategy) {
            strategies.put(serviceName, strategy);
        }

        public Optional<ServiceInstance> nextInstance(String serviceName) {
            var instances = registry.getInstances(serviceName);
            if (instances.isEmpty()) return Optional.empty();

            var strategy = strategies.getOrDefault(serviceName, new RoundRobinStrategy());
            if (strategy instanceof LeastConnectionsStrategy lc) {
                var selected = lc.select(instances);
                selected.ifPresent(s -> lc.acquireConnection(s.instanceId()));
                return selected;
            }
            return ((RoundRobinStrategy) strategy).select(instances);
        }

        public void releaseInstance(String serviceName, String instanceId) {
            var strategy = strategies.get(serviceName);
            if (strategy instanceof LeastConnectionsStrategy lc) {
                lc.releaseConnection(instanceId);
            }
        }

        public int getActiveConnections(String serviceName, String instanceId) {
            var strategy = strategies.get(serviceName);
            if (strategy instanceof LeastConnectionsStrategy lc) {
                return lc.getConnections(instanceId);
            }
            return 0;
        }
    }

    // ═══════════════════════════════════════════════
    // Circuit Breaker
    // ═══════════════════════════════════════════════

    static final class CircuitBreaker {
        private final String name;
        private final CircuitBreakerConfig config;
        private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger halfOpenCallCount = new AtomicInteger(0);
        private volatile Instant lastFailureTime = Instant.now();
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong totalFailures = new AtomicLong(0);

        CircuitBreaker(String name, CircuitBreakerConfig config) {
            this.name = name;
            this.config = config;
        }

        public synchronized <T> Result<T> execute(Supplier<T> operation) {
            totalCalls.incrementAndGet();

            switch (state) {
                case OPEN -> {
                    if (Instant.now().isBefore(lastFailureTime.plusMillis(config.openTimeoutMs()))) {
                        totalFailures.incrementAndGet();
                        return Result.failure(new CircuitBreakerOpenException("Circuit breaker is OPEN for " + name));
                    }
                    // Transition to HALF_OPEN
                    state = CircuitBreakerState.HALF_OPEN;
                    halfOpenCallCount.set(0);
                    successCount.set(0);
                }
                case HALF_OPEN -> {
                    if (halfOpenCallCount.incrementAndGet() > config.halfOpenMaxCalls()) {
                        totalFailures.incrementAndGet();
                        return Result.failure(new CircuitBreakerOpenException("Circuit breaker HALF_OPEN at max calls for " + name));
                    }
                }
            }

            try {
                T result = operation.get();
                onSuccess();
                return Result.success(result);
            } catch (Exception e) {
                totalFailures.incrementAndGet();
                onFailure();
                return Result.failure(e);
            }
        }

        private synchronized void onSuccess() {
            switch (state) {
                case HALF_OPEN -> {
                    if (successCount.incrementAndGet() >= config.successThreshold()) {
                        state = CircuitBreakerState.CLOSED;
                        failureCount.set(0);
                        halfOpenCallCount.set(0);
                    }
                }
                case CLOSED -> {
                    failureCount.set(0);
                }
            }
        }

        private synchronized void onFailure() {
            switch (state) {
                case CLOSED -> {
                    if (failureCount.incrementAndGet() >= config.failureThreshold()) {
                        state = CircuitBreakerState.OPEN;
                        lastFailureTime = Instant.now();
                    }
                }
                case HALF_OPEN -> {
                    state = CircuitBreakerState.OPEN;
                    lastFailureTime = Instant.now();
                    halfOpenCallCount.set(0);
                }
            }
        }

        public CircuitBreakerState getState() { return state; }
        public int getFailureCount() { return failureCount.get(); }
        public int getSuccessCount() { return successCount.get(); }
        public long getTotalCalls() { return totalCalls.get(); }
        public long getTotalFailures() { return totalFailures.get(); }
        public double getFailureRate() {
            long total = totalCalls.get();
            return total == 0 ? 0 : (double) totalFailures.get() / total * 100;
        }

        public void reset() {
            state = CircuitBreakerState.CLOSED;
            failureCount.set(0);
            successCount.set(0);
            halfOpenCallCount.set(0);
        }

        static class CircuitBreakerOpenException extends RuntimeException {
            CircuitBreakerOpenException(String msg) { super(msg); }
        }

        record Result<T>(T value, Exception error, boolean success) {
            static <T> Result<T> success(T value) { return new Result<>(value, null, true); }
            static <T> Result<T> failure(Exception e) { return new Result<>(null, e, false); }
        }
    }

    // ═══════════════════════════════════════════════
    // Retry Mechanism
    // ═══════════════════════════════════════════════

    static final class RetryHandler {
        private final int maxRetries;
        private final long baseDelayMs;
        private final double backoffMultiplier;
        private final Set<Class<?>> retryableExceptions;

        RetryHandler(int maxRetries, long baseDelayMs, double backoffMultiplier, Class<?>... retryableExceptions) {
            this.maxRetries = maxRetries;
            this.baseDelayMs = baseDelayMs;
            this.backoffMultiplier = backoffMultiplier;
            this.retryableExceptions = new HashSet<>(Arrays.asList(retryableExceptions));
        }

        public <T> T execute(Supplier<T> operation) {
            int attempts = 0;
            long delay = baseDelayMs;
            while (true) {
                try {
                    return operation.get();
                } catch (Exception e) {
                    attempts++;
                    if (attempts > maxRetries || !isRetryable(e)) {
                        throw e;
                    }
                    try { Thread.sleep(delay); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                    delay = (long) (delay * backoffMultiplier);
                }
            }
        }

        private boolean isRetryable(Exception e) {
            if (retryableExceptions.isEmpty()) return true;
            return retryableExceptions.stream().anyMatch(c -> c.isInstance(e));
        }
    }

    // ═══════════════════════════════════════════════
    // Metrics Collector
    // ═══════════════════════════════════════════════

    static final class MetricsCollector {
        private final ConcurrentHashMap<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, AtomicLong> successCounts = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, AtomicLong> failureCounts = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<Long>> responseTimes = new ConcurrentHashMap<>();

        public void recordRequest(String instanceId, long responseTimeMs, boolean success) {
            requestCounts.computeIfAbsent(instanceId, k -> new AtomicLong()).incrementAndGet();
            if (success) {
                successCounts.computeIfAbsent(instanceId, k -> new AtomicLong()).incrementAndGet();
            } else {
                failureCounts.computeIfAbsent(instanceId, k -> new AtomicLong()).incrementAndGet();
            }
            responseTimes.computeIfAbsent(instanceId, k -> new CopyOnWriteArrayList<>()).add(responseTimeMs);
        }

        public MetricsSnapshot getSnapshot(String instanceId) {
            long requests = requestCounts.getOrDefault(instanceId, new AtomicLong(0)).get();
            long successes = successCounts.getOrDefault(instanceId, new AtomicLong(0)).get();
            long failures = failureCounts.getOrDefault(instanceId, new AtomicLong(0)).get();
            var times = responseTimes.getOrDefault(instanceId, List.of());
            double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0);
            return new MetricsSnapshot(instanceId, requests, successes, failures, avgTime, System.currentTimeMillis());
        }

        public Map<String, MetricsSnapshot> getAllSnapshots() {
            return requestCounts.keySet().stream()
                .collect(Collectors.toMap(k -> k, this::getSnapshot));
        }

        public void reset() {
            requestCounts.clear();
            successCounts.clear();
            failureCounts.clear();
            responseTimes.clear();
        }
    }

    // ═══════════════════════════════════════════════
    // Microservice Platform
    // ═══════════════════════════════════════════════

    static final class MicroservicesPlatform {
        private final ServiceRegistry registry;
        private final LoadBalancer loadBalancer;
        private final MetricsCollector metrics;
        private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
        private final List<String> services = new CopyOnWriteArrayList<>();
        private final Random random = new Random();

        MicroservicesPlatform() {
            this.registry = new ServiceRegistry();
            this.loadBalancer = new LoadBalancer(registry);
            this.metrics = new MetricsCollector();
        }

        public ServiceRegistry getRegistry() { return registry; }
        public LoadBalancer getLoadBalancer() { return loadBalancer; }
        public MetricsCollector getMetrics() { return metrics; }

        public ServiceInstance registerServiceAndInstance(String serviceName, String version,
                                                           int requiredInstances, String host, int port) {
            registry.registerService(serviceName, version, requiredInstances);
            var instance = registry.registerInstance(serviceName, host, port);
            services.add(serviceName);
            loadBalancer.setStrategy(serviceName, new RoundRobinStrategy());
            return instance;
        }

        public CircuitBreaker addCircuitBreaker(String serviceName, CircuitBreakerConfig config) {
            var cb = new CircuitBreaker(serviceName, config);
            circuitBreakers.put(serviceName, cb);
            return cb;
        }

        // Simulate a service call
        public ServiceResponse callService(String serviceName, String payload) {
            long start = System.nanoTime();
            var cb = circuitBreakers.get(serviceName);

            if (cb != null) {
                var result = cb.execute(() -> executeCall(serviceName, payload));
                long elapsed = (System.nanoTime() - start) / 1_000_000;
                String instanceId = result.success() ? "unknown" : "unknown";

                if (result.success()) {
                    var resp = result.value();
                    metrics.recordRequest(resp.instanceId(), elapsed, true);
                    return resp;
                } else {
                    metrics.recordRequest(serviceName + ":circuit-breaker", elapsed, false);
                    return new ServiceResponse(false, 503, "Circuit breaker open: " + result.error().getMessage(), null, null);
                }
            }

            try {
                var response = executeCall(serviceName, payload);
                long elapsed = (System.nanoTime() - start) / 1_000_000;
                metrics.recordRequest(response.instanceId(), elapsed, true);
                return response;
            } catch (Exception e) {
                long elapsed = (System.nanoTime() - start) / 1_000_000;
                metrics.recordRequest(serviceName + ":error", elapsed, false);
                return new ServiceResponse(false, 500, e.getMessage(), null, null);
            }
        }

        private ServiceResponse executeCall(String serviceName, String payload) {
            var instance = loadBalancer.nextInstance(serviceName);
            if (instance.isEmpty()) {
                throw new RuntimeException("No available instances for " + serviceName);
            }
            var inst = instance.get();

            // Simulate occasional failure (10% chance)
            if (random.nextDouble() < 0.1) {
                loadBalancer.releaseInstance(serviceName, inst.instanceId());
                throw new RuntimeException("Service call failed for " + inst.instanceId());
            }

            // Simulate processing delay
            try { Thread.sleep(random.nextInt(5, 30)); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            loadBalancer.releaseInstance(serviceName, inst.instanceId());
            return new ServiceResponse(true, 200, "OK",
                inst.instanceId(),
                "Processed by " + inst.host() + ":" + inst.port());
        }

        record ServiceResponse(boolean success, int statusCode, String message, String instanceId, String details) {}

        public void shutdown() { registry.shutdown(); }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        System.out.println("=== Production-Grade Microservices Platform ===\n");

        var platform = new MicroservicesPlatform();

        // ─── Register Services ───
        System.out.println("--- Service Registration ---");
        platform.registerServiceAndInstance("user-service", "1.0.0", 2, "192.168.1.10", 8081);
        platform.registerServiceAndInstance("user-service", "1.0.0", 2, "192.168.1.11", 8081);
        platform.registerServiceAndInstance("order-service", "1.0.0", 3, "192.168.1.20", 8082);
        platform.registerServiceAndInstance("order-service", "1.0.0", 3, "192.168.1.21", 8082);
        platform.registerServiceAndInstance("order-service", "1.0.0", 3, "192.168.1.22", 8082);
        platform.registerServiceAndInstance("payment-service", "2.0.0", 2, "192.168.1.30", 8083);
        platform.registerServiceAndInstance("notification-service", "1.5.0", 1, "192.168.1.40", 8084);
        System.out.println("  Registered 4 services with " + platform.getRegistry().getAllInstances().size() + " instances");

        // ─── Set Load Balancing Strategies ───
        System.out.println("\n--- Load Balancer Strategies ---");
        platform.getLoadBalancer().setStrategy("user-service", new RoundRobinStrategy());
        platform.getLoadBalancer().setStrategy("order-service", new LeastConnectionsStrategy());
        platform.getLoadBalancer().setStrategy("payment-service", new RoundRobinStrategy());
        System.out.println("  user-service: Round Robin");
        System.out.println("  order-service: Least Connections");
        System.out.println("  payment-service: Round Robin");

        // ─── Circuit Breakers ───
        System.out.println("\n--- Circuit Breakers ---");
        var cbConfig = new CircuitBreakerConfig(3, 2, 5000, 3);
        var userCb = platform.addCircuitBreaker("user-service", cbConfig);
        var orderCb = platform.addCircuitBreaker("order-service", cbConfig);
        System.out.println("  user-service CB: threshold=3, timeout=5s");
        System.out.println("  order-service CB: threshold=3, timeout=5s");

        // ─── Test Load Balancer Distribution ───
        System.out.println("\n--- Load Balancer Distribution (Round Robin) ---");
        var rrStats = new HashMap<String, Integer>();
        platform.getLoadBalancer().setStrategy("user-service", new RoundRobinStrategy());
        for (int i = 0; i < 20; i++) {
            var inst = platform.getLoadBalancer().nextInstance("user-service");
            inst.ifPresent(i2 -> rrStats.merge(i2.instanceId(), 1, Integer::sum));
        }
        rrStats.forEach((id, count) -> {
            var inst = platform.getRegistry().getInstance(id);
            inst.ifPresent(i -> System.out.printf("  %s:%d -> %d calls%n", i.host(), i.port(), count));
        });

        // ─── Service Calls ───
        System.out.println("\n--- Service Calls ---");
        for (int i = 0; i < 10; i++) {
            var resp = platform.callService("user-service", "{\"userId\":" + i + "}");
            System.out.printf("  Call %d: %s (instance=%s, details=%s)%n",
                i + 1, resp.success() ? "OK" : "FAIL",
                resp.instanceId(), resp.details());
        }

        // ─── Circuit Breaker Failure Test ───
        System.out.println("\n--- Circuit Breaker Test ---");
        System.out.println("  Initial state: " + userCb.getState());

        // Simulate failures by calling a service that will fail
        // We need to force failures. Let's make calls that hit the 10% failure rate.
        int failures = 0;
        for (int i = 0; i < 20; i++) {
            var resp = platform.callService("user-service", "{\"test\":true}");
            if (!resp.success()) {
                failures++;
                System.out.printf("  Call %d FAILED: %s%n", i + 1, resp.message());
            }
            if (userCb.getState() == CircuitBreakerState.OPEN) {
                System.out.println("  Circuit breaker OPEN after " + (i + 1) + " calls!");
                break;
            }
        }
        System.out.println("  Circuit breaker state: " + userCb.getState());
        System.out.println("  Failure count: " + userCb.getFailureCount());
        System.out.println("  Total failures: " + userCb.getTotalFailures());

        // ─── Metrics ───
        System.out.println("\n--- Metrics ---");
        var snapshots = platform.getMetrics().getAllSnapshots();
        for (var entry : snapshots.entrySet()) {
            var s = entry.getValue();
            double successRate = s.requestCount() > 0 ?
                (double) s.successCount() / s.requestCount() * 100 : 0;
            System.out.printf("  %-35s req=%d suc=%d fail=%d avg=%.1fms successRate=%.1f%%%n",
                entry.getKey(), s.requestCount(), s.successCount(),
                s.failureCount(), s.avgResponseTime(), successRate);
        }

        // ─── Retry Handler Demo ───
        System.out.println("\n--- Retry Handler ---");
        var retryHandler = new RetryHandler(3, 50, 2.0, RuntimeException.class);
        AtomicInteger attemptCounter = new AtomicInteger(0);

        try {
            String result = retryHandler.execute(() -> {
                int attempt = attemptCounter.incrementAndGet();
                if (attempt <= 2) {
                    throw new RuntimeException("Attempt " + attempt + " failed");
                }
                return "Success on attempt " + attempt;
            });
            System.out.println("  " + result);
        } catch (Exception e) {
            System.out.println("  Failed: " + e.getMessage());
        }

        // ─── Health Check ───
        System.out.println("\n--- Health Check ---");
        System.out.println("  Registered services: " + platform.getRegistry().getRegisteredServices());
        for (var svc : platform.getRegistry().getRegisteredServices()) {
            int active = platform.getRegistry().getInstances(svc).size();
            int total = (int) platform.getRegistry().getAllInstances().stream()
                .filter(i -> i.serviceName().equals(svc)).count();
            System.out.printf("  %-20s active=%d/%d%n", svc, active, total);
        }

        // ─── Simulate Instance Failure ───
        System.out.println("\n--- Instance Failure & Recovery ---");
        var allInstances = platform.getRegistry().getAllInstances();
        if (!allInstances.isEmpty()) {
            var toFail = allInstances.getFirst();
            System.out.println("  Failing instance: " + toFail.instanceId());
            platform.getRegistry().deregisterInstance(toFail.instanceId());
            System.out.println("  Active " + toFail.serviceName() + " instances: "
                + platform.getRegistry().getInstances(toFail.serviceName()).size());
        }

        // ─── Concurrent Load ───
        System.out.println("\n--- Concurrent Load (Virtual Threads) ---");
        var concurrentCalls = new AtomicInteger(0);
        var concurrentSuccess = new AtomicInteger(0);
        var vtThreads = new Thread[30];

        for (int i = 0; i < 30; i++) {
            vtThreads[i] = Thread.ofVirtual().start(() -> {
                var resp = platform.callService("order-service", "{\"concurrent\":true}");
                concurrentCalls.incrementAndGet();
                if (resp.success()) concurrentSuccess.incrementAndGet();
            });
        }
        for (var t : vtThreads) t.join();

        System.out.println("  Concurrent calls: " + concurrentCalls.get());
        System.out.println("  Successful: " + concurrentSuccess.get());
        System.out.println("  Failure rate: " +
            String.format("%.1f%%", (double) (concurrentCalls.get() - concurrentSuccess.get()) / concurrentCalls.get() * 100));

        // ─── Final Platform Summary ───
        System.out.println("\n--- Platform Summary ---");
        System.out.println("  Registered services: " + platform.getRegistry().getRegisteredServices().size());
        System.out.println("  Total instances: " + platform.getRegistry().getAllInstances().size());
        System.out.println("  Circuit breakers: " + (userCb.getState()) + ", " + (orderCb.getState()));
        System.out.println("  Total metrics entries: " + platform.getMetrics().getAllSnapshots().size());

        platform.shutdown();

        System.out.println("\n=== Production-Grade Microservices Platform Complete ===");
    }
}
