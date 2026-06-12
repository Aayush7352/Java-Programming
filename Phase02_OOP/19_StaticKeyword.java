package phase02.oop;

import static java.lang.Math.*;

class StaticKeyword {
    // Static field
    private static int instanceCount = 0;
    private static final String COMPANY = "Acme Corp";

    private final int id;

    public StaticKeyword() {
        this.id = ++instanceCount;
    }

    // Static method
    public static int getInstanceCount() {
        return instanceCount;
    }

    public static String getCompany() {
        return COMPANY;
    }

    // Static block
    static {
        System.out.println("Static block executed - class loaded");
        System.out.println("Company: " + COMPANY);
    }

    public void display() {
        System.out.println("Employee #" + id + " at " + COMPANY);
    }

    // Static nested class
    public static class Address {
        private String street;
        private String city;

        public Address(String street, String city) {
            this.street = street;
            this.city = city;
        }

        public void print() {
            System.out.println("Address: " + street + ", " + city);
        }

        public static void printCompany() {
            System.out.println("All employees work at: " + COMPANY);
        }
    }

    public static void main(String[] args) {
        System.out.println("\n=== Static Fields ===");
        StaticKeyword e1 = new StaticKeyword();
        StaticKeyword e2 = new StaticKeyword();
        StaticKeyword e3 = new StaticKeyword();

        System.out.println("e1.id: " + e1.id);
        System.out.println("e2.id: " + e2.id);
        System.out.println("e3.id: " + e3.id);

        // Accessing static field via class name
        System.out.println("Total instances (via class): " + StaticKeyword.instanceCount);
        System.out.println("Total instances (via method): " + StaticKeyword.getInstanceCount());

        // Static method
        System.out.println("\n=== Static Methods ===");
        System.out.println("Company: " + StaticKeyword.getCompany());

        // Static import demo
        System.out.println("\n=== Static Import ===");
        System.out.println("PI = " + PI);
        System.out.println("sqrt(25) = " + sqrt(25));
        System.out.println("max(10, 20) = " + max(10, 20));

        // Static nested class
        System.out.println("\n=== Static Nested Class ===");
        StaticKeyword.Address addr = new StaticKeyword.Address("123 Main St", "Metropolis");
        addr.print();
        StaticKeyword.Address.printCompany();

        // instanceof usage
        System.out.println("\n=== instanceof with static context ===");
        Object obj = addr;
        if (obj instanceof StaticKeyword.Address a) {
            a.print();
        }
    }
}
