package phase08.filehandling;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class BufferedReader {
    public static void main(String[] args) throws IOException {
        var path = Files.createTempFile("buffered_reader_demo", ".txt");
        Files.writeString(path, "Line 1\nLine 2\nLine 3\nHello, BufferedReader!\n");

        // BufferedReader with FileReader (classic)
        System.out.println("=== FileReader + BufferedReader ===");
        try (var br = new java.io.BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("  " + line);
            }
        }

        // Files.newBufferedReader (Java 8+)
        System.out.println("\n=== Files.newBufferedReader ===");
        try (var br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("  " + line);
            }
        }

        // lines() stream (Java 8+)
        System.out.println("\n=== lines() stream ===");
        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            lines.forEach(l -> System.out.println("  " + l));
        }

        // read all lines into List
        System.out.println("\n=== Files.readAllLines ===");
        var allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        allLines.forEach(l -> System.out.println("  " + l));

        // mark / reset
        System.out.println("\n=== mark/reset ===");
        try (var br = new java.io.BufferedReader(new FileReader(path.toFile()))) {
            System.out.println("  First: " + br.readLine());
            br.mark(100);
            System.out.println("  After mark: " + br.readLine());
            br.reset();
            System.out.println("  After reset: " + br.readLine());
        }

        Files.deleteIfExists(path);
    }
}
