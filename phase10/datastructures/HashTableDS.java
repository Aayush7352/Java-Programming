package phase10.datastructures;

import java.util.LinkedList;

class Entry<K, V> {
    K key;
    V value;

    Entry(K key, V value) {
        this.key = key;
        this.value = value;
    }
}

public class HashTableDS<K, V> {
    private static final double LOAD_FACTOR = 0.75;
    private LinkedList<Entry<K, V>>[] buckets;
    private int size;

    @SuppressWarnings("unchecked")
    public HashTableDS(int capacity) {
        buckets = new LinkedList[capacity];
        for (int i = 0; i < capacity; i++) buckets[i] = new LinkedList<>();
        size = 0;
    }

    private int hash(K key) {
        return Math.abs(key.hashCode()) % buckets.length;
    }

    public void put(K key, V value) {
        int idx = hash(key);
        for (Entry<K, V> e : buckets[idx]) {
            if (e.key.equals(key)) {
                e.value = value;
                return;
            }
        }
        buckets[idx].add(new Entry<>(key, value));
        size++;
        if ((double) size / buckets.length > LOAD_FACTOR) rehash();
    }

    public V get(K key) {
        int idx = hash(key);
        for (Entry<K, V> e : buckets[idx]) {
            if (e.key.equals(key)) return e.value;
        }
        return null;
    }

    public V remove(K key) {
        int idx = hash(key);
        var it = buckets[idx].iterator();
        while (it.hasNext()) {
            Entry<K, V> e = it.next();
            if (e.key.equals(key)) {
                it.remove();
                size--;
                return e.value;
            }
        }
        return null;
    }

    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    private void rehash() {
        var oldBuckets = buckets;
        buckets = new LinkedList[oldBuckets.length * 2];
        for (int i = 0; i < buckets.length; i++) buckets[i] = new LinkedList<>();
        size = 0;
        for (var bucket : oldBuckets) {
            for (Entry<K, V> e : bucket) put(e.key, e.value);
        }
    }

    public void print() {
        for (int i = 0; i < buckets.length; i++) {
            if (!buckets[i].isEmpty()) {
                System.out.print("Bucket " + i + ": ");
                for (Entry<K, V> e : buckets[i]) System.out.print("{" + e.key + "=" + e.value + "} ");
                System.out.println();
            }
        }
    }

    public static void main(String[] args) {
        HashTableDS<String, Integer> ht = new HashTableDS<>(4);
        ht.put("one", 1);
        ht.put("two", 2);
        ht.put("three", 3);
        ht.put("four", 4);
        ht.put("five", 5);
        System.out.println("Get 'three': " + ht.get("three"));
        System.out.println("Get 'six': " + ht.get("six"));
        System.out.println("Remove 'two': " + ht.remove("two"));
        System.out.println("Get 'two' after remove: " + ht.get("two"));
        System.out.println("Size: " + ht.size());
        ht.print();
    }
}
