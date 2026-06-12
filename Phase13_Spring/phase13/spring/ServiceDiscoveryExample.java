package phase13.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// --- Demo: Eureka: @EnableEurekaServer, @EnableEurekaClient, ServiceRegistry,
//            load-balanced RestTemplate, @LoadBalanced ---

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface EnableEurekaServer {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface EnableEurekaClient {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface LoadBalanced {}

// =============================================
// Eureka Service Instance
// =============================================
record EurekaInstance(
        String instanceId,
        String serviceId,
        String host,
        int port,
        String ipAddr,
        String status,  // UP, DOWN, STARTING
        Map<String, String> metadata
) {}

// =============================================
// Eureka Server (Service Registry)
// =============================================
@EnableEurekaServer
class EurekaServer {
    private final Map<String, CopyOnWriteArrayList<EurekaInstance>> registry = new ConcurrentHashMap<>();
    private final AtomicInteger instanceCounter = new AtomicInteger(0);
    private static final long HEARTBEAT_TIMEOUT_MS = 30000;

    // Register a service instance
    public EurekaInstance registerInstance(String serviceId, String host, int port) {
        String instanceId = serviceId + ":" + host + ":" + port + ":" + instanceCounter.incrementAndGet();
        var instance = new EurekaInstance(
                instanceId, serviceId, host, port,
                "192.168.1." + (new Random().nextInt(255) + 1),
                "UP",
                Map.of("version", "1.0", "region", "us-east-1")
        );
        registry.computeIfAbsent(serviceId, k -> new CopyOnWriteArrayList<>()).add(instance);
        System.out.println("  [Eureka Server] Registered: " + instanceId + " (" + serviceId + ")");
        return instance;
    }

    // Renew lease (heartbeat)
    public boolean renew(String instanceId) {
        for (var entry : registry.entrySet()) {
            for (var inst : entry.getValue()) {
                if (inst.instanceId().equals(instanceId)) {
                    System.out.println("  [Eureka Server] Heartbeat received from " + instanceId);
                    return true;
                }
            }
        }
        return false;
    }

    // Get all instances for a service
    public List<EurekaInstance> getInstances(String serviceId) {
        var instances = registry.get(serviceId);
        if (instances == null || instances.isEmpty()) {
            return List.of();
        }
        return List.copyOf(instances);
    }

    // Get single instance (simple round-robin)
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    public EurekaInstance getNextInstance(String serviceId) {
        var instances = getInstances(serviceId);
        if (instances.isEmpty()) {
            throw new RuntimeException("No available instances for " + serviceId);
        }
        int idx = roundRobinCounters
                .computeIfAbsent(serviceId, k -> new AtomicInteger(0))
                .getAndIncrement() % instances.size();
        return instances.get(idx);
    }

    // Deregister
    public void deregister(String instanceId) {
        registry.forEach((serviceId, instances) -> {
            instances.removeIf(inst -> inst.instanceId().equals(instanceId));
        });
        System.out.println("  [Eureka Server] Deregistered: " + instanceId);
    }

    // Print registry
    public void printRegistry() {
        System.out.println("\n  [Eureka Server] Current Registry:");
        registry.forEach((serviceId, instances) -> {
            System.out.println("    " + serviceId + ":");
            instances.forEach(inst -> System.out.println("      - " + inst.instanceId()
                    + " (" + inst.host() + ":" + inst.port() + ") [" + inst.status() + "]"));
        });
    }
}

// =============================================
// Eureka Client
// =============================================
@EnableEurekaClient
class EurekaClient {
    private final String serviceId;
    private final String host;
    private final int port;
    private final EurekaServer server;
    private EurekaInstance registeredInstance;
    private boolean running = true;

    public EurekaClient(String serviceId, String host, int port, EurekaServer server) {
        this.serviceId = serviceId;
        this.host = host;
        this.port = port;
        this.server = server;
    }

    public void start() {
        System.out.println("  [Eureka Client] Starting " + serviceId + "...");
        registeredInstance = server.registerInstance(serviceId, host, port);
        // Start heartbeat thread (simulated)
        new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(10000); // Heartbeat every 10 seconds
                    server.renew(registeredInstance.instanceId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "heartbeat-" + serviceId).start();
    }

    public void stop() {
        running = false;
        if (registeredInstance != null) {
            server.deregister(registeredInstance.instanceId());
        }
    }

    public EurekaInstance getInstance() {
        return registeredInstance;
    }
}

// =============================================
// Load-balanced RestTemplate concept
// =============================================
@FunctionalInterface
interface RestTemplate {
    String getForObject(String url, Class<String> responseType);
}

class LoadBalancedRestTemplate implements RestTemplate {
    private final EurekaServer discoveryServer;

    public LoadBalancedRestTemplate(EurekaServer discoveryServer) {
        this.discoveryServer = discoveryServer;
    }

    @Override
    public String getForObject(String url, Class<String> responseType) {
        // Parse URL like: http://USER-SERVICE/api/users/1
        // Extract service name from URL
        String serviceName = extractServiceName(url);
        String path = extractPath(url);

        // Discover service instance
        var instance = discoveryServer.getNextInstance(serviceName);
        String actualUrl = "http://" + instance.host() + ":" + instance.port() + path;

        System.out.println("  [@LoadBalanced RestTemplate] Resolved: " + url);
        System.out.println("    -> " + actualUrl + " (instance: " + instance.instanceId() + ")");

        // Simulate HTTP call
        return "{\"from\":\"" + instance.serviceId() + "\",\"instance\":\""
                + instance.instanceId() + "\",\"path\":\"" + path + "\",\"status\":\"ok\"}";
    }

    private String extractServiceName(String url) {
        // Extract from http://SERVICE-NAME/path
        String withoutProtocol = url.substring(url.indexOf("://") + 3);
        return withoutProtocol.substring(0, withoutProtocol.indexOf('/'));
    }

    private String extractPath(String url) {
        String withoutProtocol = url.substring(url.indexOf("://") + 3);
        int slashIndex = withoutProtocol.indexOf('/');
        return slashIndex >= 0 ? withoutProtocol.substring(slashIndex) : "/";
    }
}

// =============================================
// Demo Services (microservices using Eureka)
// =============================================
class UserService {
    private final EurekaClient eurekaClient;

    public UserService(EurekaServer eurekaServer) {
        this.eurekaClient = new EurekaClient("USER-SERVICE", "localhost", 8081, eurekaServer);
    }

    public void start() {
        eurekaClient.start();
    }

    public String getUsers() {
        return "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]";
    }
}

class OrderService {
    private final EurekaClient eurekaClient;
    private final LoadBalancedRestTemplate restTemplate;

    public OrderService(EurekaServer eurekaServer) {
        this.eurekaClient = new EurekaClient("ORDER-SERVICE", "localhost", 8082, eurekaServer);
        this.restTemplate = new LoadBalancedRestTemplate(eurekaServer);
    }

    public void start() {
        eurekaClient.start();
    }

    public String getOrdersWithUsers() {
        // Use load-balanced RestTemplate to call User Service
        System.out.println("\n  [Order Service] Calling User Service via LoadBalanced RestTemplate...");
        String users = restTemplate.getForObject("http://USER-SERVICE/api/users", String.class);
        String orders = "[{\"id\":1,\"item\":\"Laptop\",\"userId\":1}]";
        return "{\"orders\":" + orders + ",\"users\":" + users + "}";
    }
}

@EnableEurekaServer
public class ServiceDiscoveryExample {

    @LoadBalanced
    public static LoadBalancedRestTemplate loadBalancedRestTemplate(EurekaServer server) {
        return new LoadBalancedRestTemplate(server);
    }

    public static void main(String[] args) {
        System.out.println("=== Service Discovery with Eureka Demo ===");

        // 1. Start Eureka Server
        System.out.println("\n1. Starting Eureka Server...");
        var eurekaServer = new EurekaServer();
        System.out.println("  [@EnableEurekaServer] Eureka Server is running on port 8761");

        // 2. Start Eureka Clients (microservices)
        System.out.println("\n2. Starting Microservices (Eureka Clients):");
        var userService = new UserService(eurekaServer);
        userService.start();

        var orderService = new OrderService(eurekaServer);
        orderService.start();

        // 3. Print registry
        eurekaServer.printRegistry();

        // 4. Service Discovery and Load Balancing
        System.out.println("\n3. Service Discovery in Action:");
        System.out.println("  Discovering USER-SERVICE instances:");
        var userInstances = eurekaServer.getInstances("USER-SERVICE");
        userInstances.forEach(inst ->
                System.out.println("    Found: " + inst.instanceId() + " at " + inst.host() + ":" + inst.port()));

        // 5. Load-balanced RestTemplate
        System.out.println("\n4. @LoadBalanced RestTemplate:");
        var loadBalanced = loadBalancedRestTemplate(eurekaServer);

        // Multiple calls to demonstrate round-robin
        for (int i = 0; i < 3; i++) {
            System.out.println("  Call " + (i + 1) + ": " + loadBalanced.getForObject(
                    "http://USER-SERVICE/api/users/" + (i + 1), String.class));
        }

        // 6. Service-to-service communication
        System.out.println("\n5. Service-to-Service Communication:");
        String result = orderService.getOrdersWithUsers();
        System.out.println("  Result: " + result);

        // 7. Add another instance of User Service
        System.out.println("\n6. Scaling Up - Adding another User Service instance:");
        var userService2 = new EurekaClient("USER-SERVICE", "localhost", 8083, eurekaServer);
        userService2.start();
        eurekaServer.printRegistry();

        // Show load balancing across instances
        System.out.println("\n7. Load Balancing across " + eurekaServer.getInstances("USER-SERVICE").size() + " instances:");
        for (int i = 0; i < 4; i++) {
            System.out.println("  Call " + (i + 1) + ": " + loadBalanced.getForObject(
                    "http://USER-SERVICE/api/items/" + (i + 1), String.class));
        }

        // Cleanup
        userService2.stop();

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("@EnableEurekaServer - turns the application into a Eureka Service Registry");
        System.out.println("@EnableEurekaClient - makes the application a Eureka client (registers itself)");
        System.out.println("ServiceRegistry - central registry of all available service instances");
        System.out.println("@LoadBalanced RestTemplate - client-side load balancing across instances");
        System.out.println("Round-robin load balancing - distributing requests across multiple instances");
        System.out.println("Heartbeat / Renew - periodic health checks to maintain registry");
    }
}
