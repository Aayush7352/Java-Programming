package phase13.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// --- Demo: JWT token generation, parsing, validation, @AuthenticationPrincipal ---

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface AuthenticationPrincipal {}

// JWT Payload structure
record JwtPayload(String sub, String username, String role, long iat, long exp, Map<String, Object> claims) {
    static JwtPayload of(String sub, String username, String role, long ttlSeconds) {
        long now = Instant.now().getEpochSecond();
        return new JwtPayload(sub, username, role, now, now + ttlSeconds, Map.of());
    }
}

// Simple JWT implementation using Base64 HMAC-style signing (manual, for demo)
class JwtTokenProvider {
    private final String secretKey;
    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    public JwtTokenProvider(String secretKey) {
        this.secretKey = secretKey;
    }

    // Generate JWT token
    public String generateToken(JwtPayload payload) {
        String header = base64UrlEncode(HEADER);
        String payloadJson = """
                {"sub":"%s","username":"%s","role":"%s","iat":%d,"exp":%d}
                """.formatted(payload.sub(), payload.username(), payload.role(),
                payload.iat(), payload.exp()).replace("\n", "");
        String encodedPayload = base64UrlEncode(payloadJson);
        String signature = sign(header + "." + encodedPayload);
        return header + "." + encodedPayload + "." + signature;
    }

    // Parse and validate JWT token
    public JwtPayload parseToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new RuntimeException("Invalid JWT token format");
        }

        String header = parts[0];
        String encodedPayload = parts[1];
        String signature = parts[2];

        // Verify signature
        String expectedSignature = sign(header + "." + encodedPayload);
        if (!signature.equals(expectedSignature)) {
            throw new RuntimeException("Invalid JWT signature");
        }

        // Decode payload
        String payloadJson = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        // Simple JSON parsing (for demo purposes)
        String sub = extractJsonValue(payloadJson, "sub");
        String username = extractJsonValue(payloadJson, "username");
        String role = extractJsonValue(payloadJson, "role");
        long iat = Long.parseLong(extractJsonValue(payloadJson, "iat"));
        long exp = Long.parseLong(extractJsonValue(payloadJson, "exp"));

        // Check expiration
        if (Instant.now().getEpochSecond() > exp) {
            throw new RuntimeException("JWT token has expired");
        }

        return new JwtPayload(sub, username, role, iat, exp, Map.of());
    }

    // Extract simple JSON string value
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf('"', start);
            return json.substring(start, end);
        }
        // Number value
        int end = json.indexOf(',', start);
        if (end == -1) end = json.indexOf('}', start);
        return json.substring(start, end).trim();
    }

    // Base64URL encode
    private String base64UrlEncode(String data) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    // Simple HMAC-like signing (for demo purposes - not cryptographically secure)
    private String sign(String data) {
        String raw = data + "." + secretKey;
        byte[] hash = sha256(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    // Simple SHA-256 hash simulation (concept demonstration)
    // In real apps, use javax.crypto.Mac with HmacSHA256
    private byte[] sha256(String data) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            return md.digest(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // Fallback simple hash for demo
            byte[] result = new byte[32];
            byte[] input = data.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < 32; i++) {
                result[i] = (byte) (input[i % input.length] ^ (i * 31));
            }
            return result;
        }
    }
}

// Simulates a controller that extracts user from JWT via @AuthenticationPrincipal
class SecureController {

    // Simulates @GetMapping("/me") with @AuthenticationPrincipal UserDetails user
    public String getCurrentUser(@AuthenticationPrincipal String username) {
        return "{\"username\":\"" + username + "\",\"message\":\"Authenticated via JWT\"}";
    }

    // Simulates an admin-only endpoint
    public String adminOnly(@AuthenticationPrincipal String username, String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            return "{\"error\":\"Access denied. Admin role required.\"}";
        }
        return "{\"username\":\"" + username + "\",\"message\":\"Admin access granted\"}";
    }
}

