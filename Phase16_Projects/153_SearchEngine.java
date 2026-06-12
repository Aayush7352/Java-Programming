package phase16.projects;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class SearchEngine {

    public static sealed interface QueryNode permits TermQuery, AndQuery, OrQuery, NotQuery {
        Set<String> evaluate(Map<String, Map<String, Double>> index);
    }

    public static record TermQuery(String term) implements QueryNode {
        @Override
        public Set<String> evaluate(Map<String, Map<String, Double>> index) {
            var invertedEntry = index.get(term.toLowerCase());
            if (invertedEntry == null) return Set.of();
            return invertedEntry.keySet();
        }
    }

    public static record AndQuery(QueryNode left, QueryNode right) implements QueryNode {
        @Override
        public Set<String> evaluate(Map<String, Map<String, Double>> index) {
            var leftDocs = left.evaluate(index);
            var rightDocs = right.evaluate(index);
            var result = new HashSet<>(leftDocs);
            result.retainAll(rightDocs);
            return result;
        }
    }

    public static record OrQuery(QueryNode left, QueryNode right) implements QueryNode {
        @Override
        public Set<String> evaluate(Map<String, Map<String, Double>> index) {
            var leftDocs = left.evaluate(index);
            var rightDocs = right.evaluate(index);
            var result = new HashSet<>(leftDocs);
            result.addAll(rightDocs);
            return result;
        }
    }

    public static record NotQuery(QueryNode node) implements QueryNode {
        @Override
        public Set<String> evaluate(Map<String, Map<String, Double>> index) {
            var allDocs = index.values().stream()
                    .flatMap(m -> m.keySet().stream())
                    .collect(Collectors.toSet());
            var excludeDocs = node.evaluate(index);
            allDocs.removeAll(excludeDocs);
            return allDocs;
        }
    }

    public static record Document(String docId, String title, String content) {
        public Document {
            Objects.requireNonNull(docId);
            Objects.requireNonNull(title);
            Objects.requireNonNull(content);
        }

        public List<String> getTokens() {
            return InvertedIndex.tokenize(title + " " + content);
        }

        public Map<String, Integer> getTermFrequency() {
            var tf = new HashMap<String, Integer>();
            for (var token : getTokens()) {
                tf.merge(token, 1, Integer::sum);
            }
            return tf;
        }
    }

    public static record SearchResult(String docId, String title, double score,
                                       List<String> snippets) implements Comparable<SearchResult> {
        @Override
        public int compareTo(SearchResult other) {
            return Double.compare(other.score, this.score);
        }
    }

    public static final class QueryParser {
        private int pos;
        private List<String> tokens;

        public QueryNode parse(String query) {
            pos = 0;
            tokens = tokenizeQuery(query);
            return parseOr();
        }

        private QueryNode parseOr() {
            var left = parseAnd();
            while (pos < tokens.size() && tokens.get(pos).equalsIgnoreCase("OR")) {
                pos++;
                var right = parseAnd();
                left = new OrQuery(left, right);
            }
            return left;
        }

        private QueryNode parseAnd() {
            var left = parseNot();
            while (pos < tokens.size() && !tokens.get(pos).equalsIgnoreCase("OR")) {
                if (tokens.get(pos).equalsIgnoreCase("AND")) {
                    pos++;
                }
                if (pos < tokens.size() && tokens.get(pos).equals(")")) break;
                var right = parseNot();
                left = new AndQuery(left, right);
            }
            return left;
        }

        private QueryNode parseNot() {
            if (pos < tokens.size() && tokens.get(pos).equalsIgnoreCase("NOT")) {
                pos++;
                return new NotQuery(parsePrimary());
            }
            return parsePrimary();
        }

        private QueryNode parsePrimary() {
            if (pos >= tokens.size())
                throw new IllegalArgumentException("Unexpected end of query");

            var token = tokens.get(pos);
            if (token.equals("(")) {
                pos++;
                var node = parseOr();
                if (pos >= tokens.size() || !tokens.get(pos).equals(")"))
                    throw new IllegalArgumentException("Missing closing parenthesis");
                pos++;
                return node;
            }

            pos++;
            return new TermQuery(token);
        }

        private List<String> tokenizeQuery(String query) {
            var cleaned = query.replaceAll("([()])", " $1 ");
            var result = new ArrayList<String>();
            for (var part : cleaned.split("\\s+")) {
                var trimmed = part.trim().toLowerCase();
                if (!trimmed.isEmpty()) result.add(trimmed);
            }
            return result;
        }
    }

    public static final class InvertedIndex {
        private final Map<String, Map<String, Double>> index = new ConcurrentHashMap<>();
        private final Map<String, Document> documents = new ConcurrentHashMap<>();
        private final AtomicLong docCounter = new AtomicLong(0);
        private int totalDocuments = 0;

        private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z0-9]+");

        public static List<String> tokenize(String text) {
            var tokens = new ArrayList<String>();
            var matcher = WORD_PATTERN.matcher(text.toLowerCase());
            while (matcher.find()) {
                var token = matcher.group();
                if (token.length() >= 2) {
                    tokens.add(token);
                }
            }
            return tokens;
        }

        public Document addDocument(String title, String content) {
            var docId = "DOC-" + docCounter.incrementAndGet();
            var doc = new Document(docId, title, content);
            documents.put(docId, doc);
            indexDocument(doc);
            return doc;
        }

        public void indexDocument(Document doc) {
            var tf = doc.getTermFrequency();
            var maxFreq = tf.values().stream().mapToInt(Integer::intValue).max().orElse(1);

            for (var entry : tf.entrySet()) {
                var term = entry.getKey();
                var normalizedTf = (double) entry.getValue() / maxFreq;
                index.computeIfAbsent(term, k -> new ConcurrentHashMap<>())
                        .put(doc.docId(), normalizedTf);
            }
            totalDocuments = documents.size();
        }

        public Map<String, Double> computeTfIdf(String docId) {
            var doc = documents.get(docId);
            if (doc == null) return Map.of();

            var tf = doc.getTermFrequency();
            var maxFreq = tf.values().stream().mapToInt(Integer::intValue).max().orElse(1);
            var result = new HashMap<String, Double>();

            for (var entry : tf.entrySet()) {
                var term = entry.getKey();
                var tfValue = (double) entry.getValue() / maxFreq;
                var docFreq = index.getOrDefault(term, Map.of()).size();
                var idf = Math.log((double) (totalDocuments + 1) / (docFreq + 1)) + 1;
                result.put(term, tfValue * idf);
            }

            return result;
        }

        public List<SearchResult> search(String queryString) {
            return search(queryString, 10);
        }

        public List<SearchResult> search(String queryString, int maxResults) {
            var parser = new QueryParser();
            QueryNode queryNode;
            try {
                queryNode = parser.parse(queryString);
            } catch (Exception e) {
                return List.of();
            }

            var matchingDocs = queryNode.evaluate(index);

            var results = new ArrayList<SearchResult>();
            for (var docId : matchingDocs) {
                var doc = documents.get(docId);
                if (doc == null) continue;

                var score = scoreDocument(doc, queryString);
                var snippets = generateSnippets(doc, queryString, 2);
                results.add(new SearchResult(docId, doc.title(), score, snippets));
            }

            results.sort(null);
            return results.stream().limit(maxResults).collect(Collectors.toUnmodifiableList());
        }

        private double scoreDocument(Document doc, String query) {
            var queryTokens = tokenize(query);
            var tfidf = computeTfIdf(doc.docId());
            double score = 0;

            for (var qt : queryTokens) {
                var docTfidf = tfidf.getOrDefault(qt, 0.0);
                var docFreq = index.getOrDefault(qt, Map.of()).size();
                var idf = Math.log((double) (totalDocuments + 1) / (docFreq + 1)) + 1;
                score += docTfidf * idf;
            }

            return score;
        }

        private List<String> generateSnippets(Document doc, String query, int maxSnippets) {
            var queryTokens = tokenize(query);
            var content = doc.content().toLowerCase();
            var snippets = new ArrayList<String>();
            var maxLen = 100;

            for (var qt : queryTokens) {
                if (snippets.size() >= maxSnippets) break;
                var idx = content.indexOf(qt);
                if (idx >= 0) {
                    var start = Math.max(0, idx - 40);
                    var end = Math.min(content.length(), idx + qt.length() + 40);
                    var snippet = doc.content().substring(start, end);
                    if (start > 0) snippet = "..." + snippet;
                    if (end < doc.content().length()) snippet += "...";
                    if (!snippets.contains(snippet)) {
                        snippets.add(snippet);
                    }
                }
            }

            return snippets.isEmpty() ? List.of(doc.title()) : snippets;
        }

        public Document getDocument(String docId) { return documents.get(docId); }
        public int documentCount() { return documents.size(); }
        public int termCount() { return index.size(); }
        public Set<String> getAllTerms() { return index.keySet(); }

        public Map<String, Integer> getTermFrequencies() {
            return index.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().size()));
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Search Engine ===%n".formatted());

        var index = new InvertedIndex();

        var docs = List.of(
                "The quick brown fox jumps over the lazy dog near the riverbank",
                "Java 21 introduces virtual threads for scalable concurrent applications",
                "Virtual threads are lightweight threads that improve server throughput",
                "Pattern matching for switch simplifies conditional logic in Java",
                "Records provide transparent data carriers with less boilerplate code",
                "Sealed classes enable exhaustive pattern matching in domain models",
                "The fox is quick and brown, jumping over dogs in the forest",
                "Concurrent programming with virtual threads is efficient and simple",
                "Java records and sealed classes work together for robust domain modeling",
                "Server applications benefit from virtual threads and structured concurrency",
                "Spring Framework 6 supports Java 21 features like virtual threads",
                "The lazy dog sleeps while the quick brown fox jumps over the fence"
        );

        System.out.println("--- Indexing Documents ---");
        for (int i = 0; i < docs.size(); i++) {
            var doc = index.addDocument("Document " + (i + 1), docs.get(i));
            System.out.println("  Indexed: %s - \"%s\"".formatted(doc.docId(), doc.title()));
        }

        System.out.println("%n--- Index Stats ---%n".formatted());
        System.out.println("  Documents: " + index.documentCount());
        System.out.println("  Unique terms: " + index.termCount());
        System.out.println("  Top terms: " + index.getTermFrequencies().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(e -> "%s(%d)".formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(", ")));

        System.out.println("%n--- Search: \"virtual threads\" ---%n".formatted());
        var results1 = index.search("virtual threads");
        results1.forEach(r ->
            System.out.println("  [%.4f] %s".formatted(r.score(), r.title())));

        System.out.println("%n--- Search: \"fox AND dog\" ---%n".formatted());
        var results2 = index.search("fox AND dog");
        results2.forEach(r ->
            System.out.println("  [%.4f] %s".formatted(r.score(), r.title())));

        System.out.println("%n--- Search: \"Java OR fox\" ---%n".formatted());
        var results3 = index.search("Java OR fox");
        results3.forEach(r ->
            System.out.println("  [%.4f] %s".formatted(r.score(), r.title())));

        System.out.println("%n--- Search: \"virtual AND NOT lazy\" ---%n".formatted());
        var results4 = index.search("virtual AND NOT lazy");
        results4.forEach(r ->
            System.out.println("  [%.4f] %s - snippets: %s"
                    .formatted(r.score(), r.title(), String.join(" | ", r.snippets()))));

        System.out.println("%n--- Search: \"(Java OR Spring) AND threads\" ---%n".formatted());
        var results5 = index.search("(Java OR Spring) AND threads");
        results5.forEach(r ->
            System.out.println("  [%.4f] %s".formatted(r.score(), r.title())));

        System.out.println("%n--- TF-IDF for Document 1 ---%n".formatted());
        var tfidf = index.computeTfIdf("DOC-1");
        tfidf.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> System.out.println("  %s: %.4f".formatted(e.getKey(), e.getValue())));

        System.out.println("%n--- Pattern Matching on Query Nodes ---%n".formatted());
        var parser = new QueryParser();
        for (var q : List.of("virtual threads", "fox AND dog", "Java OR fox", "virtual AND NOT lazy")) {
            var node = parser.parse(q);
            var desc = switch (node) {
                case TermQuery t -> "Single term: " + t.term();
                case AndQuery a -> "Boolean AND of 2+ terms";
                case OrQuery o -> "Boolean OR of 2+ terms";
                case NotQuery n -> "Negation of term(s)";
            };
            System.out.println("  '%s' -> %s".formatted(q, desc));
        }

        System.out.println("%n--- Virtual Threads: Concurrent Searches ---%n".formatted());
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var queries = List.of("quick fox", "Java records", "concurrent", "spring server", "lazy dog");
            for (var q : queries) {
                executor.submit(() -> {
                    var res = index.search(q, 3);
                    System.out.println("  Search '%s': %d results".formatted(q, res.size()));
                });
            }
        }

        System.out.println("%nFinal Stats: %d documents, %d terms, %d searches performed"
                .formatted(index.documentCount(), index.termCount(), 5));
        System.out.println("=== Done ===");
    }
}
