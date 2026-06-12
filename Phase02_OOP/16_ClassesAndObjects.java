package phase02.oop;

class ClassesAndObjects {
    private String make;
    private String model;
    private int year;

    public ClassesAndObjects(String make, String model, int year) {
        this.make = make;
        this.model = model;
        this.year = year;
    }

    public void displayInfo() {
        System.out.println("Car: " + year + " " + make + " " + model);
    }

    public void startEngine() {
        System.out.println(make + " " + model + " engine started.");
    }

    public static void main(String[] args) {
        ClassesAndObjects car1 = new ClassesAndObjects("Toyota", "Camry", 2024);
        car1.displayInfo();
        car1.startEngine();

        ClassesAndObjects car2 = new ClassesAndObjects("Tesla", "Model 3", 2025);
        car2.displayInfo();
        car2.startEngine();

        System.out.println("\n=== Access Modifiers Demo ===");
        AccessDemo demo = new AccessDemo();
        System.out.println("publicField: " + demo.publicField);
        System.out.println("protectedField: " + demo.protectedField);
        System.out.println("defaultField: " + demo.defaultField);
        demo.publicMethod();
    }
}

class AccessDemo {
    public String publicField = "accessible everywhere";
    protected String protectedField = "accessible in package + subclasses";
    String defaultField = "accessible in package";
    private String privateField = "accessible only in this class";

    public void publicMethod() {
        System.out.println("privateField (same class): " + privateField);
    }
}
