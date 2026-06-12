package phase04.exceptionhandling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class BestPractices {
    private static final Logger LOGGER = Logger.getLogger(BestPractices.class.getName());

    public static void tryWithResources(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            System.out.println("First line: " + reader.readLine());
        } catch (IOException e) {
            LOGGER.severe("Failed to read file: " + e.getMessage());
        }
    }

    public static String readFirstLine(String path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            return reader.readLine();
        }
    }

    static class DatabaseException extends Exception {
        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
            LOGGER.log(Level.SEVERE, message, cause);
        }
    }

    public static void chainingDemo() throws DatabaseException {
        try {
            readFirstLine("nonexistent.txt");
        } catch (IOException e) {
            throw new DatabaseException("Database operation failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Exception> void preciseRethrow(String flag) throws T {
        try {
            if ("io".equals(flag)) {
                throw (T) new IOException("I/O error");
            } else {
                throw (T) new RuntimeException("Runtime error");
            }
        } catch (Exception e) {
            throw (T) e;
        }
    }

    public static void main(String[] args) {
        tryWithResources("test.txt");

        try {
            chainingDemo();
        } catch (DatabaseException e) {
            System.out.println("Chained exception: " + e.getMessage());
            System.out.println("Cause: " + e.getCause().getClass().getSimpleName());
        }

        try {
            BestPractices.<IOException>preciseRethrow("io");
        } catch (IOException e) {
            System.out.println("Precise rethrow: " + e.getMessage());
        }

        try {
            BestPractices.<RuntimeException>preciseRethrow("runtime");
        } catch (RuntimeException e) {
            System.out.println("Precise rethrow: " + e.getMessage());
        }

        System.out.println("\nBest practices demonstrated:");
        System.out.println("- Try-with-resources (auto-close)");
        System.out.println("- Exception chaining");
        System.out.println("- Precise rethrow");
        System.out.println("- Logging (never swallow)");
    }
}
