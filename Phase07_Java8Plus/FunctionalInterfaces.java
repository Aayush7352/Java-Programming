package phase07.java8plus;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@FunctionalInterface
interface MathOperation {
    int operate(int a, int b);
}

public class FunctionalInterfaces {
    public static void main(String[] args) {
        // Predicate<T>
        Predicate<String> isEmpty = s -> s.isEmpty();
        System.out.println("isEmpty test: " + isEmpty.test(""));

        // Function<T,R>
        Function<String, Integer> lengthFunc = s -> s.length();
        System.out.println("length of 'Java': " + lengthFunc.apply("Java"));

        // Consumer<T>
        Consumer<String> printer = s -> System.out.println(">> " + s);
        printer.accept("Hello from Consumer");

        // Supplier<T>
        Supplier<Double> randomSupplier = () -> Math.random();
        System.out.println("Random: " + randomSupplier.get());

        // BinaryOperator<T>
        BinaryOperator<Integer> sum = (a, b) -> a + b;
        System.out.println("BinaryOperator sum: " + sum.apply(10, 20));

        // custom @FunctionalInterface
        MathOperation multiply = (a, b) -> a * b;
        System.out.println("Custom FI multiply: " + multiply.operate(4, 5));

        // chaining with andThen / compose
        Function<String, String> quote = s -> "'" + s + "'";
        Function<String, String> upper = s -> s.toUpperCase();
        var quotedUpper = quote.compose(upper);
        System.out.println("Composed: " + quotedUpper.apply("hello"));

        // Predicate.and / or / negate
        Predicate<Integer> positive = n -> n > 0;
        Predicate<Integer> even = n -> n % 2 == 0;
        var positiveEven = positive.and(even);
        System.out.println("positiveEven.test(4): " + positiveEven.test(4));
        System.out.println("positiveEven.test(3): " + positiveEven.test(3));
    }
}
