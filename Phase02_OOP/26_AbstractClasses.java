package phase02.oop;

abstract class Employee {
    protected int id;
    protected String name;

    // Constructor in abstract class
    public Employee(int id, String name) {
        this.id = id;
        this.name = name;
        System.out.println("Employee abstract constructor called for: " + name);
    }

    // Concrete methods
    public void displayBasicInfo() {
        System.out.println("ID: " + id + ", Name: " + name);
    }

    public int getId() { return id; }

    // Abstract methods
    public abstract double calculateSalary();
    public abstract String getRole();
    public abstract void work();

    // Concrete method with default implementation
    public String getDepartment() {
        return "General";
    }
}

class FullTimeEmployee extends Employee {
    private double monthlySalary;

    public FullTimeEmployee(int id, String name, double monthlySalary) {
        super(id, name);
        this.monthlySalary = monthlySalary;
    }

    @Override
    public double calculateSalary() {
        return monthlySalary;
    }

    @Override
    public String getRole() {
        return "Full-Time Employee";
    }

    @Override
    public void work() {
        System.out.println(name + " works 40 hours per week");
    }

    @Override
    public String getDepartment() {
        return "Engineering";
    }
}

class PartTimeEmployee extends Employee {
    private double hourlyRate;
    private int hoursPerWeek;

    public PartTimeEmployee(int id, String name, double hourlyRate, int hoursPerWeek) {
        super(id, name);
        this.hourlyRate = hourlyRate;
        this.hoursPerWeek = hoursPerWeek;
    }

    @Override
    public double calculateSalary() {
        return hourlyRate * hoursPerWeek * 4; // monthly estimate
    }

    @Override
    public String getRole() {
        return "Part-Time Employee";
    }

    @Override
    public void work() {
        System.out.println(name + " works " + hoursPerWeek + " hours per week");
    }
}

class AbstractClasses {
    public static void main(String[] args) {
        System.out.println("=== Abstract Class with Constructor ===");
        // Employee e = new Employee(1, "test"); // Cannot instantiate

        FullTimeEmployee fullTime = new FullTimeEmployee(101, "Alice", 8_500.00);
        System.out.println("\n--- Full-Time Employee ---");
        fullTime.displayBasicInfo();
        System.out.println("Role: " + fullTime.getRole());
        System.out.println("Department: " + fullTime.getDepartment());
        System.out.printf("Monthly Salary: $%,.2f%n", fullTime.calculateSalary());
        fullTime.work();

        System.out.println();
        PartTimeEmployee partTime = new PartTimeEmployee(102, "Bob", 35.00, 25);
        System.out.println("\n--- Part-Time Employee ---");
        partTime.displayBasicInfo();
        System.out.println("Role: " + partTime.getRole());
        System.out.println("Department: " + partTime.getDepartment());
        System.out.printf("Monthly Salary: $%,.2f%n", partTime.calculateSalary());
        partTime.work();

        // Polymorphic behavior
        System.out.println("\n=== Polymorphism with Abstract Class ===");
        Employee[] employees = {
            new FullTimeEmployee(201, "Charlie", 9_200.00),
            new PartTimeEmployee(202, "Diana", 40.00, 20)
        };
        for (Employee emp : employees) {
            System.out.println();
            emp.displayBasicInfo();
            System.out.println("Salary: $" + String.format("%,.2f", emp.calculateSalary()));
            emp.work();
        }
    }
}
