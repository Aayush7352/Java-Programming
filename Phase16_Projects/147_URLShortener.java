package phase16.projects;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

final class URLShortener {

    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;
    private static final int DEFAULT_SHORT_LENGTH = 7;
    private static final long MAX_BASE62_7 = 62L * 62 * 62 * 62 * 62 * 62 * 62;

    public static record ShortURL(String shortCode, String originalUrl, LocalDateTime createdAt,
                                   LocalDateTime expiresAt, long accessCount, String customAlias) {
        public ShortURL {
            Objects.requireNonNull(shortCode);
            Objects.requireNonNull(originalUrl);
            Objects.requireNonNull(createdAt);
        }

        public ShortURL withAccessCount(long count) {
            return new ShortURL(shortCode, originalUrl, createdAt, expiresAt, count, customAlias);
        }

        public boolean isExpired() {
            return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
        }

        public String getShortUrl(String baseDomain) {
            return "%s/%s".formatted(baseDomain.endsWith("/") ? baseDomain.substring(0, baseDomain.length() - 1) : baseDomain, shortCode);
        }
    }

    public static record ClickEvent(String shortCode, String referer, String userAgent,
                                     String ipAddress, LocalDateTime timestamp) {
        public ClickEvent {
            Objects.requireNonNull(shortCode);
            Objects.requireNonNull(timestamp);
        }
    }

    public static final class ShortenerEngine {
        private final Map<String, ShortURL> urlMap = new ConcurrentHashMap<>();
        private final Map<String, String> reverseMap = new ConcurrentHashMap<>();
        private final List<ClickEvent> clickEvents = Collections.synchronizedList(new ArrayList<>());
        private final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis());
        private final int shortLength;
        private final Set<String> reservedCodes = ConcurrentHashMap.newKeySet();
        private static final int MAX_COLLISION_RETRIES = 5;

        public ShortenerEngine() {
            this(DEFAULT_SHORT_LENGTH);
        }

        public ShortenerEngine(int shortLength) {
            this.shortLength = shortLength;
            reservedCodes.addAll(Set.of("api", "admin", "login", "signup", "stats", "health", "metrics"));
        }

        public ShortURL shorten(String originalUrl) {
            return shorten(originalUrl, null, null);
        }

        public ShortURL shorten(String originalUrl, String customAlias, LocalDateTime expiresAt) {
            Objects.requireNonNull(originalUrl);

            if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
                originalUrl = "https://" + originalUrl;
            }

            if (customAlias != null && !customAlias.isBlank()) {
                if (reservedCodes.contains(customAlias.toLowerCase())) {
                    throw new IllegalArgumentException("Alias '%s' is reserved".formatted(customAlias));
                }
                if (urlMap.containsKey(customAlias)) {
                    throw new IllegalArgumentException("Alias '%s' already in use".formatted(customAlias));
                }
                var shortUrl = new ShortURL(customAlias, originalUrl, LocalDateTime.now(), expiresAt, 0, customAlias);
                urlMap.put(customAlias, shortUrl);
                reverseMap.put(originalUrl, customAlias);
                return shortUrl;
            }

            var existing = reverseMap.get(originalUrl);
            if (existing != null && !urlMap.get(existing).isExpired()) {
                return urlMap.get(existing);
            }

            for (int attempt = 0; attempt < MAX_COLLISION_RETRIES; attempt++) {
                var code = generateShortCode();
                if (!urlMap.containsKey(code) && !reservedCodes.contains(code)) {
                    var shortUrl = new ShortURL(code, originalUrl, LocalDateTime.now(), expiresAt, 0, null);
                    urlMap.put(code, shortUrl);
                    reverseMap.put(originalUrl, code);
                    return shortUrl;
                }
            }

