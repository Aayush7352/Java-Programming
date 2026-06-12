package phase02.oop;

import java.util.List;

// =============================================
// 1. SRP - Single Responsibility Principle
// A class should have only one reason to change
// =============================================

// Violates SRP: handles both employee data AND report generation
class BadEmployee {
    private String name;
    private double salary;

    public BadEmployee(String name, double salary) {
        this.name = name;
        this.salary = salary;
    }

    public String getName() { return name; }
    public double getSalary() { return salary; }

    // Violates SRP - this class should not be responsible for report generation
    public String generateReport() {
        return "Employee: " + name + ", Salary: $" + salary;
    }
}

// Follows SRP: separate classes for separate concerns
class Employee {
    private String name;
    private double salary;

    public Employee(String name, double salary) {
        this.name = name;
        this.salary = salary;
    }

    public String getName() { return name; }
    public double getSalary() { return salary; }
}

class EmployeeReportGenerator {
    public static String generate(Employee emp) {
        return "Employee: " + emp.getName() + ", Salary: $" + String.format("%,.2f", emp.getSalary());
    }
}

// =============================================
// 2. OCP - Open/Closed Principle
// Open for extension, closed for modification
// =============================================

// Violates OCP - adding a new shape requires modifying this class
class BadAreaCalculator {
    public double calculateArea(Object shape) {
        if (shape instanceof CircleOCP c) {
            return Math.PI * c.radius() * c.radius();
        } else if (shape instanceof RectangleOCP r) {
            return r.length() * r.width();
        }
        throw new IllegalArgumentException("Unknown shape");
    }
}

// Follows OCP
interface ShapeOCP {
    double area();
}

record CircleOCP(double radius) implements ShapeOCP {
    @Override
    public double area() { return Math.PI * radius * radius; }
}

record RectangleOCP(double length, double width) implements ShapeOCP {
    @Override
    public double area() { return length * width; }
}

class AreaCalculator {
    public double calculateArea(ShapeOCP shape) {
        return shape.area();
    }
}

// New shape without modifying existing code
record TriangleOCP(double base, double height) implements ShapeOCP {
    @Override
    public double area() { return 0.5 * base * height; }
}

// =============================================
// 3. LSP - Liskov Substitution Principle
// Subtypes must be substitutable for their base types
// =============================================

// Violates LSP
class Bird {
    public void fly() {
        System.out.println("Bird flies");
    }
}

class Penguin extends Bird {
    @Override
    public void fly() {
        throw new UnsupportedOperationException("Penguins cannot fly!");
    }
}

// Follows LSP
interface Flyable {
    void fly();
}

class Sparrow implements Flyable {
    @Override
    public void fly() {
        System.out.println("Sparrow flies");
    }
}

class PenguinLSP {
    public void swim() {
        System.out.println("Penguin swims");
    }
}

// =============================================
// 4. ISP - Interface Segregation Principle
// Clients should not be forced to depend on interfaces they don't use
// =============================================

// Violates ISP - fat interface
interface Worker {
    void work();
    void eat();
    void sleep();
}

class RobotWorker implements Worker {
    @Override
    public void work() { System.out.println("Robot works"); }

    @Override
    public void eat() { throw new UnsupportedOperationException("Robot does not eat"); }

    @Override
    public void sleep() { throw new UnsupportedOperationException("Robot does not sleep"); }
}

// Follows ISP - segregated interfaces
interface Workable {
    void work();
}

interface Eatable {
    void eat();
}

interface Sleepable {
    void sleep();
}

class HumanWorker implements Workable, Eatable, Sleepable {
    @Override
    public void work() { System.out.println("Human works"); }

    @Override
    public void eat() { System.out.println("Human eats"); }

    @Override
    public void sleep() { System.out.println("Human sleeps"); }
}

class RobotWorkerISP implements Workable {
    @Override
    public void work() { System.out.println("Robot works"); }
}

// =============================================
// 5. DIP - Dependency Inversion Principle
// Depend on abstractions, not concretions
// =============================================

// Violates DIP - depends on concrete class
class LightBulb {
    public void turnOn() { System.out.println("Light bulb on"); }
    public void turnOff() { System.out.println("Light bulb off"); }
}

class BadSwitch {
    private final LightBulb bulb;

    public BadSwitch() {
        this.bulb = new LightBulb(); // Tightly coupled
    }

    public void operate() {
        bulb.turnOn();
    }
}

// Follows DIP
interface Switchable {
    void turnOn();
    void turnOff();
}

class LightBulbDIP implements Switchable {
    @Override
    public void turnOn() { System.out.println("Light bulb on"); }

    @Override
    public void turnOff() { System.out.println("Light bulb off"); }
}

class Fan implements Switchable {
    @Override
    public void turnOn() { System.out.println("Fan on"); }

    @Override
    public void turnOff() { System.out.println("Fan off"); }
}

class Switch {
    private final Switchable device;

    public Switch(Switchable device) {
        this.device = device;
    }

    public void operate() {
        device.turnOn();
    }

    public void turnOff() {
        device.turnOff();
    }
}

// =============================================
// Main Demo
// =============================================
class SOLIDPrinciples {
    public static void main(String[] args) {
        System.out.println("========== SOLID PRINCIPLES ==========\n");

        // SRP
        System.out.println("1. SRP - Single Responsibility Principle");
        Employee emp = new Employee("Alice", 75_000);
        String report = EmployeeReportGenerator.generate(emp);
        System.out.println(report);
        System.out.println("-> Employee class only handles data, report generation is separate");
        System.out.println();

        // OCP
        System.out.println("2. OCP - Open/Closed Principle");
        List<ShapeOCP> shapes = List.of(
                new CircleOCP(5),
                new RectangleOCP(4, 6),
                new TriangleOCP(3, 8)
        );
        AreaCalculator calc = new AreaCalculator();
        for (ShapeOCP shape : shapes) {
            System.out.printf("Area: %.2f%n", calc.calculateArea(shape));
        }
        System.out.println("-> New shapes can be added without modifying AreaCalculator");
        System.out.println();

        // LSP
        System.out.println("3. LSP - Liskov Substitution Principle");
        List<Flyable> flyingBirds = List.of(new Sparrow());
        for (Flyable bird : flyingBirds) {
            bird.fly();
        }
        PenguinLSP penguin = new PenguinLSP();
        penguin.swim();
        System.out.println("-> Penguins don't extend Bird; use proper abstraction hierarchy");
        System.out.println();

        // ISP
        System.out.println("4. ISP - Interface Segregation Principle");
        HumanWorker human = new HumanWorker();
        human.work();
        human.eat();
        human.sleep();

        RobotWorkerISP robot = new RobotWorkerISP();
        robot.work();
        System.out.println("-> Robot only implements Workable, not Eatable/Sleepable");
        System.out.println();

        // DIP
        System.out.println("5. DIP - Dependency Inversion Principle");
        Switchable bulb = new LightBulbDIP();
        Switchable fan = new Fan();

        Switch lightSwitch = new Switch(bulb);
        Switch fanSwitch = new Switch(fan);

        lightSwitch.operate();
        fanSwitch.operate();
        lightSwitch.turnOff();
        fanSwitch.turnOff();
        System.out.println("-> Switch depends on Switchable abstraction, not concrete classes");
    }
}
