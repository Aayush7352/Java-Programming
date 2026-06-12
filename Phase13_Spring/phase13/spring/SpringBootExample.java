package phase13.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// --- Demo: @SpringBootApplication, @RestController, @GetMapping, embedded server, application.properties ---

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface SpringBootApplication {
    String scanBasePackages() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface RestController {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface BootGetMapping {
    String value() default "/";
}

// Mimics an embedded HTTP server
record BootHttpRequest(String method, String path, Map<String, String> headers) {}
record BootHttpResponse(int status, String body) {}

// Simple embedded server simulation
class EmbeddedServer {
    private final Map<String, java.util.function.Function<BootHttpRequest, BootHttpResponse>> handlers = new ConcurrentHashMap<>();

    public void get(String path, java.util.function.Function<BootHttpRequest, BootHttpResponse> handler) {
        handlers.put("GET:" + path, handler);
    }

    public BootHttpResponse handle(BootHttpRequest request) {
        var handler = handlers.get(request.method() + ":" + request.path());
        if (handler == null) {
            return new BootHttpResponse(404, "Not Found");
        }
        return handler.apply(request);
    }

    public void start(int port) {
        System.out.println("[Embedded Server] Starting on port " + port + "...");
        System.out.println("[Embedded Server] Server is running. Press Ctrl+C to stop.");
    }
}

// Loads application.properties concepts
class ApplicationProperties {
    private final Map<String, String> props = new ConcurrentHashMap<>();

    public ApplicationProperties() {
        // Simulating reading from application.properties / application.yml
        props.put("server.port", "8080");
        props.put("spring.application.name", "SpringBootDemo");
        props.put("logging.level.root", "INFO");
        props.put("datasource.url", "jdbc:h2:mem:testdb");
    }

    public String getProperty(String key) {
        return props.get(key);
    }

    public void printAll() {
        System.out.println("[application.properties] Loaded properties:");
        props.forEach((k, v) -> System.out.println("  " + k + " = " + v));
    }
}

// A sample @RestController
@RestController
class HelloController {

    @BootGetMapping("/hello")
    public BootHttpResponse hello(BootHttpRequest request) {
        return new BootHttpResponse(200, "{\"message\": \"Hello from Spring Boot!\"}");
    }

    @BootGetMapping("/health")
    public BootHttpResponse health(BootHttpRequest request) {
        return new BootHttpResponse(200, "{\"status\": \"UP\"}");
    }
}

@SpringBootApplication(scanBasePackages = "phase13.spring")
public class SpringBootExample {

    // Mimicking @SpringBootApplication auto-configuration
    public static void run(Class<?> source, String[] args) {
        System.out.println("[@SpringBootApplication] Running " + source.getSimpleName());
        System.out.println("[@SpringBootApplication] Auto-configuration in progress...");

        // Load application.properties
        var props = new ApplicationProperties();
        props.printAll();

        // Start embedded server
        var server = new EmbeddedServer();
        var controller = new HelloController();

        // Register endpoints based on @BootGetMapping annotations
        for (var method : controller.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(BootGetMapping.class)) {
                var mapping = method.getAnnotation(BootGetMapping.class);
                server.get(mapping.value(), req -> {
                    try {
                        return (BootHttpResponse) method.invoke(controller, req);
                    } catch (Exception e) {
                        return new BootHttpResponse(500, "Internal Server Error");
                    }
                });
                System.out.println("[@BootGetMapping] Mapped GET " + mapping.value());
            }
        }

        int port = Integer.parseInt(props.getProperty("server.port"));
        server.start(port);

        // Demo request handling
        System.out.println("\n--- Demo Requests ---");
        var req1 = new BootHttpRequest("GET", "/hello", Map.of());
        System.out.println("GET /hello -> " + server.handle(req1));

        var req2 = new BootHttpRequest("GET", "/health", Map.of());
        System.out.println("GET /health -> " + server.handle(req2));

        var req3 = new BootHttpRequest("GET", "/unknown", Map.of());
        System.out.println("GET /unknown -> " + server.handle(req3));
    }

    public static void main(String[] args) {
        run(SpringBootExample.class, args);
        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("@SpringBootApplication - entry point, auto-configuration, component scan");
        System.out.println("@RestController - defines REST controller (combination of @Controller + @ResponseBody)");
        System.out.println("@GetMapping (BootGetMapping) - maps HTTP GET requests to handler methods");
        System.out.println("Embedded server concept (Tomcat/Jetty/Undertow) - runs app without external deployment");
        System.out.println("application.properties - externalized configuration (server.port, datasource, etc.)");
    }
}