            var hash = sha256Hash(originalUrl + System.nanoTime());
            var code = hash.substring(0, shortLength + 4);
            if (urlMap.containsKey(code)) {
                code = hash.substring(0, shortLength + 6);
            }
            var shortUrl = new ShortURL(code, originalUrl, LocalDateTime.now(), expiresAt, 0, null);
            urlMap.put(code, shortUrl);
            reverseMap.put(originalUrl, code);
            return shortUrl;
        }

        public Optional<ShortURL> resolve(String shortCode) {
            var entry = urlMap.get(shortCode);
            if (entry == null) return Optional.empty();
            if (entry.isExpired()) {
                urlMap.remove(shortCode);
                reverseMap.remove(entry.originalUrl());
                return Optional.empty();
            }
            var updated = entry.withAccessCount(entry.accessCount() + 1);
            urlMap.put(shortCode, updated);
            return Optional.of(updated);
        }

        public String resolveAndRedirect(String shortCode) {
            return resolve(shortCode)
                    .map(ShortURL::originalUrl)
                    .orElseThrow(() -> new NoSuchElementException("Short code not found: " + shortCode));
        }

        public boolean delete(String shortCode) {
            var entry = urlMap.remove(shortCode);
            if (entry != null) {
                reverseMap.remove(entry.originalUrl());
                return true;
            }
            return false;
        }

        public void recordClick(String shortCode, String referer, String userAgent, String ip) {
            clickEvents.add(new ClickEvent(shortCode,
                    referer != null ? referer : "direct",
                    userAgent != null ? userAgent : "unknown",
                    ip != null ? ip : "0.0.0.0",
                    LocalDateTime.now()));
        }

        public List<ClickEvent> getClickEvents(String shortCode) {
            return clickEvents.stream()
                    .filter(e -> e.shortCode().equals(shortCode))
                    .collect(Collectors.toUnmodifiableList());
        }

        public Map<String, Long> getClickStats() {
            return clickEvents.stream()
                    .collect(Collectors.groupingBy(ClickEvent::shortCode, Collectors.counting()));
        }

        public List<ShortURL> getAllUrls() {
            return List.copyOf(urlMap.values());
        }

        public int totalUrls() { return urlMap.size(); }
        public long totalClicks() { return clickEvents.size(); }

        private String generateShortCode() {
            var num = idCounter.incrementAndGet() % MAX_BASE62_7;
            return base62Encode(num);
        }

        public static String base62Encode(long value) {
            if (value == 0) return String.valueOf(BASE62_ALPHABET.charAt(0));
            var sb = new StringBuilder();
            var v = value;
            while (v > 0) {
                sb.append(BASE62_ALPHABET.charAt((int) (v % BASE)));
                v /= BASE;
            }
            return sb.reverse().toString();
        }

        public static long base62Decode(String value) {
            long result = 0;
            for (int i = 0; i < value.length(); i++) {
                result = result * BASE + BASE62_ALPHABET.indexOf(value.charAt(i));
            }
            return result;
        }

        private static String sha256Hash(String input) {
            try {
                var digest = MessageDigest.getInstance("SHA-256");
                var hashBytes = digest.digest(input.getBytes());
                var sb = new StringBuilder();
                for (var b : hashBytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 not available", e);
            }
        }
    }

    public static final class AnalyticsService {
        private final ShortenerEngine engine;

        public AnalyticsService(ShortenerEngine engine) {
            this.engine = engine;
        }

        public String generateReport() {
            var sb = new StringBuilder();
            sb.append("=== URL Shortener Analytics ===\n");
            sb.append("Total URLs: ").append(engine.totalUrls()).append("\n");
            sb.append("Total Clicks: ").append(engine.totalClicks()).append("\n");

            var topUrls = engine.getAllUrls().stream()
                    .sorted((a, b) -> Long.compare(b.accessCount(), a.accessCount()))
                    .limit(5)
                    .toList();

            sb.append("\nTop 5 URLs:\n");
            for (var url : topUrls) {
                sb.append("  [").append(url.shortCode()).append("] ")
                        .append(url.accessCount()).append(" clicks -> ")
                        .append(url.originalUrl()).append("\n");
            }

            var clickStats = engine.getClickStats();
            sb.append("\nClicks per short code:\n");
            clickStats.forEach((code, count) ->
                sb.append("  ").append(code).append(": ").append(count).append(" clicks\n"));

            return sb.toString();
        }
    }

    public static void main(String[] args) {
        System.out.println("=== URL Shortener ===%n".formatted());

        var engine = new ShortenerEngine();
        var analytics = new AnalyticsService(engine);

        System.out.println("--- Base62 Tests ---");
        var encoded = ShortenerEngine.base62Encode(12345);
        var decoded = ShortenerEngine.base62Decode(encoded);
        System.out.println("  Base62(12345) = " + encoded);
        System.out.println("  Decode('%s') = %d".formatted(encoded, decoded));
        System.out.println("  Match: " + (decoded == 12345));

        System.out.println("%n--- Shorten URLs ---%n".formatted());
        var url1 = engine.shorten("https://www.example.com/very/long/path?query=parameter&foo=bar");
        System.out.println("  Short: " + url1.shortCode() + " -> " + url1.originalUrl());

        var url2 = engine.shorten("https://github.com/opencode-ai/opencode");
        System.out.println("  Short: " + url2.shortCode() + " -> " + url2.originalUrl());

        System.out.println("%n--- Custom Alias ---%n".formatted());
        var url3 = engine.shorten("https://docs.oracle.com/en/java/javase/21/", "javadocs",
                LocalDateTime.now().plusDays(365));
        System.out.println("  Custom: " + url3.shortCode() + " -> " + url3.originalUrl());
        System.out.println("  Expires: " + url3.expiresAt());
        System.out.println("  Full short URL: " + url3.getShortUrl("https://short.link"));

        System.out.println("%n--- Resolve & Redirect Simulation ---%n".formatted());
        var resolved1 = engine.resolveAndRedirect(url1.shortCode());
        System.out.println("  Resolve '%s': %s".formatted(url1.shortCode(), resolved1));

        var resolved2 = engine.resolve(url2.shortCode());
        resolved2.ifPresentOrElse(
            u -> System.out.println("  Resolve '%s': %s (clicks: %d)".formatted(u.shortCode(), u.originalUrl(), u.accessCount())),
            () -> System.out.println("  Not found")
        );

        System.out.println("%n--- Record Clicks ---%n".formatted());
        engine.recordClick(url1.shortCode(), "https://twitter.com", "Mozilla/5.0", "192.168.1.1");
        engine.recordClick(url1.shortCode(), "https://reddit.com", "Chrome/120", "10.0.0.1");
        engine.recordClick(url2.shortCode(), "direct", "curl/8.0", "172.16.0.1");

        System.out.println("%n--- Delete a URL ---%n".formatted());
        var url4 = engine.shorten("https://temp.com/deleteme");
        System.out.println("  Created: " + url4.shortCode());
        var deleted = engine.delete(url4.shortCode());
        System.out.println("  Deleted: " + deleted);
        try {
            engine.resolveAndRedirect(url4.shortCode());
        } catch (NoSuchElementException e) {
            System.out.println("  Resolve after delete: " + e.getMessage());
        }

        System.out.println("%n--- Analytics Report ---%n".formatted());
        System.out.println(analytics.generateReport());

        System.out.println("%n--- Virtual Threads: Concurrent URL Creation ---%n".formatted());
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10; i++) {
                final int idx = i;
                executor.submit(() -> {
                    var u = engine.shorten("https://example.com/page?id=" + idx + "&t=" + System.nanoTime());
                    System.out.println("  [VT-%d] Created: %s".formatted(idx, u.shortCode()));
                });
            }
        }

        System.out.println("%n--- Pattern Matching on ShortURLs ---%n".formatted());
        for (var su : engine.getAllUrls().stream().limit(5).toList()) {
            switch (su) {
                case ShortURL s when s.customAlias() != null ->
                    System.out.println("  Custom alias '%s' -> %s (%,d clicks)".formatted(s.shortCode(), s.originalUrl(), s.accessCount()));
                case ShortURL s when s.accessCount() > 1 ->
                    System.out.println("  Popular '%s' -> %s (%,d clicks)".formatted(s.shortCode(), s.originalUrl(), s.accessCount()));
                case ShortURL s ->
                    System.out.println("  Normal '%s' -> %s".formatted(s.shortCode(), s.originalUrl()));
            }
        }

        System.out.println("%nFinal stats: %d URLs, %d clicks".formatted(engine.totalUrls(), engine.totalClicks()));
        System.out.println("=== Done ===");
    }
}
