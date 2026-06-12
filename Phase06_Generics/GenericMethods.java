package phase06.generics;

import java.util.Arrays;
import java.util.List;

public class GenericMethods {
    public static void main(String[] args) {
        // Generic method - type inference
        String[] strArray = {"apple", "banana", "cherry"};
        String middle = getMiddle(strArray);
        System.out.println("Middle of strings: " + middle);

        Integer[] intArray = {1, 2, 3, 4, 5};
        Integer midInt = getMiddle(intArray);
        System.out.println("Middle of ints: " + midInt);

        // Type inference with explicit type witness
        Double midDouble = GenericMethods.<Double>getMiddle(new Double[]{1.0, 2.0, 3.0});
        System.out.println("Middle of doubles (type witness): " + midDouble);

        // Generic method with two different types
        boolean isEqual = isEqual("hello", "hello");
        System.out.println("\nisEqual('hello', 'hello'): " + isEqual);
        System.out.println("isEqual(10, 20): " + isEqual(10, 20));

        // Generic varargs
        List<String> strList = toList("A", "B", "C", "D");
        System.out.println("\ntoList varargs: " + strList);

        List<Integer> intList = toList(1, 2, 3, 4, 5);
        System.out.println("toList ints: " + intList);

        // Generic method with bounded type parameter
        double sum = sumOfList(List.of(1, 2, 3, 4, 5));
        System.out.println("\nSum of numbers: " + sum);

        double sumDouble = sumOfList(List.of(1.5, 2.5, 3.5));
        System.out.println("Sum of doubles: " + sumDouble);

        // Generic method returning different types
        Integer[] ints = toArray(1, 2, 3, 4);
        System.out.println("\ntoArray: " + Arrays.toString(ints));
    }

    // Generic method
    public static <T> T getMiddle(T[] array) {
        return array[array.length / 2];
    }

    // Generic method with two type parameters
    public static <T, U> boolean isEqual(T a, U b) {
        return a.equals(b);
    }

    // Generic varargs
    @SafeVarargs
    public static <T> List<T> toList(T... elements) {
        return Arrays.asList(elements);
    }

    // Generic method with bounded type parameter
    public static <T extends Number> double sumOfList(List<T> list) {
        double sum = 0.0;
        for (T item : list) {
            sum += item.doubleValue();
        }
        return sum;
    }

    // Generic method returning array
    @SafeVarargs
    public static <T> T[] toArray(T... elements) {
        return elements;
    }
}
