package phase01.basics;

class Operators {
    public static void main(String[] args) {
        int a = 15, b = 4;

        // Arithmetic
        System.out.println("=== Arithmetic Operators ===");
        System.out.println("a + b = " + (a + b));
        System.out.println("a - b = " + (a - b));
        System.out.println("a * b = " + (a * b));
        System.out.println("a / b = " + (a / b));
        System.out.println("a %% b = " + (a % b));

        // Relational
        System.out.println("\n=== Relational Operators ===");
        System.out.println("a == b: " + (a == b));
        System.out.println("a != b: " + (a != b));
        System.out.println("a > b: " + (a > b));
        System.out.println("a < b: " + (a < b));
        System.out.println("a >= b: " + (a >= b));
        System.out.println("a <= b: " + (a <= b));

        // Logical
        boolean x = true, y = false;
        System.out.println("\n=== Logical Operators ===");
        System.out.println("x && y: " + (x && y));
        System.out.println("x || y: " + (x || y));
        System.out.println("!x: " + (!x));

        // Bitwise
        System.out.println("\n=== Bitwise Operators ===");
        System.out.println("a & b: " + (a & b));
        System.out.println("a | b: " + (a | b));
        System.out.println("a ^ b: " + (a ^ b));
        System.out.println("~a: " + (~a));
        System.out.println("a << 2: " + (a << 2));
        System.out.println("a >> 2: " + (a >> 2));
        System.out.println("a >>> 2: " + (a >>> 2));

        // Assignment, compound
        int c = 10;
        c += 5;
        System.out.println("\n=== Assignment Operators ===");
        System.out.println("c += 5: " + c);
        c -= 3;
        System.out.println("c -= 3: " + c);
        c *= 2;
        System.out.println("c *= 2: " + c);
        c /= 4;
        System.out.println("c /= 4: " + c);
        c %= 3;
        System.out.println("c %%= 3: " + c);

        // Ternary
        System.out.println("\n=== Ternary Operator ===");
        int age = 20;
        String status = (age >= 18) ? "Adult" : "Minor";
        System.out.println("age=20 => " + status);
        age = 15;
        status = (age >= 18) ? "Adult" : "Minor";
        System.out.println("age=15 => " + status);

        // Instanceof (Java 16+ pattern matching)
        System.out.println("\n=== instanceof Operator ===");
        Object obj = "Hello, Java 21!";
        if (obj instanceof String str && str.length() > 5) {
            System.out.println("String length: " + str.length());
        }
    }
}
