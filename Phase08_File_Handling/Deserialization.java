package phase08.filehandling;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class Deserialization {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        var singlePath = System.getProperty("java.io.tmpdir") + "/employee.ser";
        var multiplePath = System.getProperty("java.io.tmpdir") + "/employees.ser";

        // Deserialize single object
        System.out.println("=== Single deserialization ===");
        try (var fis = new FileInputStream(singlePath);
             var ois = new ObjectInputStream(fis)) {
            var emp = ois.readObject();
            System.out.println("Deserialized: " + emp);
        }

        // Verify transient field is lost
        System.out.println("(password should be null — it was transient)");

        // Deserialize multiple objects
        System.out.println("\n=== Multiple deserialization ===");
        try (var fis = new FileInputStream(multiplePath);
             var ois = new ObjectInputStream(fis)) {
            for (int i = 0; i < 2; i++) {
                var obj = ois.readObject();
                System.out.println("Object " + (i + 1) + ": " + obj);
            }
        }

        // serialVersionUID compatibility check demonstration
        System.out.println("\n=== Version compatibility ===");
        System.out.println("If the serialVersionUID in the class changes,");
        System.out.println("deserialization will throw InvalidClassException.");
        System.out.println("Always declare explicit serialVersionUID to maintain");
        System.out.println("compatibility across versions.");

        // Demonstrate readObject with type checking
        System.out.println("\n=== Type checking ===");
        try (var fis = new FileInputStream(singlePath);
             var ois = new ObjectInputStream(fis)) {
            var obj = ois.readObject();
            if (obj instanceof Employee emp) {
                System.out.println("Employee name: " + emp.name);
                System.out.println("Employee age: " + emp.age);
                System.out.println("Employee password (transient, should be null): " + emp.password);
                System.out.println("Employee address: " + emp.address);
            }
        }
    }
}
