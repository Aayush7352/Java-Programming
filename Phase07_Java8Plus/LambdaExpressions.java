package phase07.java8plus;

import java.util.List;
import java.util.function.Consumer;

interface StringProcessor {
    String process(String s);
}

public class LambdaExpressions {
    public static void main(String[] args) {
        // (params) -> expr
        StringProcessor toUpper = s -> s.toUpperCase();
        System.out.println("toUpper: " + toUpper.process("hello"));

        // (params) -> { stmts }
        StringProcessor reverse = s -> {
            var sb = new StringBuilder(s);
            return sb.reverse().toString();
        };
        System.out.println("reverse: " + reverse.process("lambda"));

        // lambda as argument
        List<String> names = List.of("Alice", "Bob", "Charlie");
        names.forEach(name -> System.out.println("Hello, " + name));

        // variable capture / effectively final
        String suffix = "!";
        Consumer<String> greet = msg -> System.out.println(msg + suffix);
        greet.accept("Hi");

        // effectively final — reassignment commented out so it stays effectively final
        // suffix = "?";  // would cause compile error because suffix is captured
        var captured = List.of(1, 2, 3);
        captured.forEach(n -> System.out.println(n + 10));
    }
}
