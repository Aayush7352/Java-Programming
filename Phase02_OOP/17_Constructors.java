package phase02.oop;

class Constructors {
    private int id;
    private String name;
    private double salary;

    // Default constructor
    public Constructors() {
        this.id = 0;
        this.name = "Unknown";
        this.salary = 0.0;
        System.out.println("Default constructor called");
    }

    // Parameterized constructor
    public Constructors(int id, String name, double salary) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        System.out.println("Parameterized constructor called");
    }

    // Constructor with this() chaining
    public Constructors(int id, String name) {
        this(id, name, 30_000.0);
        System.out.println("Two-arg constructor -> chained to three-arg");
    }

    // Copy constructor
    public Constructors(Constructors other) {
        this(other.id, other.name, other.salary);
        System.out.println("Copy constructor called");
    }

    public Constructors(String name) {
        this(0, name, 0.0);
    }

    public void display() {
        System.out.printf("Employee[id=%d, name='%s', salary=%.2f]%n", id, name, salary);
    }

    public static void main(String[] args) {
        System.out.println("=== Default Constructor ===");
        Constructors e1 = new Constructors();
        e1.display();

        System.out.println("\n=== Parameterized Constructor ===");
        Constructors e2 = new Constructors(101, "Alice", 75_000.00);
        e2.display();

        System.out.println("\n=== this() Chaining (2 args -> 3 args) ===");
        Constructors e3 = new Constructors(102, "Bob");
        e3.display();

        System.out.println("\n=== Copy Constructor ===");
        Constructors e4 = new Constructors(e2);
        e4.display();

        System.out.println("\n=== Overloaded Constructor ===");
        Constructors e5 = new Constructors("Charlie");
        e5.display();
    }
}
