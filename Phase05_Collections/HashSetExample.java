package phase05.collections;

import java.util.HashSet;
import java.util.Objects;

public class HashSetExample {
    public static void main(String[] args) {
        // Basic HashSet - no duplicates
        HashSet<String> set = new HashSet<>();
        set.add("Apple");
        set.add("Banana");
        set.add("Cherry");
        set.add("Apple"); // duplicate, ignored
        set.add("Banana"); // duplicate, ignored

        System.out.println("HashSet: " + set);
        System.out.println("Size (expected 3): " + set.size());
        System.out.println("Contains 'Banana': " + set.contains("Banana"));
        System.out.println("Contains 'Mango': " + set.contains("Mango"));

        // Iteration order is not guaranteed
        HashSet<Integer> numbers = new HashSet<>();
        numbers.add(100);
        numbers.add(5);
        numbers.add(23);
        numbers.add(7);
        numbers.add(42);
        System.out.println("\nIteration order (not guaranteed): " + numbers);

        // hashCode/equals impact
        System.out.println("\nhashCode/equals impact:");
        HashSet<Person> people = new HashSet<>();
        people.add(new Person("Alice", 30));
        people.add(new Person("Bob", 25));
        people.add(new Person("Alice", 30)); // duplicate if equals/hashCode implemented
        people.add(new Person("Charlie", 35));
        System.out.println("People set size: " + people.size());

        // Without proper equals/hashCode
        HashSet<BadPerson> badPeople = new HashSet<>();
        badPeople.add(new BadPerson("Alice", 30));
        badPeople.add(new BadPerson("Alice", 30)); // NOT treated as duplicate
        System.out.println("BadPeople set size (no equals/hashCode): " + badPeople.size());

        // removeIf
        set.removeIf(s -> s.startsWith("A"));
        System.out.println("\nAfter removeIf(starts with A): " + set);
    }

    record Person(String name, int age) {}

    static class BadPerson {
        String name;
        int age;
        BadPerson(String name, int age) { this.name = name; this.age = age; }
        // No equals/hashCode override - object identity only
        @Override public String toString() { return name + ":" + age; }
    }
}
