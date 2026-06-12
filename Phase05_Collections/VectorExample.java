package phase05.collections;

import java.util.Enumeration;
import java.util.Vector;

public class VectorExample {
    public static void main(String[] args) {
        // Vector is a legacy synchronized dynamic array
        Vector<String> vector = new Vector<>(5, 3); // initialCapacity=5, capacityIncrement=3
        System.out.println("Initial capacity: " + vector.capacity());

        vector.add("One");
        vector.add("Two");
        vector.add("Three");
        vector.add("Four");
        vector.add("Five");
        System.out.println("Size: " + vector.size() + ", Capacity: " + vector.capacity());

        // Adding more elements triggers capacity increment
        vector.add("Six");
        System.out.println("After adding 6th element - Size: " + vector.size() + ", Capacity: " + vector.capacity());

        // Enumeration iteration (legacy)
        System.out.print("Enumeration: ");
        Enumeration<String> e = vector.elements();
        while (e.hasMoreElements()) {
            System.out.print(e.nextElement() + " ");
        }
        System.out.println();

        // CRUD
        vector.add(2, "Inserted");
        System.out.println("After insert: " + vector);
        System.out.println("get(3): " + vector.get(3));
        vector.set(1, "Updated");
        System.out.println("After set: " + vector);
        vector.remove("Three");
        System.out.println("After remove 'Three': " + vector);

        // Capacity management
        System.out.println("\nCapacity management:");
        Vector<Integer> v2 = new Vector<>();
        System.out.println("Default initial capacity: " + v2.capacity());
        for (int i = 0; i < 15; i++) v2.add(i);
        System.out.println("After 15 adds - Size: " + v2.size() + ", Capacity: " + v2.capacity());

        v2.trimToSize();
        System.out.println("After trimToSize - Size: " + v2.size() + ", Capacity: " + v2.capacity());

        v2.ensureCapacity(30);
        System.out.println("After ensureCapacity(30) - Capacity: " + v2.capacity());
    }
}
