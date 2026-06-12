package phase08.filehandling;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

record Address(String street, String city) implements Serializable {
    private static final long serialVersionUID = 1L;
}

class Employee implements Serializable {
    private static final long serialVersionUID = 42L;

    String name;
    int age;
    transient String password;     // won't be serialized
    Address address;

    Employee(String name, int age, String password, Address address) {
        this.name = name;
        this.age = age;
        this.password = password;
        this.address = address;
    }

    @Override
    public String toString() {
        return "Employee{name='%s', age=%d, password='%s', address=%s}"
                .formatted(name, age, password, address);
    }
}

public class Serialization {
    public static void main(String[] args) throws IOException {
        var filePath = System.getProperty("java.io.tmpdir") + "/employee.ser";
        System.out.println("Serializing to: " + filePath);

        var address = new Address("123 Main St", "Springfield");
        var emp = new Employee("Alice", 30, "secret123", address);

        System.out.println("Before serialization: " + emp);

        try (var fos = new FileOutputStream(filePath);
             var oos = new ObjectOutputStream(fos)) {
            oos.writeObject(emp);
            System.out.println("Serialized successfully");
        }

        // Serialize multiple objects
        var multiplePath = System.getProperty("java.io.tmpdir") + "/employees.ser";
        try (var fos = new FileOutputStream(multiplePath);
             var oos = new ObjectOutputStream(fos)) {
            oos.writeObject(new Employee("Bob", 25, "pass1", new Address("456 Oak", "Shelbyville")));
            oos.writeObject(new Employee("Carol", 35, "pass2", new Address("789 Pine", "Capital City")));
            System.out.println("Serialized multiple objects");
        }

        System.out.println("\nRun Deserialization to read back the objects.");
    }
}
