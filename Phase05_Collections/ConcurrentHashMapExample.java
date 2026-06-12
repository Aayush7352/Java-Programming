package phase05.collections;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class ConcurrentHashMapExample {
    public static void main(String[] args) {
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        System.out.println("Initial: " + map);

        // compute
        map.compute("A", (k, v) -> v == null ? 1 : v + 10);
        System.out.println("After compute A (+10): " + map);

        // computeIfAbsent
        map.computeIfAbsent("D", k -> 4);
        map.computeIfAbsent("A", k -> 99); // not computed
        System.out.println("After computeIfAbsent: " + map);

        // computeIfPresent
        map.computeIfPresent("B", (k, v) -> v * 10);
        System.out.println("After computeIfPresent B (*10): " + map);

        // merge
        map.merge("C", 100, Integer::sum);
        map.merge("E", 5, Integer::sum); // absent, puts 5
        System.out.println("After merge: " + map);

        // forEachKey (Java 21)
        System.out.print("\nforEachKey (parallel): ");
        map.forEachKey(1, k -> System.out.print(k + " "));
        System.out.println();

        // forEach (key-value)
        System.out.println("forEach (parallel):");
        map.forEach(1, (k, v) -> System.out.println("  " + k + " -> " + v));

        // search
        String result = map.search(1, (k, v) -> v > 3 ? k : null);
        System.out.println("search (value > 3): " + result);

        // reduce
        int sum = map.reduceValues(1, (a, b) -> a + b);
        System.out.println("reduceValues sum: " + sum);

        // Thread-safe counter using ConcurrentHashMap + LongAdder
        System.out.println("\nThread-safe counter:");
        ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();
        counters.computeIfAbsent("visits", k -> new LongAdder()).increment();
        counters.computeIfAbsent("visits", k -> new LongAdder()).increment();
        counters.computeIfAbsent("visits", k -> new LongAdder()).increment();
        System.out.println("Visits: " + counters.get("visits").sum());
    }
}
