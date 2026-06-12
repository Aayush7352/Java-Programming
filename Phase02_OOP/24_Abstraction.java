package phase02.oop;

abstract class Vehicle {
    protected String brand;
    protected String model;

    public Vehicle(String brand, String model) {
        this.brand = brand;
        this.model = model;
    }

    // Abstract methods
    public abstract void start();
    public abstract void stop();
    public abstract String getFuelType();

    // Concrete method
    public void displayInfo() {
        System.out.println("Brand: " + brand + ", Model: " + model);
    }

    public static void showStatic() {
        System.out.println("Static method in abstract class");
    }
}

class Car extends Vehicle {
    public Car(String brand, String model) {
        super(brand, model);
    }

    @Override
    public void start() {
        System.out.println(brand + " " + model + ": Turn key and press brake to start");
    }

    @Override
    public void stop() {
        System.out.println(brand + " " + model + ": Press brake pedal to stop");
    }

    @Override
    public String getFuelType() {
        return "Petrol/Diesel/Electric";
    }
}

class Bicycle extends Vehicle {
    public Bicycle(String brand, String model) {
        super(brand, model);
    }

    @Override
    public void start() {
        System.out.println(brand + " " + model + ": Start pedaling");
    }

    @Override
    public void stop() {
        System.out.println(brand + " " + model + ": Apply hand brakes");
    }

    @Override
    public String getFuelType() {
        return "Human power (no fuel)";
    }
}

// Interface comparison
interface VehicleInterface {
    void start();
    void stop();
    default void honk() {
        System.out.println("Beep beep!");
    }
}

class Abstraction {
    public static void main(String[] args) {
        System.out.println("=== Abstract Class Demo ===");
        // Vehicle v = new Vehicle("x", "y"); // Cannot instantiate abstract class

        Vehicle car = new Car("Toyota", "Camry");
        Vehicle bike = new Bicycle("Giant", "Escape 3");

        car.displayInfo();
        car.start();
        car.stop();
        System.out.println("Fuel: " + car.getFuelType());

        System.out.println();
        bike.displayInfo();
        bike.start();
        bike.stop();
        System.out.println("Fuel: " + bike.getFuelType());

        Vehicle.showStatic();

        System.out.println("\n=== Abstract Class vs Interface ===");
        System.out.println("""
                Abstract Class                          Interface
                ---------------                         ---------
                Can have constructors                   Cannot have constructors
                Can have instance variables              Can only have static final variables
                Can have any access modifiers            Methods are public by default
                Single inheritance (extends)             Multiple inheritance (implements)
                Can have concrete and abstract methods   Can have abstract, default, static methods
                Supports final methods                  Methods cannot be final
                """);
    }
}
