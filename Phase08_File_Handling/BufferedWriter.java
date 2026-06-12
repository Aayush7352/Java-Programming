package phase08.filehandling;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class BufferedWriter {
    public static void main(String[] args) throws IOException {
        var path = Files.createTempFile("buffered_writer_demo", ".txt");
        System.out.println("Writing to: " + path);

        // BufferedWriter with FileWriter (classic)
        System.out.println("\n=== FileWriter + BufferedWriter ===");
        try (var bw = new java.io.BufferedWriter(new FileWriter(path.toFile()))) {
            bw.write("Hello from BufferedWriter");
            bw.newLine();
            bw.write("Second line");
            bw.newLine();
        }
        System.out.println(Files.readString(path));

        // Files.newBufferedWriter (Java 8+)
        System.out.println("=== Files.newBufferedWriter ===");
        try (var bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            bw.write("Line A");
            bw.newLine();
            bw.write("Line B");
            bw.newLine();
            bw.append("Line C");
            bw.newLine();
        }
        System.out.println(Files.readString(path));

        // append mode
        System.out.println("=== Append mode ===");
        try (var bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.APPEND)) {
            bw.write("Appended line");
            bw.newLine();
        }
        System.out.println(Files.readString(path));

        // write with char array
        System.out.println("=== Write char array ===");
        try (var bw = new java.io.BufferedWriter(new FileWriter(path.toFile()))) {
            bw.write("ABCDEFGHIJ".toCharArray(), 0, 5);
            bw.newLine();
        }
        System.out.println(Files.readString(path));

        // flush
        try (var bw = new java.io.BufferedWriter(new FileWriter(path.toFile()))) {
            bw.write("Before flush");
            bw.flush();
            System.out.println("Flushed successfully");
        }

        Files.deleteIfExists(path);
        System.out.println("\nDeleted temp file");
    }
}
