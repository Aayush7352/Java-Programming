package phase04.exceptionhandling;

import java.io.FileNotFoundException;
import java.io.IOException;

class Throws {
    public static void readFile(String path) throws FileNotFoundException {
        if (path == null || path.isBlank()) {
            throw new FileNotFoundException("Path is empty");
        }
        System.out.println("Reading file: " + path);
    }

    public static void processFile(String path) throws IOException {
        readFile(path);
        System.out.println("Processing file...");
    }

    public static void main(String[] args) {
        try {
            processFile("");
        } catch (IOException e) {
            System.out.println("IOException propagated: " + e.getClass().getSimpleName()
                    + " - " + e.getMessage());
        }

        try {
            processFile("data.txt");
        } catch (IOException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }
}
