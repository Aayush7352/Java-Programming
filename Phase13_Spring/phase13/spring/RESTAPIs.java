package phase13.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

// --- Demo: @RestController, @RequestMapping, @PathVariable, @RequestParam, @RequestBody, ResponseEntity, HTTP methods ---

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface RestControllerAnn {}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface RequestMapping {
    String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface PathVariable {
    String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface RequestParam {
    String value() default "";
    String defaultValue() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface RequestBody {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface GetMapping {
    String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface PostMapping {
    String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface PutMapping {
    String value() default "";
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface DeleteMapping {
    String value() default "";
}

// HTTP response with status code
record ResponseEntity<T>(int statusCode, T body, Map<String, String> headers) {
    public static <T> ResponseEntity<T> ok(T body) {
        return new ResponseEntity<>(200, body, Map.of());
    }

    public static <T> ResponseEntity<T> status(int statusCode, T body) {
        return new ResponseEntity<>(statusCode, body, Map.of());
    }

    public static <T> ResponseEntity<T> created(T body) {
        return new ResponseEntity<>(201, body, Map.of());
    }

    public static <T> ResponseEntity<T> noContent() {
        return new ResponseEntity<>(204, null, Map.of());
    }
}

// Simple HTTP request model
record HttpRequest(String method, String path, Map<String, String> queryParams, String body) {}

// In-memory data store
record User(Long id, String name, String email) {}

@RestControllerAnn
@RequestMapping("/api/users")
class UserController {

    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    @GetMapping
    public ResponseEntity<String> getAllUsers(@RequestParam(value = "page", defaultValue = "0") int page) {
        System.out.println("  [@RequestParam] page = " + page);
        var json = users.values().stream()
                .map(u -> "{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\"}".formatted(u.id(), u.name(), u.email()))
                .toList()
                .toString();
        return ResponseEntity.ok(json);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getUserById(@PathVariable("id") Long id) {
        System.out.println("  [@PathVariable] id = " + id);
        var user = users.get(id);
        if (user == null) {
            return ResponseEntity.status(404, "{\"error\":\"User not found\"}");
        }
        return ResponseEntity.ok("{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\"}".formatted(
                user.id(), user.name(), user.email()));
    }

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody User user) {
        System.out.println("  [@RequestBody] user = " + user);
        var newUser = new User(nextId++, user.name(), user.email());
        users.put(newUser.id(), newUser);
        return ResponseEntity.created("{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\"}".formatted(
                newUser.id(), newUser.name(), newUser.email()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable("id") Long id, @RequestBody User user) {
        System.out.println("  [@PathVariable + @RequestBody] id = " + id + ", user = " + user);
        if (!users.containsKey(id)) {
            return ResponseEntity.status(404, "{\"error\":\"User not found\"}");
        }
        var updated = new User(id, user.name(), user.email());
        users.put(id, updated);
        return ResponseEntity.ok("{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\"}".formatted(
                updated.id(), updated.name(), updated.email()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable("id") Long id) {
        System.out.println("  [@PathVariable] delete id = " + id);
        if (users.remove(id) == null) {
            return ResponseEntity.status(404, "{\"error\":\"User not found\"}");
        }
        return ResponseEntity.noContent();
    }
}

// Simple REST API router to demonstrate the concept
class RestApiRouter {
    private final UserController controller = new UserController();

    public ResponseEntity<String> route(HttpRequest request) {
        String path = request.path();
        String method = request.method();

        System.out.println("\n--- Incoming Request: " + method + " " + path + " ---");

        return switch (method + " " + extractBasePath(path)) {
            case "GET /api/users" -> {
                int page = 0;
                if (request.queryParams().containsKey("page")) {
                    page = Integer.parseInt(request.queryParams().get("page"));
                }
                final int p = page;
                // Simulating @RequestParam via method invocation
                try {
                    var m = UserController.class.getMethod("getAllUsers", int.class);
                    yield (ResponseEntity<String>) m.invoke(controller, p);
                } catch (Exception e) {
                    yield ResponseEntity.status(500, "{\"error\":\"Internal error\"}");
                }
            }
            case "POST /api/users" -> controller.createUser(
                    new User(0L, "John Doe", "john@example.com"));
            default -> {
                // Check for path with {id}
                if (method.equals("GET") && path.matches("/api/users/\\d+")) {
                    Long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
                    yield controller.getUserById(id);
                } else if (method.equals("PUT") && path.matches("/api/users/\\d+")) {
                    Long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
                    yield controller.updateUser(id, new User(id, "Updated", "updated@example.com"));
                } else if (method.equals("DELETE") && path.matches("/api/users/\\d+")) {
                    Long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
                    yield controller.deleteUser(id);
                } else {
                    yield ResponseEntity.status(404, "{\"error\":\"Not Found\"}");
                }
            }
        };
    }

    private String extractBasePath(String path) {
        int idx = path.indexOf('?');
        return idx == -1 ? path : path.substring(0, idx);
    }
}

public class RESTAPIs {
    public static void main(String[] args) {
        System.out.println("=== REST APIs Demo ===");

        var router = new RestApiRouter();

        // Test requests
        var requests = new HttpRequest[]{
                new HttpRequest("GET", "/api/users", Map.of("page", "1"), ""),
                new HttpRequest("POST", "/api/users", Map.of(), "{\"name\":\"Alice\",\"email\":\"alice@test.com\"}"),
                new HttpRequest("GET", "/api/users/1", Map.of(), ""),
                new HttpRequest("GET", "/api/users", Map.of("page", "0"), ""),
                new HttpRequest("PUT", "/api/users/1", Map.of(), "{\"name\":\"Alice Updated\",\"email\":\"alice@new.com\"}"),
                new HttpRequest("DELETE", "/api/users/1", Map.of(), ""),
                new HttpRequest("GET", "/api/users/1", Map.of(), ""),
                new HttpRequest("GET", "/api/unknown", Map.of(), "")
        };

        for (var req : requests) {
            var response = router.route(req);
            System.out.println("Response: " + response.statusCode() + " " + response.body());
        }

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("@RestController - defines a RESTful controller (specialization of @Component)");
        System.out.println("@RequestMapping(\"/api/users\") - class-level URL mapping");
        System.out.println("@PathVariable - extracts values from URI path segments");
        System.out.println("@RequestParam - extracts query parameters");
        System.out.println("@RequestBody - binds HTTP request body to a Java object");
        System.out.println("ResponseEntity - full HTTP response (status, headers, body)");
        System.out.println("HTTP methods: @GetMapping, @PostMapping, @PutMapping, @DeleteMapping");
    }
}
