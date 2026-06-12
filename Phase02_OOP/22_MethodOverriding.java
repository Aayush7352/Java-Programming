package phase02.oop;

class Shape {
    public String getName() {
        return "Shape";
    }

    public Shape getInstance() {
        return this;
    }

    public final void displayCategory() {
        System.out.println("This is a geometric shape.");
    }
}

class Circle extends Shape {
    private double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public String getName() {
        return "Circle";
    }

    // Covariant return type
    @Override
    public Circle getInstance() {
        return this;
    }

    public double area() {
        return Math.PI * radius * radius;
    }

    public double getRadius() {
        return radius;
    }

    // Cannot override displayCategory() because it's final
}

class Rectangle extends Shape {
    private double length, width;

    public Rectangle(double length, double width) {
        this.length = length;
        this.width = width;
    }

    @Override
    public String getName() {
        return "Rectangle";
    }

    @Override
    public Rectangle getInstance() {
        return this;
    }

    public double area() {
        return length * width;
    }
}

class MethodOverriding {
    public static void main(String[] args) {
        System.out.println("=== Method Overriding ===");
        Shape circle = new Circle(5.0);
        Shape rectangle = new Rectangle(4.0, 6.0);

        System.out.println("Circle name: " + circle.getName());
        System.out.println("Rectangle name: " + rectangle.getName());

        // Polymorphic behavior
        System.out.println("\n=== Polymorphic Method Calls ===");
        printShapeInfo(circle);
        printShapeInfo(rectangle);

        // Covariant return types
        System.out.println("\n=== Covariant Return Types ===");
        Circle c = (Circle) circle.getInstance();
        System.out.println("Circle radius: " + c.getRadius());

        Rectangle r = (Rectangle) rectangle.getInstance();
        System.out.println("Rectangle area: " + r.area());

        // super to call parent method from subclass
        System.out.println("\n=== @Override Annotation ===");
        System.out.println("Both getName() methods are annotated with @Override");

        // Final method demonstration
        System.out.println("\n=== Final Method ===");
        circle.displayCategory();
        rectangle.displayCategory();

        // Runtime polymorphism
        System.out.println("\n=== Runtime Polymorphism ===");
        Shape shape1 = new Circle(3.0);
        Shape shape2 = new Rectangle(2.0, 5.0);
        System.out.println("shape1 name: " + shape1.getName());
        System.out.println("shape2 name: " + shape2.getName());
    }

    public static void printShapeInfo(Shape shape) {
        System.out.println("Shape: " + shape.getName()
                + ", Type: " + shape.getClass().getSimpleName());
    }
}
