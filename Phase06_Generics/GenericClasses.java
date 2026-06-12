package phase06.generics;

public class GenericClasses {
    public static void main(String[] args) {
        // Single type parameter
        Box<String> stringBox = new Box<>("Hello Generics");
        System.out.println("String box: " + stringBox.getContent());
        stringBox.setContent("Updated");
        System.out.println("Updated: " + stringBox.getContent());

        Box<Integer> intBox = new Box<>(42);
        System.out.println("Integer box: " + intBox.getContent());

        // Multiple type parameters
        Pair<String, Integer> pair = new Pair<>("Age", 30);
        System.out.println("\nPair: key=" + pair.getKey() + ", value=" + pair.getValue());

        Pair<String, Pair<String, Integer>> nested = new Pair<>("Person",
                new Pair<>("Age", 25));
        System.out.println("Nested pair: " + nested.getKey() + " -> " +
                nested.getValue().getKey() + "=" + nested.getValue().getValue());

        // Generic constructor
        GenericConstructor gc = new GenericConstructor(100);
        System.out.println("\nGeneric constructor value: " + gc.getValue());

        GenericConstructor gc2 = new GenericConstructor("Test");
        System.out.println("Generic constructor string: " + gc2.getValue());
    }
}

// Single type parameter
class Box<T> {
    private T content;

    public Box(T content) {
        this.content = content;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }
}

// Multiple type parameters
class Pair<K, V> {
    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}

// Generic constructor
class GenericConstructor {
    private final Object value;

    public <T> GenericConstructor(T value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
