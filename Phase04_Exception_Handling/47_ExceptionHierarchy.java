package phase04.exceptionhandling;

import java.io.FileNotFoundException;
import java.io.IOException;

class ExceptionHierarchy {
    public static void main(String[] args) {
        System.out.println("Throwable");
        System.out.println("  +-- Error (unrecoverable: OutOfMemoryError, etc.)");
        System.out.println("  +-- Exception");
        System.out.println("        +-- RuntimeException (unchecked)");
        System.out.println("        +-- IOException (checked)");
        System.out.println("        +-- CustomException\n");

        try {
            methodA(0);
        } catch (IOException e) {
            System.out.println("Caught IOException: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Caught RuntimeException: " + e.getMessage());
        }

        try {
            methodA(1);
        } catch (FileNotFoundException e) {
            System.out.println("Caught FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Caught IOException: " + e.getMessage());
        }

        try {
            methodA(2);
        } catch (IOException e) {
            System.out.println("Caught IOException (base type): " + e.getClass().getSimpleName()
                    + " - " + e.getMessage());
        }
    }

    static void methodA(int choice) throws IOException, FileNotFoundException {
        switch (choice) {
            case 0 -> throw new RuntimeException("Runtime: division by zero");
            case 1 -> throw new FileNotFoundException("File not found");
            case 2 -> throw new IOException("General I/O error");
        }
    }
}
