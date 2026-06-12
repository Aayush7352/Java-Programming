package phase13.spring;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

// --- Demo: Microservices patterns: API Gateway, Service Discovery, Config Server,
//            Circuit Breaker (Resilience4j concept), Distributed tracing ---

// =============================================
// 1. Config Server concept
// =============================================
class ConfigServer {
    private final Map<String, Map<String, String>> configs = new ConcurrentHashMap<>();

    public ConfigServer() {
        // Service-specific configurations
        configs.put("user-service", Map.of(
                "server.port", "8081",
                "db.url", "jdbc:postgresql://localhost:5432/users",
                "cache.ttl", "300"
        ));
        configs.put("order-service", Map.of(
                "server.port", "8082",
                "db.url", "jdbc:postgresql://localhost:5432/orders",
                "payment.timeout", "5000"
        ));
        configs.put("inventory-service", Map.of(
                "server.port", "8083",
                "db.url", "jdbc:postgresql://localhost:5432/inventory",
                "low.stock.threshold", "10"
        ));
    }

    public Map<String, String> getConfig(String serviceName) {
        return configs.getOrDefault(serviceName, Map.of());
    }

    public void printConfig(String serviceName) {
        System.out.println("\n  [Config Server] Configuration for " + serviceName + ":");
        getConfig(serviceName).forEach((k, v) -> System.out.println("    " + k + " = " + v));
    }
}

// =============================================
// 2. Service Discovery concept
// =============================================
record ServiceInstance(String serviceId, String host, int port, boolean healthy) {}

class ServiceRegistry {
    private final Map<String, List<ServiceInstance>> registry = new ConcurrentHashMap<>();

    public void register(String serviceId, String host, int port) {
        registry.computeIfAbsent(serviceId, k -> new CopyOnWriteArrayList<>())
                .add(new ServiceInstance(serviceId, host, port, true));
        System.out.println("  [Service Registry] Registered " + serviceId + " at " + host + ":" + port);
    }

    public List<ServiceInstance> getInstances(String serviceId) {
        return registry.getOrDefault(serviceId, List.of());
    }

    public ServiceInstance getInstance(String serviceId) {
        var instances = getInstances(serviceId);
        if (instances.isEmpty()) {
            throw new RuntimeException("No instances found for " + serviceId);
        }
        // Simple round-robin (just return first for demo)
        return instances.get(0);
    }
}

// =============================================
// 3. Circuit Breaker (Resilience4j concept)
// =============================================
enum CircuitState { CLOSED, OPEN, HALF_OPEN }

class CircuitBreaker {
    private CircuitState state = CircuitState.CLOSED;
    private final int failureThreshold;
    private final long timeoutMs;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private Instant lastFailureTime;
    private final String name;

    public CircuitBreaker(String name, int failureThreshold, long timeoutMs) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeoutMs;
    }

    public synchronized <T> T call(Supplier<T> operation, Supplier<T> fallback) {
        if (state == CircuitState.OPEN) {
            if (Duration.between(lastFailureTime, Instant.now()).toMillis() > timeoutMs) {
                System.out.println("  [Circuit Breaker: " + name + "] Half-open -> trying again");
                state = CircuitState.HALF_OPEN;
            } else {
                System.out.println("  [Circuit Breaker: " + name + "] OPEN -> using fallback");
                return fallback.get();
            }
        }

        try {
            T result = operation.get();
            if (state == CircuitState.HALF_OPEN) {
                System.out.println("  [Circuit Breaker: " + name + "] Half-open -> success, closing");
                state = CircuitState.CLOSED;
                failureCount.set(0);
            }
            return result;
        } catch (Exception e) {
            failureCount.incrementAndGet();
            lastFailureTime = Instant.now();
            System.out.println("  [Circuit Breaker: " + name + "] Failure #" + failureCount.get());
            if (failureCount.get() >= failureThreshold) {
                System.out.println("  [Circuit Breaker: " + name + "] Threshold reached -> OPEN");
                state = CircuitState.OPEN;
            }
            return fallback.get();
        }
    }

    public CircuitState getState() {
        return state;
    }
}

// =============================================
// 4. Distributed Tracing concept
// =============================================
class Span {
    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final String operationName;
    private final Instant startTime;
    private Instant endTime;

    public Span(String traceId, String spanId, String parentSpanId, String operationName) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.operationName = operationName;
        this.startTime = Instant.now();
    }

    public String spanId() { return spanId; }
    public String traceId() { return traceId; }
    public String parentSpanId() { return parentSpanId; }

    public void finish() {
        this.endTime = Instant.now();
        long duration = Duration.between(startTime, endTime).toMillis();
        System.out.println("  [Tracing] Span: " + operationName
                + " | traceId: " + traceId
                + " | spanId: " + spanId
                + " | parentSpanId: " + (parentSpanId != null ? parentSpanId : "root")
                + " | duration: " + duration + "ms");
    }
}

