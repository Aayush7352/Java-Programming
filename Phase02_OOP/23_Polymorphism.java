package phase02.oop;

// Compile-time polymorphism (overloading)
class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public int add(int a, int b, int c) {
        return a + b + c;
    }

    public double add(double a, double b) {
        return a + b;
    }

    public String add(String a, String b) {
        return a + b;
    }
}

// Base for runtime polymorphism
abstract class Payment {
    abstract void processPayment(double amount);
}

class CreditCardPayment extends Payment {
    @Override
    void processPayment(double amount) {
        System.out.printf("Processing $%.2f via Credit Card%n", amount);
    }
}

class PayPalPayment extends Payment {
    @Override
    void processPayment(double amount) {
        System.out.printf("Processing $%.2f via PayPal%n", amount);
    }
}

class CryptoPayment extends Payment {
    @Override
    void processPayment(double amount) {
        System.out.printf("Processing $%.2f via Cryptocurrency%n", amount);
    }
}

// Sealed hierarchy for pattern matching demo (Java 17+)
sealed interface Shape permits CircleS, RectangleS {}
final record CircleS(double radius) implements Shape {}
final record RectangleS(double length, double width) implements Shape {}

class Polymorphism {
    public static void main(String[] args) {
        // Compile-time polymorphism (method overloading)
        System.out.println("=== Compile-Time Polymorphism (Overloading) ===");
        Calculator calc = new Calculator();
        System.out.println("add(int, int): " + calc.add(10, 20));
        System.out.println("add(int, int, int): " + calc.add(10, 20, 30));
        System.out.println("add(double, double): " + calc.add(10.5, 20.5));
        System.out.println("add(String, String): " + calc.add("Hello", " World"));

        // Runtime polymorphism (method overriding)
        System.out.println("\n=== Runtime Polymorphism (Overriding) ===");
        Payment payment;
        payment = new CreditCardPayment();
        payment.processPayment(99.99);

        payment = new PayPalPayment();
        payment.processPayment(149.99);

        payment = new CryptoPayment();
        payment.processPayment(0.05);

        // instanceof with pattern matching (Java 16+)
        System.out.println("\n=== instanceof Pattern Matching (Java 16+) ===");
        Object obj = "Hello, Java 21!";
        if (obj instanceof String s) {
            System.out.println("String length: " + s.length());
        }

        // Pattern matching for switch (Java 21)
        System.out.println("\n=== Pattern Matching for switch (Java 21) ===");
        Shape shape = new CircleS(5.0);
        String result = switch (shape) {
            case null -> "No shape";
            case CircleS c -> "Circle with radius " + c.radius();
            case RectangleS r -> "Rectangle " + r.length() + "x" + r.width();
        };
        System.out.println("Shape: " + result);

        // Record pattern matching (Java 21)
        System.out.println("\n=== Record Patterns (Java 21) ===");
        Object shapeObj = new RectangleS(4, 5);
        if (shapeObj instanceof RectangleS(double w, double h)) {
            System.out.println("Deconstructed Rectangle: " + w + " x " + h);
        }

        // Demonstrating polymorphic arrays
        System.out.println("\n=== Polymorphic Array ===");
        Payment[] payments = {
            new CreditCardPayment(),
            new PayPalPayment(),
            new CryptoPayment()
        };
        for (Payment p : payments) {
            p.processPayment(50.0);
        }
    }
}
