package phase05.collections;

import java.util.TreeMap;

public class TreeMapExample {
    public static void main(String[] args) {
        // TreeMap with natural ordering
        TreeMap<String, Integer> map = new TreeMap<>();
        map.put("Charlie", 35);
        map.put("Alice", 30);
        map.put("Bob", 25);
        map.put("David", 40);
        map.put("Eve", 28);

        System.out.println("TreeMap (sorted by key): " + map);

        // First and last keys
        System.out.println("firstKey: " + map.firstKey());
        System.out.println("lastKey: " + map.lastKey());
        System.out.println("firstEntry: " + map.firstEntry());
        System.out.println("lastEntry: " + map.lastEntry());

        // Lower/floor/ceiling/higher
        System.out.println("lowerKey('C'): " + map.lowerKey("C"));
        System.out.println("floorKey('C'): " + map.floorKey("C"));
        System.out.println("ceilingKey('C'): " + map.ceilingKey("C"));
        System.out.println("higherKey('C'): " + map.higherKey("C"));

        // subMap - range view
        System.out.println("\nsubMap('B', 'D'): " + map.subMap("B", "D"));
        System.out.println("subMap('B', true, 'D', true): " + map.subMap("B", true, "D", true));

        // headMap - keys less than
        System.out.println("headMap('C'): " + map.headMap("C"));
        System.out.println("headMap('C', true): " + map.headMap("C", true));

        // tailMap - keys greater than or equal
        System.out.println("tailMap('C'): " + map.tailMap("C"));

        // descending map
        System.out.println("\ndescendingMap: " + map.descendingMap());

        // pollFirstEntry / pollLastEntry
        System.out.println("pollFirstEntry: " + map.pollFirstEntry());
        System.out.println("After pollFirst: " + map);

        // NavigableMap methods
        System.out.println("\nnavigableKeySet: " + map.navigableKeySet());
        System.out.println("descendingKeySet: " + map.descendingKeySet());
    }
}
