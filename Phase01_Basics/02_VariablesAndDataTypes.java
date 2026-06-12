package phase01.basics;

class VariablesAndDataTypes {
    public static void main(String[] args) {
        // Primitive types
        byte b = 127;
        short s = 32_767;
        int i = 2_147_483_647;
        long l = 9_223_372_036_854_775_807L;
        float f = 3.1415f;
        double d = 3.141592653589793;
        char c = 'J';
        boolean bool = true;

        System.out.println("byte: " + b);
        System.out.println("short: " + s);
        System.out.println("int: " + i);
        System.out.println("long: " + l);
        System.out.println("float: " + f);
        System.out.println("double: " + d);
        System.out.println("char: " + c);
        System.out.println("boolean: " + bool);

        // var keyword (Java 10+)
        var message = "Inferred as String";
        var number = 42;
        var pi = 3.14;
        System.out.println("\nvar keyword:");
        System.out.println("message: " + message + " (" + message.getClass().getSimpleName() + ")");
        System.out.println("number: " + number + " (" + ((Object) number).getClass().getSimpleName() + ")");
        System.out.println("pi: " + pi + " (" + ((Object) pi).getClass().getSimpleName() + ")");

        // Text blocks (Java 15+)
        String json = """
                {
                    "name": "Java 21",
                    "type": "Programming Language",
                    "features": ["Pattern Matching", "Records", "Sealed Classes", "Text Blocks"]
                }
                """;
        System.out.println("\nText block (JSON):\n" + json);

        String sql = """
                SELECT id, name, salary
                FROM employees
                WHERE salary > 50000
                ORDER BY salary DESC
                """;
        System.out.println("Text block (SQL):\n" + sql);

        // Underscores in numeric literals
        int million = 1_000_000;
        // Note: not a real credit card number, just demo underscores in literals
        System.out.println("1 million: " + million);
        long creditCard = 1234_5678_9012_3456L;
        System.out.println("Credit card (demo): " + creditCard);
    }
}
