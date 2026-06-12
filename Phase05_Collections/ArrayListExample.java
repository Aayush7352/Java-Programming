package phase05.collections;

import java.util.ArrayList;
import java.util.ListIterator;

public class ArrayListExample {
    public static void main(String[] args) {
        // CRUD operations
        ArrayList<String> list = new ArrayList<>();
        list.add("Apple");
        list.add("Banana");
        list.add("Cherry");
        list.add("Date");
        System.out.println("Initial list: " + list);

        // Read
        System.out.println("Element at index 2: " + list.get(2));

        // Update
        list.set(1, "Blueberry");
        System.out.println("After update: " + list);

        // Delete
        list.remove("Date");
        list.remove(0);
        System.out.println("After deletions: " + list);

        // ListIterator forward
        System.out.print("ListIterator forward: ");
        ListIterator<String> lit = list.listIterator();
        while (lit.hasNext()) {
            System.out.print(lit.next() + " ");
        }
        System.out.println();

        // ListIterator backward
        System.out.print("ListIterator backward: ");
        while (lit.hasPrevious()) {
            System.out.print(lit.previous() + " ");
        }
        System.out.println();

        // ListIterator modification
        lit = list.listIterator();
        while (lit.hasNext()) {
            String s = lit.next();
            if (s.equals("Cherry")) {
                lit.set("Coconut");
            }
        }
        System.out.println("After ListIterator set: " + list);

        // forEach
        System.out.print("forEach: ");
        list.forEach(s -> System.out.print(s + " "));
        System.out.println();

        // removeIf
        list.add("Apple");
        list.add("Apricot");
        list.removeIf(s -> s.startsWith("A"));
        System.out.println("After removeIf (starts with A): " + list);
    }
}
