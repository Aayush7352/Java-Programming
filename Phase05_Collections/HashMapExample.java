package phase05.collections;

import java.util.HashMap;
import java.util.Map;

public class HashMapExample {
    public static void main(String[] args) {
        // HashMap basic operations
        HashMap<String, Integer> map = new HashMap<>();

        // put
        map.put("Alice", 30);
        map.put("Bob", 25);
        map.put("Charlie", 35);
        map.put("Alice", 31); // overwrites
        System.out.println("Map: " + map);

        // get
        System.out.println("get('Alice'): " + map.get("Alice"));
        System.out.println("get('Missing'): " + map.get("Missing"));
        System.out.println("getOrDefault('Missing', 0): " + map.getOrDefault("Missing", 0));

        // remove
        map.remove("Charlie");
        System.out.println("After remove Charlie: " + map);

        // containsKey / containsValue
        System.out.println("containsKey('Bob'): " + map.containsKey("Bob"));
        System.out.println("containsValue(25): " + map.containsValue(25));

        // Iteration over entries
        System.out.println("\nIteration:");
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }

        // computeIfAbsent
        map.computeIfAbsent("David", k -> 40);
        map.computeIfAbsent("Alice", k -> 99); // not computed, already present
        System.out.println("\nAfter computeIfAbsent: " + map);

        // computeIfPresent
        map.computeIfPresent("Bob", (k, v) -> v + 5);
        System.out.println("After computeIfPresent Bob (+5): " + map);

        // merge
        map.merge("Alice", 10, Integer::sum);
        map.merge("Eve", 50, Integer::sum); // key absent, puts 50
        System.out.println("After merge: " + map);

        // putIfAbsent
        map.putIfAbsent("Bob", 100); // not updated
        System.out.println("After putIfAbsent Bob: " + map);
    }
}
