package phase05.collections;

import java.util.LinkedHashSet;

public class LinkedHashSetExample {
    public static void main(String[] args) {
        // LinkedHashSet maintains insertion order
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add("Zebra");
        set.add("Apple");
        set.add("Monkey");
        set.add("Dog");
        set.add("Cat");
        set.add("Apple"); // duplicate, ignored

        System.out.println("LinkedHashSet (insertion order preserved):");
        System.out.println(set);

        // Compare with HashSet
        System.out.println("\nComparison - first element is always 'Zebra' in LinkedHashSet");
        System.out.println("  First: " + set.iterator().next());

        // Duplicate elements don't change order
        set.add("Monkey"); // duplicate, ignored
        System.out.println("\nAfter re-adding 'Monkey': " + set);

        // Remove and re-add - element moves to end
        set.remove("Dog");
        set.add("Dog");
        System.out.println("After removing and re-adding 'Dog': " + set);

        // Standard Set operations
        System.out.println("\nContains 'Cat': " + set.contains("Cat"));
        System.out.println("Size: " + set.size());

        set.remove("Zebra");
        System.out.println("After removing 'Zebra': " + set);
    }
}