// JWT Authentication Filter concept
class JwtAuthenticationFilter {
    private final JwtTokenProvider tokenProvider;
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>(); // token -> username

    public JwtAuthenticationFilter(String secretKey) {
        this.tokenProvider = new JwtTokenProvider(secretKey);
    }

    public String login(String username, String role) {
        var payload = JwtPayload.of(username, username, role, 3600); // 1 hour TTL
        String token = tokenProvider.generateToken(payload);
        tokenStore.put(token, username);
        System.out.println("  [JWT] Generated token for " + username + ": " + token);
        return token;
    }

    public JwtPayload authenticate(String token) {
        var payload = tokenProvider.parseToken(token);
        System.out.println("  [JWT] Validated token for " + payload.username()
                + " (role: " + payload.role() + ", expires: " + Instant.ofEpochSecond(payload.exp()) + ")");
        return payload;
    }

    public boolean isAuthenticated(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        try {
            var payload = authenticate(token);
            return tokenStore.containsKey(token) && tokenStore.get(token).equals(payload.username());
        } catch (Exception e) {
            return false;
        }
    }
}

public class JWTAuthentication {
    public static void main(String[] args) {
        System.out.println("=== JWT Authentication Demo ===");

        var secretKey = "my-very-secret-key-for-jwt-demo-2024";
        var jwtFilter = new JwtAuthenticationFilter(secretKey);
        var controller = new SecureController();

        // 1. Login and generate JWT tokens
        System.out.println("\n1. Login (Token Generation):");
        String adminToken = jwtFilter.login("admin", "ROLE_ADMIN");
        String userToken = jwtFilter.login("john_doe", "ROLE_USER");

        // 2. Parse and validate tokens
        System.out.println("\n2. Token Validation:");
        var adminPayload = jwtFilter.authenticate(adminToken);
        System.out.println("  Subject: " + adminPayload.sub());
        System.out.println("  Username: " + adminPayload.username());
        System.out.println("  Role: " + adminPayload.role());

        // 3. Access protected resources
        System.out.println("\n3. Access Control with @AuthenticationPrincipal:");
        System.out.println("  Admin accessing /me: "
                + controller.getCurrentUser(adminPayload.username()));
        System.out.println("  Admin accessing /admin: "
                + controller.adminOnly(adminPayload.username(), adminPayload.role()));
        System.out.println("  User accessing /admin: "
                + controller.adminOnly("john_doe", "ROLE_USER"));

        // 4. Authentication filter
        System.out.println("\n4. Authentication Filter:");
        System.out.println("  Request with valid admin token: "
                + jwtFilter.isAuthenticated("Bearer " + adminToken));
        System.out.println("  Request with invalid token: "
                + jwtFilter.isAuthenticated("Bearer invalid.token.here"));
        System.out.println("  Request without token: "
                + jwtFilter.isAuthenticated(null));

        // 5. Expired token demo
        System.out.println("\n5. Token Expiration:");
        var expiredTokenProvider = new JwtTokenProvider(secretKey);
        var expiredPayload = JwtPayload.of("expired", "expired_user", "ROLE_USER",
                -100); // already expired
        String expiredToken = expiredTokenProvider.generateToken(expiredPayload);
        try {
            jwtFilter.authenticate(expiredToken);
        } catch (Exception e) {
            System.out.println("  Expired token rejected: " + e.getMessage());
        }

        System.out.println("\n--- Concepts Demonstrated ---");
        System.out.println("JWT (JSON Web Token) - compact, self-contained token format");
        System.out.println("Token generation: header.payload.signature (Base64Url encoded)");
        System.out.println("Token parsing: decode payload, extract claims (sub, role, exp)");
        System.out.println("Token validation: signature verification, expiration check");
        System.out.println("@AuthenticationPrincipal - injects authenticated user into controller");
        System.out.println("Bearer Authentication scheme: 'Authorization: Bearer <token>'");
        System.out.println("io.jsonwebtoken (JJWT) - popular library for JWT in Spring Boot");
    }
}
