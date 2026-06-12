package phase02.oop;

class Encapsulation {
    private String name;
    private int age;
    private double salary;
    private String email;

    // Constructor
    public Encapsulation(String name, int age, double salary, String email) {
        setName(name);
        setAge(age);
        setSalary(salary);
        setEmail(email);
    }

    // Getters
    public String getName() { return name; }

    public int getAge() { return age; }

    public double getSalary() { return salary; }

    public String getEmail() { return email; }

    // Setters with validation
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.name = name;
    }

    public void setAge(int age) {
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Age must be between 0 and 150");
        }
        this.age = age;
    }

    public void setSalary(double salary) {
        if (salary < 0) {
            throw new IllegalArgumentException("Salary cannot be negative");
        }
        this.salary = salary;
    }

    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        this.email = email;
    }

    // Method to give raise (controlled access)
    public void giveRaise(double percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
        this.salary += this.salary * (percentage / 100.0);
    }

    @Override
    public String toString() {
        return """
                Employee Record:
                  Name   : %s
                  Age    : %d
                  Salary : $%,.2f
                  Email  : %s
                """.formatted(name, age, salary, email);
    }

    public static void main(String[] args) {
        System.out.println("=== Encapsulation Demo ===");

        try {
            Encapsulation emp = new Encapsulation("Alice Johnson", 30, 75_000.00, "alice@company.com");
            System.out.println(emp);

            System.out.println("=== Access Through Getters ===");
            System.out.println("Name: " + emp.getName());
            System.out.println("Age: " + emp.getAge());
            System.out.println("Salary: $" + String.format("%,.2f", emp.getSalary()));
            System.out.println("Email: " + emp.getEmail());

            System.out.println("\n=== Give Raise ===");
            emp.giveRaise(10);
            System.out.println("After 10%% raise: $" + String.format("%,.2f", emp.getSalary()));

            // Validation demo
            System.out.println("\n=== Validation in Setter ===");
            try {
                emp.setAge(-5);
            } catch (IllegalArgumentException e) {
                System.out.println("Validation caught: " + e.getMessage());
            }

            try {
                @SuppressWarnings("unused")
                Encapsulation invalid = new Encapsulation("", 25, 50_000, "bad-email");
            } catch (IllegalArgumentException e) {
                System.out.println("Creation validation caught: " + e.getMessage());
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
