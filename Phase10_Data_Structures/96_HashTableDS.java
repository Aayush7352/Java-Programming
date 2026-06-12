package phase10.datastructures;

import java.util.LinkedList;

class HashTableDS<K, V> {

    private static final int INITIAL_CAPACITY = 8;
    private static final double LOAD_FACTOR_THRESHOLD = 0.75;

    private LinkedList<Entry<K, V>>[] buckets;
    private int size;

    record Entry<K, V>(K key, V value) {}

    @SuppressWarnings("unchecked")
    public HashTableDS() {
        buckets = (LinkedList<Entry<K, V>>[]) new LinkedList[INITIAL_CAPACITY];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new LinkedList<>();
        }
    }

    private int hash(K key) {
        return Math.abs(key.hashCode() % buckets.length);
    }

    public void put(K key, V value) {
        var idx = hash(key);
        var bucket = buckets[idx];
        for (var entry : bucket) {
            if (entry.key().equals(key)) {
                return;
            }
        }
        bucket.add(new Entry<>(key, value));
        size++;
        if ((double) size / buckets.length > LOAD_FACTOR_THRESHOLD) {
            rehash();
        }
    }

    public V get(K key) {
        var idx = hash(key);
        for (var entry : buckets[idx]) {
            if (entry.key().equals(key)) {
                return entry.value();
            }
        }
        return null;
    }

    public V remove(K key) {
        var idx = hash(key);
        var bucket = buckets[idx];
        var it = bucket.listIterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.key().equals(key)) {
                it.remove();
                size--;
                return entry.value();
            }
        }
        return null;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @SuppressWarnings("unchecked")
    private void rehash() {
        var oldBuckets = buckets;
        buckets = (LinkedList<Entry<K, V>>[]) new LinkedList[oldBuckets.length * 2];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new LinkedList<>();
        }
        size = 0;
        for (var bucket : oldBuckets) {
            for (var entry : bucket) {
                put(entry.key(), entry.value());
            }
        }
    }

    public void printAll() {
        System.out.println("HashTable (size=" + size + ", capacity=" + buckets.length + "):");
        for (int i = 0; i < buckets.length; i++) {
            if (!buckets[i].isEmpty()) {
                System.out.println("  bucket[" + i + "]: " + buckets[i]);
            }
        }
    }

    public static void main(String[] args) {
        var map = new HashTableDS<String, Integer>();

        map.put("Alice", 25);
        map.put("Bob", 30);
        map.put("Charlie", 35);
        map.put("Diana", 28);
        map.put("Eve", 32);
        map.put("Frank", 27);
        map.put("Grace", 22);
        map.put("Hank", 40);

        System.out.println("Alice: " + map.get("Alice"));
        System.out.println("Unknown: " + map.get("Unknown"));

        map.printAll();

        System.out.println("\nRemove Bob: " + map.remove("Bob"));
        System.out.println("Contains Bob? " + map.containsKey("Bob"));
        System.out.println("Contains Charlie? " + map.containsKey("Charlie"));

        map.put("Eve", 33);
        System.out.println("Eve updated: " + map.get("Eve"));
    }
}
