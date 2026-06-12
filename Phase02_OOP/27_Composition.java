package phase02.oop;

class Processor {
    private String model;
    private int cores;
    private double speed; // GHz

    public Processor(String model, int cores, double speed) {
        this.model = model;
        this.cores = cores;
        this.speed = speed;
    }

    public void process() {
        System.out.println(model + " (" + cores + " cores @ " + speed + "GHz) processing data");
    }

    @Override
    public String toString() {
        return model + " " + cores + "c/" + speed + "GHz";
    }
}

class Computer {
    private String brand;
    private String model;
    private Processor processor; // Composition: Computer HAS-A Processor

    public Computer(String brand, String model, String processorModel, int cores, double speed) {
        this.brand = brand;
        this.model = model;
        this.processor = new Processor(processorModel, cores, speed);
    }

    public void start() {
        System.out.println(brand + " " + model + " starting...");
        processor.process();
    }

    public void shutdown() {
        System.out.println(brand + " " + model + " shutting down");
    }

    public Processor getProcessor() {
        return processor;
    }

    @Override
    public String toString() {
        return brand + " " + model + " [" + processor + "]";
    }
}

class Engine {
    private String type;
    private int horsepower;

    public Engine(String type, int horsepower) {
        this.type = type;
        this.horsepower = horsepower;
    }

    public void start() {
        System.out.println(type + " " + horsepower + "hp engine started");
    }

    @Override
    public String toString() {
        return type + " " + horsepower + "HP";
    }
}

class Car {
    private String make;
    private String model;
    private Engine engine;

    public Car(String make, String model, String engineType, int horsepower) {
        this.make = make;
        this.model = model;
        this.engine = new Engine(engineType, horsepower);
    }

    public void drive() {
        System.out.println(make + " " + model + " is being driven");
        engine.start();
    }

    @Override
    public String toString() {
        return make + " " + model + " [" + engine + "]";
    }
}

class Composition {
    public static void main(String[] args) {
        System.out.println("=== Composition: Computer HAS-A Processor ===");
        Computer computer = new Computer("Dell", "XPS 15", "Intel Core i7-13700H", 14, 2.4);
        System.out.println("Computer: " + computer);
        computer.start();
        computer.shutdown();

        System.out.println("\nAccessing Processor via Computer:");
        Processor cpu = computer.getProcessor();
        cpu.process();

        System.out.println("\n=== Composition: Car HAS-A Engine ===");
        Car car = new Car("Honda", "Civic", "V4", 180);
        System.out.println("Car: " + car);
        car.drive();

        // If Computer is destroyed, Processor is also destroyed
        System.out.println("\n=== Lifecycle Dependency ===");
        System.out.println("When Computer is garbage collected, its Processor is also collected.");
        System.out.println("Processor cannot exist independently of Computer.");
    }
}
