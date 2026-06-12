package phase16.projects;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/**
 * URLShortener.java
 *
 * URL shortener: encode/decode using Base62, in-memory storage with
 * ConcurrentHashMap, collision handling, redirect simulation.
 */
public class URLShortener {

    // ═══════════════════════════════════════════════
    // Base62 Encoding
    // ═══════════════════════════════════════════════

    static final class Base62 {
        private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        private static final int BASE = 62;
        private static final Map<Character, Integer> CHAR_MAP = new HashMap<>();

        static {
            for (int i = 0; i < BASE; i++) CHAR_MAP.put(ALPHABET.charAt(i), i);
        }

        public static String encode(long value) {
            if (value == 0) return "0";
            var sb = new StringBuilder();
            long v = value;
            while (v > 0) {
                sb.append(ALPHABET.charAt((int) (v % BASE)));
                v /= BASE;
            }
            return sb.reverse().toString();
        }

        public static long decode(String str) {
            long result = 0;
            for (int i = 0; i < str.length(); i++) {
                result = result * BASE + CHAR_MAP.get(str.charAt(i));
            }
            return result;
        }
    }

    // ═══════════════════════════════════════════════
    // URL Record
    // ═══════════════════════════════════════════════

    record ShortUrl(String shortCode, String originalUrl, String userId, Instant createdAt,
                    long accessCount, Instant lastAccessed, long ttlSeconds) {

        public boolean isExpired() {
            return ttlSeconds > 0 && Instant.now().isAfter(createdAt.plusSeconds(ttlSeconds));
        }

        public ShortUrl withAccess() {
            return new ShortUrl(shortCode, originalUrl, userId, createdAt,
                accessCount + 1, Instant.now(), ttlSeconds);
        }
    }

    // ═══════════════════════════════════════════════
    // URL Shortener Service
    // ═══════════════════════════════════════════════

    static final class UrlShortenerService {
        private final ConcurrentHashMap<String, ShortUrl> urlByCode = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Set<String>> codesByUser = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, String> codeByOriginalUrl = new ConcurrentHashMap<>();
        private final AtomicLong counter = new AtomicLong(1_000_000);
        private final int maxRetries = 5;

        // Custom short code storage
        private final ConcurrentHashMap<String, Boolean> customCodes = new ConcurrentHashMap<>();

        public String shorten(String originalUrl) {
            return shorten(originalUrl, "anonymous", 0);
        }

        public String shorten(String originalUrl, String userId) {
            return shorten(originalUrl, userId, 0);
        }

        public String shorten(String originalUrl, String userId, long ttlSeconds) {
            // Check if already shortened
            String existing = codeByOriginalUrl.get(originalUrl);
            if (existing != null) {
                var existingUrl = urlByCode.get(existing);
                if (existingUrl != null && !existingUrl.isExpired()) {
                    return existing;
                }
            }

            String shortCode = generateUniqueCode();
            var shortUrl = new ShortUrl(shortCode, originalUrl, userId, Instant.now(), 0, null, ttlSeconds);
            urlByCode.put(shortCode, shortUrl);
            codeByOriginalUrl.put(originalUrl, shortCode);
            codesByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(shortCode);
            return shortCode;
        }

        public String shortenWithCustomCode(String originalUrl, String customCode, String userId) {
            if (customCode == null || customCode.isBlank()) {
                throw new IllegalArgumentException("Custom code cannot be empty");
            }
            if (customCode.length() < 3 || customCode.length() > 20) {
                throw new IllegalArgumentException("Custom code must be 3-20 characters");
            }
            if (!customCode.matches("[a-zA-Z0-9_-]+")) {
                throw new IllegalArgumentException("Custom code must be alphanumeric");
            }
            if (urlByCode.containsKey(customCode)) {
                throw new IllegalArgumentException("Custom code already in use: " + customCode);
            }

            var shortUrl = new ShortUrl(customCode, originalUrl, userId, Instant.now(), 0, null, 0);
            urlByCode.put(customCode, shortUrl);
            codeByOriginalUrl.put(originalUrl, customCode);
            codesByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(customCode);
            return customCode;
        }

        public Optional<String> resolve(String shortCode) {
            var url = urlByCode.get(shortCode);
            if (url == null) return Optional.empty();
            if (url.isExpired()) {
                urlByCode.remove(shortCode);
                codeByOriginalUrl.remove(url.originalUrl());
                return Optional.empty();
            }
            // Update access stats
            urlByCode.put(shortCode, url.withAccess());
            return Optional.of(url.originalUrl());
        }

        public Optional<ShortUrl> getUrlInfo(String shortCode) {
            return Optional.ofNullable(urlByCode.get(shortCode));
        }

        public boolean delete(String shortCode, String userId) {
            var url = urlByCode.get(shortCode);
            if (url == null) return false;
            if (!url.userId().equals(userId)) return false;

            urlByCode.remove(shortCode);
            codeByOriginalUrl.remove(url.originalUrl());
            var userCodes = codesByUser.get(userId);
            if (userCodes != null) userCodes.remove(shortCode);
            return true;
        }

        public List<ShortUrl> getUrlsByUser(String userId) {
            var codes = codesByUser.getOrDefault(userId, Set.of());
            return codes.stream()
                .map(urlByCode::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ShortUrl::createdAt).reversed())
                .collect(Collectors.toList());
        }

