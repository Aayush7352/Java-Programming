package phase05.collections;

import java.util.LinkedHashMap;
import java.util.Map;

public class LinkedHashMapExample {
    public static void main(String[] args) {
        // Insertion order
        LinkedHashMap<String, Integer> insertionOrder = new LinkedHashMap<>();
        insertionOrder.put("One", 1);
        insertionOrder.put("Two", 2);
        insertionOrder.put("Three", 3);
        insertionOrder.put("Four", 4);
        System.out.println("Insertion order: " + insertionOrder);

        // Access order
        LinkedHashMap<String, Integer> accessOrder = new LinkedHashMap<>(16, 0.75f, true);
        accessOrder.put("A", 1);
        accessOrder.put("B", 2);
        accessOrder.put("C", 3);
        accessOrder.put("D", 4);
        System.out.println("\nAccess order (initial): " + accessOrder);

        // Access some elements
        accessOrder.get("B");
        accessOrder.get("A");
        accessOrder.get("C");
        System.out.println("Access order (after getting B, A, C): " + accessOrder);

        // LRU Cache example
        System.out.println("\n--- LRU Cache (max 3 entries) ---");
        LRUCache<String, String> cache = new LRUCache<>(3);
        cache.put("user1", "Alice");
        cache.put("user2", "Bob");
        cache.put("user3", "Charlie");
        System.out.println("Cache: " + cache);

        // Access user1, making it most recently used
        cache.get("user1");
        System.out.println("After accessing user1: " + cache);

        // Add new entry - should evict least recently used (user2)
        cache.put("user4", "David");
        System.out.println("After adding user4 (evicts LRU): " + cache);
    }

    static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxEntries;

        LRUCache(int maxEntries) {
            super(maxEntries, 0.75f, true);
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxEntries;
        }
    }
}
