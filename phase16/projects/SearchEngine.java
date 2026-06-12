package phase16.projects;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.io.*;
import java.nio.file.*;

/**
 * SearchEngine.java
 *
 * Search engine: Document indexing, inverted index, TF-IDF scoring,
 * query processing, ranked results, boolean search.
 */
public class SearchEngine {

    // ═══════════════════════════════════════════════
    // Records
    // ═══════════════════════════════════════════════

    record Document(String docId, String title, String content) {
        public List<String> tokens() {
            return tokenize(content);
        }

        public List<String> titleTokens() {
            return tokenize(title);
        }
    }

    record IndexEntry(String docId, int termFrequency, List<Integer> positions) {}

    record SearchResult(String docId, String title, double score, String snippet) implements Comparable<SearchResult> {
        @Override
        public int compareTo(SearchResult o) {
            return Double.compare(o.score, this.score); // descending
        }
    }

    record TermStats(int documentFrequency, int totalFrequency) {}

    // ═══════════════════════════════════════════════
    // Tokenizer & Utilities
    // ═══════════════════════════════════════════════

    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "by", "with", "from", "is", "are", "was", "were", "be", "been",
        "being", "have", "has", "had", "do", "does", "did", "will", "would",
        "could", "should", "may", "might", "shall", "can", "need", "dare",
        "it", "its", "this", "that", "these", "those", "i", "you", "he",
        "she", "we", "they", "me", "him", "her", "us", "them", "my", "your",
        "his", "its", "our", "their", "not", "no", "nor", "so", "as",
        "if", "then", "else", "when", "than", "too", "very", "just"
    );

    static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.toLowerCase().split("[^a-zA-Z0-9]+"))
            .filter(t -> t.length() >= 2)
            .filter(t -> !STOP_WORDS.contains(t))
            .collect(Collectors.toList());
    }

    static String stem(String word) {
        // Simple Porter-style stemmer (very basic)
        if (word.endsWith("ing")) return word.substring(0, word.length() - 3);
        if (word.endsWith("ed")) return word.substring(0, word.length() - 2);
        if (word.endsWith("ly")) return word.substring(0, word.length() - 2);
        if (word.endsWith("es")) return word.substring(0, word.length() - 2);
        if (word.endsWith("s") && !word.endsWith("ss")) return word.substring(0, word.length() - 1);
        return word;
    }

    static String generateSnippet(String content, List<String> queryTerms, int maxLength) {
        String lower = content.toLowerCase();
        int bestPos = -1;
        for (var term : queryTerms) {
            int pos = lower.indexOf(term);
            if (pos >= 0 && (bestPos < 0 || pos < bestPos)) {
                bestPos = pos;
            }
        }
        if (bestPos < 0) {
            return content.substring(0, Math.min(maxLength, content.length())) + "...";
        }
        int start = Math.max(0, bestPos - 50);
        int end = Math.min(content.length(), bestPos + maxLength - 50);
        String snippet = (start > 0 ? "..." : "") +
            content.substring(start, end) +
            (end < content.length() ? "..." : "");
        return snippet;
    }

    // ═══════════════════════════════════════════════
    // Inverted Index
    // ═══════════════════════════════════════════════

    static final class InvertedIndex {
        private final ConcurrentHashMap<String, List<IndexEntry>> index = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Document> documents = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, TermStats> termStats = new ConcurrentHashMap<>();
        private int totalDocuments = 0;

        public void addDocument(Document doc) {
            documents.put(doc.docId(), doc);
            totalDocuments++;

            var tokens = doc.tokens();
            var titleTokens = doc.titleTokens();

            // Build term frequency map
            var tfMap = new LinkedHashMap<String, MutableInt>();
            var posMap = new LinkedHashMap<String, List<Integer>>();

            int pos = 0;
            // Title tokens get weighted double
            for (var token : titleTokens) {
                String stemmed = stem(token);
                tfMap.computeIfAbsent(stemmed, k -> new MutableInt()).increment();
                posMap.computeIfAbsent(stemmed, k -> new ArrayList<>()).add(pos);
                pos++;
            }
            for (var token : tokens) {
                String stemmed = stem(token);
                // Boost title occurrences
                int weight = titleTokens.contains(token) ? 2 : 1;
                tfMap.computeIfAbsent(stemmed, k -> new MutableInt()).add(weight);
                for (int w = 0; w < weight; w++) {
                    posMap.computeIfAbsent(stemmed, k -> new ArrayList<>()).add(pos++);
                }
            }

            // Update index
            for (var entry : tfMap.entrySet()) {
                String term = entry.getKey();
                int tf = entry.getValue().value();
                var positions = posMap.getOrDefault(term, List.of());

                var idxEntry = new IndexEntry(doc.docId(), tf, positions);
                index.computeIfAbsent(term, k -> new CopyOnWriteArrayList<>()).add(idxEntry);

                termStats.merge(term, new TermStats(1, tf), (a, b) ->
                    new TermStats(a.documentFrequency() + 1, a.totalFrequency() + tf));
            }
        }

        public Optional<Document> getDocument(String docId) {
            return Optional.ofNullable(documents.get(docId));
        }

        public List<IndexEntry> getPostings(String term) {
            return index.getOrDefault(stem(term), List.of());
        }

        public TermStats getTermStats(String term) {
            return termStats.getOrDefault(stem(term), new TermStats(0, 0));
        }

        public int getTotalDocuments() { return totalDocuments; }
        public int getVocabularySize() { return index.size(); }

        public boolean containsTerm(String term) {
            return index.containsKey(stem(term));
        }
    }

    static class MutableInt {
        private int value = 0;
        public void increment() { value++; }
        public void add(int n) { value += n; }
        public int value() { return value; }
    }

    // ═══════════════════════════════════════════════
    // TF-IDF Scoring
    // ═══════════════════════════════════════════════

    static final class TfIdfScorer {
        private final InvertedIndex index;

        TfIdfScorer(InvertedIndex index) { this.index = index; }

        public double tfIdf(String term, String docId) {
            var postings = index.getPostings(term);
            int tf = postings.stream()
                .filter(e -> e.docId().equals(docId))
                .mapToInt(IndexEntry::termFrequency)
                .findFirst().orElse(0);

            if (tf == 0) return 0;

            int df = index.getTermStats(term).documentFrequency();
            int N = index.getTotalDocuments();
            double idf = Math.log(1 + (double) N / (1 + df));

            return (1 + Math.log(tf)) * idf;
        }

        public double score(String docId, List<String> queryTerms) {
            double total = 0;
            for (var term : queryTerms) {
                total += tfIdf(term, docId);
            }
            return total;
        }
    }

    // ═══════════════════════════════════════════════
    // Search Engine
    // ═══════════════════════════════════════════════

    static final class SearchEngineService {
        private final InvertedIndex index;
        private final TfIdfScorer scorer;

        SearchEngineService() {
            this.index = new InvertedIndex();
            this.scorer = new TfIdfScorer(index);
        }

        public void indexDocument(Document doc) {
            index.addDocument(doc);
        }

        public void indexDocuments(List<Document> docs) {
            for (var doc : docs) {
                index.addDocument(doc);
            }
        }

        // ─── Ranked Search ───

        public List<SearchResult> search(String query, int maxResults) {
            var queryTerms = tokenize(query).stream()
                .map(SearchEngine::stem)
                .distinct()
                .collect(Collectors.toList());

            if (queryTerms.isEmpty()) return List.of();

            // Find candidate documents
            var candidates = new ConcurrentHashMap<String, Double>();
            for (var term : queryTerms) {
                for (var posting : index.getPostings(term)) {
                    candidates.merge(posting.docId(), scorer.tfIdf(term, posting.docId()), Double::sum);
                }
            }

            // Build results
            return candidates.entrySet().stream()
                .map(e -> {
                    var doc = index.getDocument(e.getKey()).orElse(null);
                    if (doc == null) return null;
                    String snippet = generateSnippet(doc.content(), queryTerms, 150);
                    return new SearchResult(doc.docId(), doc.title(), e.getValue(), snippet);
                })
                .filter(Objects::nonNull)
                .sorted()
                .limit(maxResults)
                .collect(Collectors.toList());
        }

        // ─── Boolean Search (AND/OR/NOT) ───

        public List<SearchResult> booleanSearch(String query) {
            // Parse simple boolean: term1 AND term2, term1 OR term2, term1 NOT term2
            String upper = query.toUpperCase();
            boolean isAnd = upper.contains(" AND ");
            boolean isOr = upper.contains(" OR ");
            boolean isNot = upper.contains(" NOT ");

            if (isAnd) {
                var parts = query.split("(?i)\\s+AND\\s+");
                var terms = Arrays.stream(parts).map(String::trim).collect(Collectors.toList());
                if (terms.size() < 2) return List.of();

                var firstResults = getDocIdsForTerm(terms.get(0));
                for (int i = 1; i < terms.size(); i++) {
                    var nextResults = getDocIdsForTerm(terms.get(i));
                    firstResults.retainAll(nextResults);
                }
                return buildBooleanResults(firstResults);
            } else if (isOr) {
                var parts = query.split("(?i)\\s+OR\\s+");
                var results = new HashSet<String>();
                for (var part : parts) {
                    results.addAll(getDocIdsForTerm(part.trim()));
                }
                return buildBooleanResults(results);
            } else if (isNot) {
                var parts = query.split("(?i)\\s+NOT\\s+");
                if (parts.length < 2) return List.of();
                var include = getDocIdsForTerm(parts[0].trim());
                var exclude = getDocIdsForTerm(parts[1].trim());
                include.removeAll(exclude);
                return buildBooleanResults(include);
            }

            // Default to ranked search
            return search(query, 10);
        }

        private Set<String> getDocIdsForTerm(String term) {
            return index.getPostings(term).stream()
                .map(IndexEntry::docId)
                .collect(Collectors.toSet());
        }

        private List<SearchResult> buildBooleanResults(Set<String> docIds) {
            return docIds.stream()
                .map(id -> index.getDocument(id).orElse(null))
                .filter(Objects::nonNull)
                .map(doc -> new SearchResult(doc.docId(), doc.title(), 1.0,
                    doc.content().substring(0, Math.min(100, doc.content().length()))))
                .collect(Collectors.toList());
        }

        public InvertedIndex getIndex() { return index; }
    }

    // ═══════════════════════════════════════════════
    // Demo
    // ═══════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("=== Search Engine ===\n");

        SearchEngineService engine = new SearchEngineService();

        // ─── Index Documents ───
        System.out.println("--- Indexing Documents ---");
        var docs = List.of(
            new Document("DOC-1", "Java Programming Guide",
                "Java is a high-level programming language developed by Sun Microsystems. " +
                "It is widely used for building enterprise applications, mobile apps (Android), " +
                "and web services. Java runs on the Java Virtual Machine (JVM). " +
                "Java 21 introduced virtual threads, pattern matching, and record patterns."),
            new Document("DOC-2", "Python for Data Science",
                "Python is a versatile programming language popular in data science, " +
                "machine learning, and artificial intelligence. It has libraries like " +
                "NumPy, Pandas, and TensorFlow that make data analysis powerful and accessible."),
            new Document("DOC-3", "Web Development with JavaScript",
                "JavaScript is the language of the web. It runs in browsers and on servers " +
                "via Node.js. Modern JavaScript frameworks include React, Angular, and Vue.js. " +
                "JavaScript is essential for front-end and full-stack development."),
            new Document("DOC-4", "Introduction to Machine Learning",
                "Machine learning is a subset of artificial intelligence that enables systems " +
                "to learn from data. Python is the most common language for machine learning, " +
                "with libraries like scikit-learn, TensorFlow, and PyTorch."),
            new Document("DOC-5", "Advanced Java Concepts",
                "Advanced Java covers topics like concurrency, streams, lambda expressions, " +
                "the memory model, garbage collection, and performance tuning. " +
                "Virtual threads in Java 21 revolutionize concurrent programming."),
            new Document("DOC-6", "Database Systems",
                "Database systems store and manage data. SQL databases like PostgreSQL and " +
                "MySQL use structured queries. NoSQL databases like MongoDB offer flexible " +
                "schemas for modern applications."),
            new Document("DOC-7", "Cloud Computing Basics",
                "Cloud computing delivers computing services over the internet. AWS, Azure, " +
                "and Google Cloud provide virtual machines, storage, databases, and AI services. " +
                "Microservices architecture is commonly deployed on cloud platforms."),
            new Document("DOC-8", "Data Structures and Algorithms",
                "Data structures organize data for efficient access. Common structures include " +
                "arrays, linked lists, trees, graphs, hash tables, and heaps. Algorithms " +
                "manipulate these structures to solve computational problems."),
            new Document("DOC-9", "The Art of Software Engineering",
                "Software engineering applies engineering principles to software development. " +
                "It includes requirements analysis, design, implementation, testing, and " +
                "maintenance. Agile methodologies like Scrum are widely adopted."),
            new Document("DOC-10", "Network Programming in Java",
                "Java provides extensive APIs for network programming including sockets, " +
                "URL connections, and NIO channels. Java's concurrency utilities make " +
                "it ideal for building scalable network services and servers.")
        );

        engine.indexDocuments(docs);
        System.out.println("  Indexed " + docs.size() + " documents");
        System.out.println("  Vocabulary size: " + engine.getIndex().getVocabularySize() + " unique terms");

        // ─── Ranked Search ───
        System.out.println("\n--- Ranked Search: 'Java programming virtual threads' ---");
        var results = engine.search("Java programming virtual threads", 5);
        for (var r : results) {
            System.out.printf("  %.4f %s%n", r.score(), r.title());
            System.out.println("    " + r.snippet());
        }

        // ─── Ranked Search: Python/ML ───
        System.out.println("\n--- Ranked Search: 'machine learning Python' ---");
        results = engine.search("machine learning Python", 5);
        for (var r : results) {
            System.out.printf("  %.4f %s%n", r.score(), r.title());
            System.out.println("    " + r.snippet());
        }

        // ─── Boolean AND Search ───
        System.out.println("\n--- Boolean AND: 'Java AND virtual' ---");
        var boolResults = engine.booleanSearch("Java AND virtual");
        for (var r : boolResults) {
            System.out.println("  " + r.title());
        }

        // ─── Boolean OR Search ───
        System.out.println("\n--- Boolean OR: 'JavaScript OR Python' ---");
        boolResults = engine.booleanSearch("JavaScript OR Python");
        for (var r : boolResults) {
            System.out.println("  " + r.title());
        }

        // ─── Boolean NOT Search ───
        System.out.println("\n--- Boolean NOT: 'Java NOT virtual' ---");
        boolResults = engine.booleanSearch("Java NOT virtual");
        for (var r : boolResults) {
            System.out.println("  " + r.title());
        }

        // ─── Single Term Search ───
        System.out.println("\n--- Search: 'cloud' ---");
        results = engine.search("cloud", 5);
        for (var r : results) {
            System.out.printf("  %.4f %s%n", r.score(), r.title());
        }

        // ─── Term Statistics ───
        System.out.println("\n--- Term Stats ---");
        for (var term : List.of("java", "python", "machine", "data", "learning")) {
            var stats = engine.getIndex().getTermStats(term);
            System.out.printf("  %-12s DF=%-2d TF=%-3d%n", term, stats.documentFrequency(), stats.totalFrequency());
        }

        System.out.println("\n=== Search Engine Complete ===");
    }
}
