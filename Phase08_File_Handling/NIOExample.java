package phase08.filehandling;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Stream;

public class NIOExample {
    public static void main(String[] args) throws IOException {
        var tmpDir = Files.createTempDirectory("nio_demo");
        var filePath = tmpDir.resolve("demo.txt");

        // Path (Java 7+)
        System.out.println("Path: " + filePath);
        System.out.println("getFileName: " + filePath.getFileName());
        System.out.println("getParent: " + filePath.getParent());
        System.out.println("getRoot: " + filePath.getRoot());

        // Files.write
        Files.writeString(filePath, "Hello NIO.2!\nLine 2\nLine 3\n");
        System.out.println("\nWrote to file");

        // Files.readAllLines
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        System.out.println("readAllLines: " + lines);

        // Files.copy
        var copyPath = tmpDir.resolve("copy.txt");
        Files.copy(filePath, copyPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Copied to: " + copyPath);
        System.out.println("Copy contents: " + Files.readString(copyPath));

        // Files.move
        var movedPath = tmpDir.resolve("moved.txt");
        Files.move(copyPath, movedPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Moved to: " + movedPath);

        // Files.delete
        Files.delete(movedPath);
        System.out.println("Deleted moved file");

        // Files.walk (recursive directory traversal)
        var subDir = tmpDir.resolve("sub");
        Files.createDirectories(subDir.resolve("nested"));
        Files.writeString(subDir.resolve("a.txt"), "aaa");
        Files.writeString(subDir.resolve("b.txt"), "bbb");
        Files.writeString(subDir.resolve("nested/c.txt"), "ccc");

        System.out.println("\n=== Files.walk ===");
        try (Stream<Path> walk = Files.walk(tmpDir)) {
            walk.forEach(p -> System.out.println("  " + p));
        }

        // Files.find
        System.out.println("\n=== Files.find (.txt files) ===");
        try (Stream<Path> found = Files.find(tmpDir, 3,
                (p, attrs) -> p.toString().endsWith(".txt"))) {
            found.forEach(p -> System.out.println("  " + p));
        }

        // FileChannel + ByteBuffer
        System.out.println("\n=== FileChannel + ByteBuffer ===");
        var channelFile = tmpDir.resolve("channel.dat");
        try (var channel = FileChannel.open(channelFile,
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            var buffer = ByteBuffer.allocate(48);
            buffer.put("Channel data!".getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            int bytesWritten = channel.write(buffer);
            System.out.println("Wrote " + bytesWritten + " bytes via channel");

            channel.position(0);
            buffer.clear();
            int bytesRead = channel.read(buffer);
            buffer.flip();
            var content = new byte[bytesRead];
            buffer.get(content);
            System.out.println("Read via channel: " + new String(content, StandardCharsets.UTF_8));
        }

        // RandomAccessFile + FileChannel
        System.out.println("\n=== RandomAccessFile + FileChannel ===");
        try (var raf = new RandomAccessFile(channelFile.toFile(), "rw");
             var channel = raf.getChannel()) {
            raf.seek(0);
            var buf = ByteBuffer.allocate((int) channel.size());
            channel.read(buf);
            buf.flip();
            var result = new byte[buf.remaining()];
            buf.get(result);
            System.out.println("RandomAccessFile read: " + new String(result, StandardCharsets.UTF_8));
        }

        // Files.isDirectory, isRegularFile, size
        System.out.println("\nisDirectory: " + Files.isDirectory(tmpDir));
        System.out.println("isRegularFile: " + Files.isRegularFile(filePath));
        System.out.println("size: " + Files.size(filePath));

        // BasicFileAttributes
        var attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
        System.out.println("creationTime: " + attrs.creationTime());
        System.out.println("lastModifiedTime: " + attrs.lastModifiedTime());
        System.out.println("size: " + attrs.size());

        // clean up
        try (Stream<Path> walk = Files.walk(tmpDir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException e) {} });
        }
        System.out.println("\nCleaned up all temp files");
    }
}
