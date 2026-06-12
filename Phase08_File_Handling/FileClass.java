package phase08.filehandling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class FileClass {
    public static void main(String[] args) throws IOException {
        var basePath = System.getProperty("java.io.tmpdir") + "/phase08_demo";
        var dir = new File(basePath);
        dir.mkdirs();

        var file = new File(dir, "test.txt");
        System.out.println("File object: " + file);

        // createNewFile
        boolean created = file.createNewFile();
        System.out.println("created: " + created);

        // exists
        System.out.println("exists: " + file.exists());

        // isFile / isDirectory
        System.out.println("isFile: " + file.isFile());
        System.out.println("isDirectory: " + dir.isDirectory());

        // mkdir / mkdirs
        var nested = new File(dir, "a/b/c");
        System.out.println("mkdirs for nested: " + nested.mkdirs());

        // list
        System.out.print("list: ");
        for (var name : dir.list()) System.out.print(name + " ");
        System.out.println();

        // listFiles
        System.out.print("listFiles: ");
        for (var f : dir.listFiles()) System.out.print(f.getName() + " ");
        System.out.println();

        // length
        System.out.println("file length: " + file.length());

        // renameTo
        var renamed = new File(dir, "renamed.txt");
        System.out.println("renameTo: " + file.renameTo(renamed));

        // delete
        System.out.println("delete renamed: " + renamed.delete());

        // File vs Path (Java 7+)
        var path = file.toPath();
        System.out.println("toPath: " + path);

        var path2 = Path.of(basePath, "from_path.txt");
        var fileFromPath = path2.toFile();
        System.out.println("File from Path: " + fileFromPath);

        // clean up
        deleteRecursively(dir);
    }

    private static void deleteRecursively(File f) {
        if (f.isDirectory()) {
            for (var child : f.listFiles()) deleteRecursively(child);
        }
        f.delete();
    }
}
