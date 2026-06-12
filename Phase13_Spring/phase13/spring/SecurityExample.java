package phase13.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// --- Demo: Spring Security concepts, SecurityFilterChain, @EnableWebSecurity, UserDetailsService, BCryptPasswordEncoder ---

// Annotations
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface EnableWebSecurity {}

// User details
record UserDetails(String username, String password, List<String> authorities, boolean enabled) {
    public static UserDetails of(String username, String password, String... roles) {
        return new UserDetails(username, password, List.of(roles), true);
    }
}

// UserDetailsService interface
@FunctionalInterface
interface UserDetailsService {
    UserDetails loadUserByUsername(String username);
}

// Simple BCrypt-like password encoder (not actual BCrypt, just concept)
class BCryptPasswordEncoder {
    // Simulates BCrypt hashing with a simple reversible salt-hash
    public String encode(CharSequence rawPassword) {
        // In real Spring Security, this uses BCrypt strong hashing
        return "{bcrypt}" + rawPassword.toString().hashCode();
    }

    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.startsWith("{bcrypt}")) {
            return false;
        }
        String storedHash = encodedPassword.substring(8);
        return Integer.toString(rawPassword.toString().hashCode()).equals(storedHash);
    }
}

// Security filter chain entry (mimics SecurityFilterChain)
@FunctionalInterface
interface SecurityFilter {
    boolean apply(SecHttpRequest request);
}

record SecurityFilterChain(List<SecurityFilter> filters, boolean matchesRequest) {}

// HTTP request/response models for demo
record SecHttpRequest(String method, String path, String authHeader) {}
record SecHttpResponse(int status, String body) {}

// Mimics a security context holder
class SecurityContextHolder {
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    public static void setCurrentUser(String username) {
        currentUser.set(username);
    }

    public static String getCurrentUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}

// Security configuration builder
@EnableWebSecurity
class SecurityConfig {
    private final List<SecurityFilter> filters = new ArrayList<>();
    private UserDetailsService userDetailsService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public SecurityConfig userDetailsService(UserDetailsService service) {
        this.userDetailsService = service;
        return this;
    }

    public SecurityConfig addFilter(SecurityFilter filter) {
        filters.add(filter);
        return this;
    }

    public List<SecurityFilterChain> buildFilterChains() {
        // Chain 1: Public endpoints (no auth)
        SecurityFilter publicFilter = req -> req.path().startsWith("/public") || req.path().equals("/login");
        var publicFilters = List.of(publicFilter);

        // Chain 2: Protected endpoints (with authentication)
        var authFilter = new SecurityFilter() {
            @Override
            public boolean apply(SecHttpRequest request) {
                String auth = request.authHeader();
                if (auth == null || auth.isBlank()) {
                    System.out.println("  [SecurityFilterChain] No auth header - rejecting");
                    return false;
                }
                // Basic Base64-like parsing: "Basic <base64>"
                if (auth.startsWith("Basic ")) {
                    String credentials = auth.substring(6);
                    // In real app: decode Base64, split by ":"
                    String username = "user"; // simplified
                    String password = credentials; // simplified
                    try {
                        UserDetails user = userDetailsService.loadUserByUsername(username);
                        if (passwordEncoder.matches(password, user.password())) {
                            SecurityContextHolder.setCurrentUser(username);
                            System.out.println("  [SecurityFilterChain] Authenticated: " + username);
                            return true;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
                return false;
            }
        };

        SecurityFilter protectedPathFilter = req -> !req.path().startsWith("/public") && !req.path().equals("/login");
        var protectedFilters = List.<SecurityFilter>of(
                protectedPathFilter,
                authFilter
        );

        return List.of(
                new SecurityFilterChain(publicFilters, true),
                new SecurityFilterChain(protectedFilters, true)
        );
    }

    public BCryptPasswordEncoder passwordEncoder() {
        return passwordEncoder;
    }
}

// Simple security filter chain executor
class SecurityFilterChainExecutor {
    private final List<SecurityFilterChain> chains;
    private final SecurityConfig config;

    public SecurityFilterChainExecutor(SecurityConfig config) {
        this.config = config;
        this.chains = config.buildFilterChains();
    }

    public boolean authenticate(SecHttpRequest request) {
        for (var chain : chains) {
            boolean allPass = true;
            for (var filter : chain.filters()) {
                if (!filter.apply(request)) {
                    allPass = false;
                    break;
                }
            }
            if (allPass) {
                return true;
            }
        }
        return false;
    }
}

// In-memory UserDetailsService
class InMemoryUserDetailsService implements UserDetailsService {
    private final List<UserDetails> users = new ArrayList<>();

    public InMemoryUserDetailsService(BCryptPasswordEncoder encoder) {
        users.add(UserDetails.of("admin", encoder.encode("admin123"), "ROLE_ADMIN", "ROLE_USER"));
        users.add(UserDetails.of("user", encoder.encode("user123"), "ROLE_USER"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.stream()
                .filter(u -> u.username().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}

public class SecurityExample {
    public static void main(String[] args) {
        System.out.println("=== Spring Security Demo ===");

        // Configure security
        var encoder = new BCryptPasswordEncoder();
        var securityConfig = new SecurityConfig()
                .userDetailsService(new InMemoryUserDetailsService(encoder));

        var executor = new SecurityFilterChainExecutor(securityConfig);

        // Show password encoding
        System.out.println("\n--- BCryptPasswordEncoder ---");
        String rawPassword = "admin123";
        String encoded = encoder.encode(rawPassword);
        System.out.println("  Raw: " + rawPassword);
        System.out.println("  Encoded: " + encoded);
        System.out.println("  Matches: " + encoder.matches(rawPassword, encoded));
        System.out.println("  Wrong password matches: " + encoder.matches("wrong", encoded));

        // Test requests
        System.out.println("\n--- Security Filter Chain ---");

        var requests = List.of(
                new SecHttpRequest("GET", "/public/health", null),
                new SecHttpRequest("GET", "/login", null),
                new SecHttpRequest("GET", "/api/users", null),
                new SecHttpRequest("GET", "/api/users", "Basic admin123"),
                new SecHttpRequest("GET", "/api/admin", "Basic invalid"),
                new SecHttpRequest("GET", "/public/about", null)
        );

        for (var req : requests) {
            System.out.println("\n" + req.method() + " " + req.path());
            if (req.authHeader() != null) {
                System.out.println("  Auth: " + req.authHeader());
            }
            boolean authenticated = executor.authenticate(req);
            System.out.println("  Result: " + (authenticated ? "GRANTED" : "DENIED"));
            SecurityContextHolder.clear();
        }

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("@EnableWebSecurity - enables Spring Security's web security support");
        System.out.println("SecurityFilterChain - defines a chain of security filters");
        System.out.println("UserDetailsService - loads user-specific data for authentication");
        System.out.println("BCryptPasswordEncoder - one-way password hashing (strong hash)");
        System.out.println("SecurityContextHolder - stores security context for current thread");
        System.out.println("Authentication filter chain: public endpoints vs protected endpoints");
    }
}
