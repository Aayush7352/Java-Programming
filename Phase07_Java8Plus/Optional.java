package phase07.java8plus;

public class Optional {
    public static void main(String[] args) {
        // of (non-null)
        var nonEmpty = java.util.Optional.of("Hello");
        System.out.println("of: " + nonEmpty);

        // ofNullable (may be null)
        var nullable = java.util.Optional.ofNullable(null);
        System.out.println("ofNullable(null): " + nullable);

        // empty
        java.util.Optional<String> empty = java.util.Optional.empty();
        System.out.println("empty: " + empty);

        // isPresent
        System.out.println("isPresent on nonEmpty: " + nonEmpty.isPresent());

        // ifPresent
        nonEmpty.ifPresent(v -> System.out.println("ifPresent: " + v));

        // orElse
        String val1 = empty.orElse("default");
        System.out.println("orElse: " + val1);

        // orElseGet
        String val2 = empty.orElseGet(() -> "supplied default");
        System.out.println("orElseGet: " + val2);

        // orElseThrow
        try {
            empty.orElseThrow(() -> new RuntimeException("value absent"));
        } catch (RuntimeException e) {
            System.out.println("orElseThrow caught: " + e.getMessage());
        }

        // map
        var mapped = nonEmpty.map(String::toUpperCase);
        System.out.println("map: " + mapped);

        // flatMap
        var flatMapped = nonEmpty.flatMap(v -> java.util.Optional.of(v + " world"));
        System.out.println("flatMap: " + flatMapped);

        // filter
        var filtered = nonEmpty.filter(v -> v.startsWith("H"));
        System.out.println("filter (starts H): " + filtered);
        var filtered2 = nonEmpty.filter(v -> v.startsWith("X"));
        System.out.println("filter (starts X): " + filtered2);

        // or (Java 9+)
        var orResult = java.util.Optional.<String>empty()
                .or(() -> java.util.Optional.of("fallback"));
        System.out.println("or: " + orResult);

        // ifPresentOrElse (Java 9+)
        empty.ifPresentOrElse(
                v -> System.out.println("not called"),
                () -> System.out.println("ifPresentOrElse: empty branch")
        );
    }
}
