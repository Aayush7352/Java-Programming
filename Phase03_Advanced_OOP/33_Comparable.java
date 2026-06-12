package phase03.advancedoop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Comparable implements java.lang.Comparable<Comparable> {
    private final String name;
    private final double price;

    public Comparable(String name, double price) {
        this.name = name;
        this.price = price;
    }

    @Override
    public int compareTo(Comparable other) {
        return Double.compare(this.price, other.price);
    }

    @Override
    public String toString() {
        return name + " ($" + price + ")";
    }

    public static void main(String[] args) {
        List<Comparable> items = new ArrayList<>();
        items.add(new Comparable("Laptop", 1200.0));
        items.add(new Comparable("Mouse", 25.0));
        items.add(new Comparable("Monitor", 350.0));
        items.add(new Comparable("Keyboard", 80.0));

        System.out.println("Before sorting: " + items);
        Collections.sort(items);
        System.out.println("After sorting by price: " + items);

        List<String> words = new ArrayList<>(List.of("banana", "apple", "cherry", "date"));
        Collections.sort(words);
        System.out.println("String natural order: " + words);
    }
}
