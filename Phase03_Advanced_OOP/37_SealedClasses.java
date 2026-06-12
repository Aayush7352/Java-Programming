package phase03.advancedoop;

sealed interface Shape permits Circle, Rectangle, Triangle {}

final class Circle implements Shape {
    private final double radius;
    public Circle(double radius) { this.radius = radius; }
    public double radius() { return radius; }
}

final class Rectangle implements Shape {
    private final double width, height;
    public Rectangle(double width, double height) { this.width = width; this.height = height; }
    public double width() { return width; }
    public double height() { return height; }
}

non-sealed class Triangle implements Shape {
    private final double side1, side2, side3;
    public Triangle(double side1, double side2, double side3) { this.side1 = side1; this.side2 = side2; this.side3 = side3; }
    public double side1() { return side1; }
    public double side2() { return side2; }
    public double side3() { return side3; }
}

class SealedClasses {
    public static double area(Shape shape) {
        return switch (shape) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
            case Triangle t -> {
                double s = (t.side1() + t.side2() + t.side3()) / 2;
                yield Math.sqrt(s * (s - t.side1()) * (s - t.side2()) * (s - t.side3()));
            }
        };
    }

    public static void main(String[] args) {
        Shape circle = new Circle(5);
        Shape rect = new Rectangle(4, 6);
        Shape tri = new Triangle(3, 4, 5);

        System.out.println("Circle area: " + area(circle));
        System.out.println("Rectangle area: " + area(rect));
        System.out.println("Triangle area: " + area(tri));

        System.out.println("\nSealed hierarchy: Shape -> Circle, Rectangle, Triangle");
        System.out.println("Exhaustive switch covers all permitted subtypes.");
    }
}
