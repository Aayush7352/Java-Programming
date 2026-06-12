package phase06.generics;

public class BoundedTypes {
    public static void main(String[] args) {
        // Single bound - T extends Number
        NumberBox<Integer> intBox = new NumberBox<>(42);
        NumberBox<Double> doubleBox = new NumberBox<>(3.14);
        // NumberBox<String> stringBox; // compilation error - String is not Number

        System.out.println("Integer box double value: " + intBox.getDoubleValue());
        System.out.println("Double box int value: " + doubleBox.getIntValue());

        // Multiple bounds - T extends Comparable<T> & Displayable & Resettable
        StringWrapper sw = new StringWrapper("Hello");
        MultiBound<StringWrapper> mb = new MultiBound<>(sw);
        mb.display();
        mb.reset();
        System.out.println("MultiBound compareTo with new StringWrapper('World'): "
                + mb.compareTo(new StringWrapper("World")));

        // Bounded type with Comparable
        System.out.println("\nMax of 3, 7, 5: " + findMax(3, 7, 5));
        System.out.println("Max of 'apple', 'zebra', 'banana': " + findMax("apple", "zebra", "banana"));

        // Bounded type with Number operations
        System.out.println("\nSum of ints: " + sum(10, 20));
        System.out.println("Sum of doubles: " + sum(2.5, 3.7));
    }

    // Bounded type parameter with Comparable
    public static <T extends Comparable<T>> T findMax(T a, T b, T c) {
        T max = a;
        if (b.compareTo(max) > 0) max = b;
        if (c.compareTo(max) > 0) max = c;
        return max;
    }

    // Bounded type with Number
    public static <T extends Number> double sum(T a, T b) {
        return a.doubleValue() + b.doubleValue();
    }
}

// Single bound: T extends Number
class NumberBox<T extends Number> {
    private final T value;

    public NumberBox(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public double getDoubleValue() {
        return value.doubleValue();
    }

    public int getIntValue() {
        return value.intValue();
    }
}

// Interfaces for multiple bounds demo
interface Displayable {
    void display();
}

interface Resettable {
    void reset();
}

// Multiple bounds
class MultiBound<T extends Comparable<T> & Displayable & Resettable> {
    private T item;

    public MultiBound(T item) {
        this.item = item;
    }

    public void display() {
        item.display();
    }

    public void reset() {
        item.reset();
    }

    public int compareTo(T other) {
        return item.compareTo(other);
    }
}

// Class satisfying all three bounds
class StringWrapper implements Comparable<StringWrapper>, Displayable, Resettable {
    private final String value;

    StringWrapper(String value) {
        this.value = value;
    }

    @Override
    public int compareTo(StringWrapper other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public void display() {
        System.out.println("StringWrapper: " + value);
    }

    @Override
    public void reset() {
        System.out.println("Reset called on: " + value);
    }
}