class Tracer {
    private static final AtomicInteger counter = new AtomicInteger(0);

    public Span startSpan(String operationName, String traceId, String parentSpanId) {
        String spanId = Integer.toHexString(counter.incrementAndGet());
        return new Span(traceId, spanId, parentSpanId, operationName);
    }

    public String generateTraceId() {
        return "trace-" + Integer.toHexString(counter.incrementAndGet());
    }
}

// =============================================
// 5. API Gateway concept
// =============================================
class ApiGateway {
    private final ServiceRegistry registry;
    private final CircuitBreaker circuitBreaker;
    private final Tracer tracer;

    public ApiGateway(ServiceRegistry registry, CircuitBreaker circuitBreaker, Tracer tracer) {
        this.registry = registry;
        this.circuitBreaker = circuitBreaker;
        this.tracer = tracer;
    }

    public String routeRequest(String serviceId, String path, String traceId) {
        var span = tracer.startSpan("gateway:" + serviceId + path, traceId, null);

        String result = circuitBreaker.call(
                () -> {
                    var instance = registry.getInstance(serviceId);
                    System.out.println("  [API Gateway] Routing to " + instance.serviceId()
                            + " at " + instance.host() + ":" + instance.port() + path);
                    // Simulate service call
                    if (new Random().nextInt(5) == 0) {
                        throw new RuntimeException("Service " + serviceId + " unavailable");
                    }
                    return "[Response from " + serviceId + " at " + instance.host() + ":" + instance.port() + "]";
                },
                () -> "[Fallback] " + serviceId + " is currently unavailable. Please try again later."
        );

        span.finish();
        return result;
    }
}

// Microservice simulation
record Microservice(String name, int port) {
    public String call(String path) {
        System.out.println("  [Microservice] " + name + " handling " + path);
        return "{\"service\":\"" + name + "\",\"status\":\"ok\",\"path\":\"" + path + "\"}";
    }
}

public class MicroservicesExample {
    public static void main(String[] args) {
        System.out.println("=== Microservices Patterns Demo ===");

        // Setup infrastructure
        var configServer = new ConfigServer();
        var registry = new ServiceRegistry();
        var circuitBreaker = new CircuitBreaker("backend-service", 3, 2000);
        var tracer = new Tracer();
        var gateway = new ApiGateway(registry, circuitBreaker, tracer);

        // 1. Config Server
        System.out.println("\n1. Config Server (Centralized Configuration):");
        configServer.printConfig("user-service");
        configServer.printConfig("order-service");

        // 2. Service Discovery
        System.out.println("\n2. Service Discovery (Eureka/Nacos):");
        registry.register("user-service", "192.168.1.10", 8081);
        registry.register("order-service", "192.168.1.11", 8082);
        registry.register("inventory-service", "192.168.1.12", 8083);
        System.out.println("  Discovered user-service instances: " + registry.getInstances("user-service"));

        // 3. Distributed Tracing
        System.out.println("\n3. Distributed Tracing (Zipkin/Jaeger):");
        String traceId = tracer.generateTraceId();
        var span1 = tracer.startSpan("user-service:getUser", traceId, null);
        // Simulate nested spans
        var span1_1 = tracer.startSpan("user-service:getUser:queryDB", traceId, span1.spanId());
        span1_1.finish();
        span1.finish();

        // 4. API Gateway routing
        System.out.println("\n4. API Gateway (Spring Cloud Gateway / Zuul):");
        String traceId2 = tracer.generateTraceId();
        System.out.println("  " + gateway.routeRequest("user-service", "/users/1", traceId2));
        System.out.println("  " + gateway.routeRequest("order-service", "/orders", traceId2));

        // 5. Circuit Breaker
        System.out.println("\n5. Circuit Breaker (Resilience4j / Hystrix):");
        // Simulate failures
        for (int i = 0; i < 5; i++) {
            String traceId3 = tracer.generateTraceId();
            System.out.println("  Request " + (i + 1) + ": "
                    + gateway.routeRequest("inventory-service", "/items", traceId3)
                    + " | CB State: " + circuitBreaker.getState());
        }

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("API Gateway - single entry point routing to microservices");
        System.out.println("Service Discovery - dynamic service location (Eureka/Consul)");
        System.out.println("Config Server - externalized configuration management (Spring Cloud Config)");
        System.out.println("Circuit Breaker - fault tolerance, preventing cascading failures (Resilience4j)");
        System.out.println("Distributed Tracing - request tracking across services (traceId/spanId, Zipkin)");
    }
}
