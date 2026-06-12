package phase01.basics;

import java.util.StringJoiner;

class Strings {
    public static void main(String[] args) {
        // String immutability
        System.out.println("=== String Immutability ===");
        String s1 = "Hello";
        String s2 = s1.concat(" World");
        System.out.println("s1: " + s1 + " (unchanged)");
        System.out.println("s2: " + s2 + " (new string)");

        // StringBuilder (mutable, not thread-safe)
        System.out.println("\n=== StringBuilder ===");
        StringBuilder sb = new StringBuilder("Java");
        sb.append(" 21");
        sb.insert(5, "Version ");
        sb.replace(0, 4, "OpenJDK");
        System.out.println("StringBuilder: " + sb);
        System.out.println("Reverse: " + sb.reverse());

        // StringBuffer (mutable, thread-safe)
        System.out.println("\n=== StringBuffer ===");
        StringBuffer sbf = new StringBuffer("Concurrent");
        sbf.append(" Safe");
        sbf.insert(10, " - ");
        System.out.println("StringBuffer: " + sbf);

        // StringJoiner
        System.out.println("\n=== StringJoiner ===");
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        joiner.add("Apple");
        joiner.add("Banana");
        joiner.add("Cherry");
        System.out.println("StringJoiner: " + joiner);

        // Text blocks (Java 15+)
        System.out.println("\n=== Text Blocks ===");
        String html = """
                <html>
                    <body>
                        <h1>Hello, Java 21!</h1>
                        <p>Text blocks make multi-line strings easy.</p>
                    </body>
                </html>
                """;
        System.out.println(html);

        // strip() vs trim()
        System.out.println("=== strip() ===");
        String padded = "  \u2000  Hello  \u2000  ";
        System.out.println("trim():   '" + padded.trim() + "'");
        System.out.println("strip():  '" + padded.strip() + "'");
        System.out.println("stripLeading():  '" + padded.stripLeading() + "'");
        System.out.println("stripTrailing(): '" + padded.stripTrailing() + "'");

        // repeat
        System.out.println("\n=== repeat() ===");
        System.out.println("Ha".repeat(3));
        System.out.println("=".repeat(30));

        // indent
        System.out.println("\n=== indent() ===");
        String code = "int x = 1;\nint y = 2;";
        System.out.println("Indented:\n" + code.indent(4));

        // transform (Java 12+)
        System.out.println("\n=== transform() ===");
        String transformed = "hello".transform(str -> str.toUpperCase());
        System.out.println("transformed: " + transformed);

        // lines()
        System.out.println("\n=== lines() ===");
        "line1\nline2\nline3".lines().forEach(System.out::println);

        // isBlank (Java 11+)
        System.out.println("\n=== isBlank() ===");
        System.out.println("'   ' is blank: " + "   ".isBlank());
        System.out.println("'abc' is blank: " + "abc".isBlank());

        // Formatted string
        System.out.println("\n=== formatted ===");
        String formatted = "Name: %s, Age: %d".formatted("Alice", 30);
        System.out.println(formatted);
    }
}
