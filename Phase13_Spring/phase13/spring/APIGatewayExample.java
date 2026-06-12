package phase13.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// --- Demo: Spring Cloud Gateway: routes, filters (pre/post), @EnableZuulProxy or Gateway, route predicates ---

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface EnableZuulProxy {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface EnableGateway {}

// Route predicate: determines if a request matches a route
@FunctionalInterface
interface RoutePredicate {
    boolean matches(Map<String, String> request);
}

// Gateway filter (pre or post)
@FunctionalInterface
interface GatewayFilter {
    Map<String, String> apply(Map<String, String> request);
}

record PreFilter(String name, GatewayFilter filter) {}
record PostFilter(String name, GatewayFilter filter) {}

// Route definition
record RouteDefinition(
        String id,
        String uri,
        RoutePredicate predicate,
        List<PreFilter> preFilters,
        List<PostFilter> postFilters,
        int order
) {
    public RouteDefinition {
        preFilters = preFilters != null ? preFilters : List.of();
        postFilters = postFilters != null ? postFilters : List.of();
    }
}

// HTTP request/response models for gateway simulation
record GatewayHttpRequest(String method, String path, Map<String, String> headers, String body) {
    public Map<String, String> toMap() {
        var map = new HashMap<>(headers);
        map.put("method", method);
        map.put("path", path);
        map.put("body", body);
        return Collections.unmodifiableMap(map);
    }
}

record GatewayHttpResponse(int status, String body, Map<String, String> headers) {}

// Spring Cloud Gateway simulation
@EnableGateway
class SpringCloudGateway {
    private final List<RouteDefinition> routes = new ArrayList<>();
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();

    public SpringCloudGateway addRoute(RouteDefinition route) {
        routes.add(route);
        routes.sort(Comparator.comparingInt(RouteDefinition::order));
        return this;
    }

    public GatewayHttpResponse handleRequest(GatewayHttpRequest request) {
        var requestMap = request.toMap();
        System.out.println("  [Gateway] Incoming: " + request.method() + " " + request.path());

        // Find matching route
        var matchedRoute = routes.stream()
                .filter(r -> r.predicate().matches(requestMap))
                .findFirst();

        if (matchedRoute.isEmpty()) {
            System.out.println("  [Gateway] No matching route for " + request.path());
            return new GatewayHttpResponse(404, "{\"error\":\"No route found\"}", Map.of());
        }

        var route = matchedRoute.get();
        System.out.println("  [Gateway] Matched route: " + route.id() + " -> " + route.uri());

        // --- Pre-filters ---
        var currentRequest = new HashMap<>(requestMap);
        for (var preFilter : route.preFilters()) {
            System.out.println("  [PreFilter] " + preFilter.name());
            currentRequest.putAll(preFilter.filter().apply(currentRequest));
        }

        // --- Route to backend service (simulated) ---
        var backendResponse = callBackend(route.uri(), currentRequest);

        // --- Post-filters ---
        var currentResponse = new HashMap<>(backendResponse);
        for (var postFilter : route.postFilters()) {
            System.out.println("  [PostFilter] " + postFilter.name());
            currentResponse.putAll(postFilter.filter().apply(currentResponse));
        }

        return new GatewayHttpResponse(
                Integer.parseInt(currentResponse.getOrDefault("status", "200")),
                currentResponse.getOrDefault("body", ""),
                Map.of()
        );
    }

    private Map<String, String> callBackend(String uri, Map<String, String> request) {
        // Simulated backend call
        System.out.println("  [Backend] Routing to " + uri + request.get("path"));
        var response = new HashMap<String, String>();
        response.put("status", "200");
        response.put("body", "{\"from\":\"" + uri + "\",\"path\":\"" + request.get("path") + "\",\"data\":\"ok\"}");

        // Track request for rate limiting (simulated)
        requestCounts.merge(uri, 1, Integer::sum);
        lastRequestTime.put(uri, System.currentTimeMillis());

        return response;
    }
}

// Pre-built filters
class GatewayFilters {
    // Add a request header
    public static PreFilter addRequestHeader(String name, String value) {
        return new PreFilter("AddRequestHeader:" + name, req -> {
            req.put("X-" + name, value);
            return Map.of();
        });
    }

    // Rate limiting filter (pre)
    public static PreFilter rateLimiter(int maxRequests) {
        return new PreFilter("RateLimiter", req -> {
            // Simplified rate limiting logic
            return Map.of();
        });
    }

    // Authentication filter (pre)
    public static PreFilter authenticate() {
        return new PreFilter("Authentication", req -> {
            String auth = req.get("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                throw new RuntimeException("Authentication failed: missing or invalid token");
            }
            System.out.println("    [Auth] Token validated");
            return Map.of("X-Authenticated-User", "demo_user");
        });
    }

    // Add response header (post)
    public static PostFilter addResponseHeader(String name, String value) {
        return new PostFilter("AddResponseHeader:" + name, resp -> {
            resp.put("X-" + name, value);
            return Map.of();
        });
    }

    // Response transformation (post)
    public static PostFilter wrapResponse() {
        return new PostFilter("ResponseWrapper", resp -> {
            String body = resp.getOrDefault("body", "");
            body = "{\"gateway\":\"SpringCloudGateway\",\"response\":" + body + "}";
            resp.put("body", body);
            return Map.of();
        });
    }
}

// Route predicates
class RoutePredicates {
    // Path matching predicate
    public static RoutePredicate path(String pattern) {
        return req -> {
            String path = req.get("path");
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                return path.startsWith(prefix);
            }
            return path.equals(pattern);
        };
    }

    // Method predicate
    public static RoutePredicate method(String httpMethod) {
        return req -> httpMethod.equalsIgnoreCase(req.get("method"));
    }

    // Header predicate
    public static RoutePredicate header(String name, String regex) {
        return req -> {
            String value = req.get(name);
            return value != null && value.matches(regex);
        };
    }

    // Combined predicate (AND)
    public static RoutePredicate and(RoutePredicate... predicates) {
        return req -> Arrays.stream(predicates).allMatch(p -> p.matches(req));
    }
}

