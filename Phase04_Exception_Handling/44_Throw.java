package phase04.exceptionhandling;

import java.io.IOException;

class Throw {
    public static void validateAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative: " + age);
        }
        if (age < 18) {
            throw new RuntimeException("Must be 18 or older, got " + age);
        }
        System.out.println("Age " + age + " is valid.");
    }

    public static void throwChecked() throws IOException {
        throw new IOException("Simulated checked exception");
    }

    public static void rethrowDemo(String action) throws Exception {
        try {
            if ("io".equals(action)) {
                throw new IOException("I/O failure");
            } else if ("runtime".equals(action)) {
                throw new RuntimeException("Runtime failure");
            }
        } catch (Exception e) {
            System.out.println("Rethrowing: " + e.getClass().getSimpleName());
            throw e;
        }
    }

    public static void main(String[] args) {
        try {
            validateAge(25);
            validateAge(-5);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        try {
            validateAge(15);
        } catch (RuntimeException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        try {
            throwChecked();
        } catch (IOException e) {
            System.out.println("Checked exception: " + e.getMessage());
        }

        try {
            rethrowDemo("io");
        } catch (Exception e) {
            System.out.println("Main caught: " + e.getClass().getSimpleName());
        }
    }
}