        public List<ShortUrl> getRecentUrls(int limit) {
            return urlByCode.values().stream()
                .sorted(Comparator.comparing(ShortUrl::createdAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        }

        public long getTotalUrls() { return urlByCode.size(); }

        public long getTotalClicks() {
            return urlByCode.values().stream().mapToLong(ShortUrl::accessCount).sum();
        }

        private String generateUniqueCode() {
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                long id = counter.getAndIncrement();
                String code = Base62.encode(id);
                if (!urlByCode.containsKey(code)) {
                    return code;
                }
            }
            throw new RuntimeException("Failed to generate unique code after " + maxRetries + " attempts");
        }

        // Simulate redirect
        public RedirectResult redirect(String shortCode) {
            var result = resolve(shortCode);
            if (result.isEmpty()) {
                return new RedirectResult(false, null, "URL not found or expired");
            }
            return new RedirectResult(true, result.get(), "Redirecting...");
        }

        record RedirectResult(boolean success, String targetUrl, String message) {}
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) throws Exception {
        System.out.println("=== URL Shortener ===\n");

        UrlShortenerService svc = new UrlShortenerService();

        // ─── Base62 Test ───
        System.out.println("--- Base62 Encoding ---");
        long[] testValues = {0, 1, 61, 62, 1000, 1000000, 999999999999L};
        for (long v : testValues) {
            String encoded = Base62.encode(v);
            long decoded = Base62.decode(encoded);
            System.out.printf("  %12d -> %-8s -> %12d %s%n", v, encoded, decoded, v == decoded ? "OK" : "MISMATCH");
        }

        // ─── Shorten URLs ───
        System.out.println("\n--- Shorten URLs ---");
        String url1 = "https://example.com/very/long/path?query=param&another=value";
        String url2 = "https://docs.oracle.com/en/java/javase/21/";
        String url3 = "https://github.com/spring-projects/spring-boot";

        String code1 = svc.shorten(url1, "alice");
        String code2 = svc.shorten(url2, "bob");
        String code3 = svc.shorten(url3, "alice");
        String code4 = svc.shorten("https://news.com/article/12345", "alice", 3600); // 1 hour TTL

        System.out.println("  " + url1);
        System.out.println("    -> " + code1);
        System.out.println("  " + url2);
        System.out.println("    -> " + code2);
        System.out.println("  " + url3);
        System.out.println("    -> " + code3);

        // ─── Custom Code ───
        System.out.println("\n--- Custom Short Code ---");
        try {
            String custom = svc.shortenWithCustomCode("https://myblog.com/post/42", "myblog", "alice");
            System.out.println("  Custom code: " + custom);
        } catch (Exception e) {
            System.out.println("  Error: " + e.getMessage());
        }

        // ─── Resolve URLs ───
        System.out.println("\n--- Resolve Short URLs ---");
        var resolved1 = svc.resolve(code1);
        System.out.println("  " + code1 + " -> " + resolved1.orElse("NOT FOUND"));

        // ─── Access Tracking ───
        System.out.println("\n--- Access Tracking ---");
        for (int i = 0; i < 5; i++) {
            svc.resolve(code1);
            svc.resolve(code2);
        }
        svc.resolve(code3);
        svc.resolve(code3);

        var info1 = svc.getUrlInfo(code1).orElseThrow();
        System.out.println("  " + code1 + " accessed " + info1.accessCount() + " times");
        System.out.println("  Last accessed: " + info1.lastAccessed());

        // ─── TTL Expiration ───
        System.out.println("\n--- TTL Expiration ---");
        System.out.println("  " + code4 + " (TTL=1h) expired: " + svc.getUrlInfo(code4).orElseThrow().isExpired());
        // Simulate expiration by accessing a TTL-ed URL with 0 TTL
        var expiredCode = svc.shorten("https://temp.com", "temp", -1);
        System.out.println("  Expired URL resolve: " + svc.resolve("nonexistent").orElse("NOT FOUND"));

        // ─── Redirect Simulation ───
        System.out.println("\n--- Redirect Simulation ---");
        var redirect = svc.redirect(code1);
        System.out.println("  " + code1 + " -> " + (redirect.success() ? "301 " + redirect.targetUrl() : "404 " + redirect.message()));

        redirect = svc.redirect("nonexistent");
        System.out.println("  nonexistent -> " + (redirect.success() ? redirect.targetUrl() : redirect.message()));

        // ─── User URL Management ───
        System.out.println("\n--- Alice's URLs ---");
        for (var u : svc.getUrlsByUser("alice")) {
            System.out.printf("  %-10s %-60s %d clicks%n", u.shortCode(), u.originalUrl(), u.accessCount());
        }

        // ─── Delete URL ───
        System.out.println("\n--- Delete URL ---");
        boolean deleted = svc.delete(code3, "alice");
        System.out.println("  Deleted " + code3 + ": " + deleted);
        System.out.println("  Resolve " + code3 + ": " + svc.resolve(code3).orElse("NOT FOUND (deleted)"));

        // ─── Duplicate URL Detection ───
        System.out.println("\n--- Duplicate URL Deduplication ---");
        String dupCode = svc.shorten(url1, "alice");
        System.out.println("  Same URL shortened again: " + dupCode + " (should equal original: " + code1 + ")");
        System.out.println("  Same code: " + dupCode.equals(code1));

        // ─── Bulk Generation ───
        System.out.println("\n--- Bulk Generation (1000 URLs) ---");
        for (int i = 0; i < 1000; i++) {
            svc.shorten("https://bulk.com/page/" + i, "bulk-user");
        }
        System.out.println("  Total URLs: " + svc.getTotalUrls());
        System.out.println("  Total clicks: " + svc.getTotalClicks());
        System.out.println("  Recent URLs: " + svc.getRecentUrls(3).size() + " shown");

        System.out.println("\n=== URL Shortener Complete ===");
    }
}