public class APIGatewayExample {
    public static void main(String[] args) {
        System.out.println("=== Spring Cloud Gateway Demo ===");

        var gateway = new SpringCloudGateway();

        // 1. Define routes with predicates and filters
        System.out.println("\n1. Route Definitions:");

        // Route: User Service
        gateway.addRoute(new RouteDefinition(
                "user-service",
                "http://user-service:8081",
                RoutePredicates.and(
                        RoutePredicates.path("/api/users/**"),
                        RoutePredicates.method("GET")
                ),
                List.of(
                        GatewayFilters.authenticate(),
                        GatewayFilters.addRequestHeader("X-Gateway", "SpringCloudGateway")
                ),
                List.of(
                        GatewayFilters.addResponseHeader("Response-Time", String.valueOf(System.currentTimeMillis())),
                        GatewayFilters.wrapResponse()
                ),
                1
        ));

        // Route: Order Service
        gateway.addRoute(new RouteDefinition(
                "order-service",
                "http://order-service:8082",
                RoutePredicates.and(
                        RoutePredicates.path("/api/orders/**"),
                        RoutePredicates.method("GET")
                ),
                List.of(
                        GatewayFilters.addRequestHeader("X-Source", "API-Gateway")
                ),
                List.of(
                        GatewayFilters.wrapResponse()
                ),
                2
        ));

        // Route: Public endpoint (no auth)
        gateway.addRoute(new RouteDefinition(
                "public-route",
                "http://public-service:9090",
                RoutePredicates.path("/public/**"),
                List.of(
                        GatewayFilters.rateLimiter(100)
                ),
                List.of(),
                0
        ));

        // 2. Test requests
        System.out.println("\n2. Test Requests:");

        var requests = List.of(
                new GatewayHttpRequest("GET", "/api/users/123",
                        Map.of("Authorization", "Bearer my-token", "Content-Type", "application/json"), ""),
                new GatewayHttpRequest("GET", "/api/orders",
                        Map.of("Authorization", "Bearer my-token"), ""),
                new GatewayHttpRequest("GET", "/public/health",
                        Map.of(), ""),
                new GatewayHttpRequest("GET", "/api/users/456",
                        Map.of(), "") // No auth -> should fail
        );

        for (var req : requests) {
            try {
                var response = gateway.handleRequest(req);
                System.out.println("  Response: " + response.status() + " " + response.body() + "\n");
            } catch (Exception e) {
                System.out.println("  Error: " + e.getMessage() + "\n");
            }
        }

        System.out.println("--- Concepts Demonstrated ---");
        System.out.println("@EnableZuulProxy / Spring Cloud Gateway - enables API Gateway functionality");
        System.out.println("Routes - mapping of requests to backend services (id + uri + predicate)");
        System.out.println("Route Predicates - conditions for matching requests (Path, Method, Header, etc.)");
        System.out.println("Pre-filters - executed before routing to backend (auth, rate limiting, header manipulation)");
        System.out.println("Post-filters - executed after backend response (response transformation, header injection)");
    }
}
