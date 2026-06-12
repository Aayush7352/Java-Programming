package phase06.generics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Wildcards {
    public static void main(String[] args) {
        // Unbounded wildcard
        List<String> strings = Arrays.asList("A", "B", "C");
        List<Integer> integers = Arrays.asList(1, 2, 3);
        printList(strings);
        printList(integers);

        // Covariant wildcard (? extends T) - PRODUCER
        List<Integer> ints = Arrays.asList(1, 2, 3);
        List<Double> doubles = Arrays.asList(1.5, 2.5, 3.5);
        List<Number> numbers = new ArrayList<>();

        System.out.println("\nCovariant (? extends):");
        double sum = sum(ints);
        System.out.println("Sum of ints: " + sum);
        sum = sum(doubles);
        System.out.println("Sum of doubles: " + sum);

        // Cannot ADD to ? extends (except null)
        // List<? extends Number> covariant = ints;
        // covariant.add(4); // compilation error

        // Contravariant wildcard (? super T) - CONSUMER
        System.out.println("\nContravariant (? super):");
        List<? super Integer> contravariant = numbers;
        contravariant.add(10);
        contravariant.add(20);
        System.out.println("Added to numbers via ? super Integer: " + numbers);

        // Can read as Object from ? super
        Object obj = contravariant.get(0);
        System.out.println("Read from ? super as Object: " + obj);

        // PECS principle examples
        System.out.println("\n--- PECS Principle ---");

        // Producer extends - copy src (producer) to dest
        List<Integer> src = Arrays.asList(1, 2, 3, 4, 5);
        List<Number> dest = new ArrayList<>();
        copy(src, dest);
        System.out.println("Copy result: " + dest);

        // Consumer super - addAll takes ? extends E
        List<Number> nums = new ArrayList<>();
        nums.addAll(ints); // Integer extends Number
        nums.addAll(doubles); // Double extends Number
        System.out.println("\naddAll with wildcard: " + nums);

        // Wildcard with custom class hierarchy
        System.out.println("\n--- Hierarchy example ---");
        List<Dog> dogs = Arrays.asList(new Dog(), new Dog());
        List<Cat> cats = Arrays.asList(new Cat(), new Cat());
        List<Animal> animals = new ArrayList<>();

        addAnimals(dogs, animals);
        addAnimals(cats, animals);
        System.out.println("Animals count: " + animals.size());
    }

    // Unbounded wildcard
    public static void printList(List<?> list) {
        System.out.print("List: ");
        for (Object elem : list) {
            System.out.print(elem + " ");
        }
        System.out.println();
    }

    // Covariant (? extends) - producer
    public static double sum(List<? extends Number> list) {
        double sum = 0.0;
        for (Number n : list) {
            sum += n.doubleValue();
        }
        return sum;
    }

    // PECS: copy with producer extends, consumer super
    public static <T> void copy(List<? extends T> src, List<? super T> dest) {
        for (T item : src) {
            dest.add(item);
        }
    }

    // PECS: add animals
    public static <T extends Animal> void addAnimals(
            List<? extends T> producer,
            List<? super T> consumer) {
        consumer.addAll(producer);
    }

    // Hierarchy classes
    interface Animal {}
    record Dog() implements Animal {}
    record Cat() implements Animal {}
}
