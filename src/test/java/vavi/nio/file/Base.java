/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static com.rainerhahnekamp.sneakythrow.Sneaky.sneaked;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Base.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/xx umjammer initial version <br>
 */
public interface Base {

    /** */
    static void testAll(FileSystem fileSystem) throws Exception {

        Path src = Paths.get("src/test/resources" , "Hello.java");
        Path dir = fileSystem.getPath("/").resolve("VAVIFUSE_FS_TEST");
        final Path dir0 = dir;
System.out.println("$ [list]: " + dir.getParent());
Files.list(dir.getParent()).forEach(System.out::println);
        long count = Files.list(dir.getParent()).count();

        if (Files.exists(dir)) {
            List<Path> files = new ArrayList<>();
            List<Path> dirs = new ArrayList<>();
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    files.add(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    dirs.add(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            files.forEach(sneaked(p -> {
System.out.println("$ [*delete file]: " + p);
                Files.delete(p);
Thread.sleep(300);
            }));
            dirs.stream().filter(p -> !p.equals(dir0)).forEach(sneaked(p -> {
System.out.println("$ [*delete dir]: " + p);
                Files.delete(p);
Thread.sleep(300);
            }));
Thread.sleep(1000);
System.out.println("$ [delete directory]: " + dir);
            Files.delete(dir);
            count--;
Thread.sleep(300);
        }
System.out.println("$ [createDirectory]: " + dir);
        dir = Files.createDirectory(dir);
        count++;
Thread.sleep(300); 
System.out.println("$ [list]: " + dir.getParent());
Files.list(dir.getParent()).forEach(System.out::println);
        assertEquals(count, Files.list(dir.getParent()).count());

System.out.println("$ [copy (upload)]: " + src + " " + dir.resolve(src.getFileName().toString()));
        Path src2 = Files.copy(src, dir.resolve(src.getFileName().toString()));
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(1, Files.list(dir).count());
        assertEquals(Files.size(src), Files.size(src2));

System.out.println("$ [copy (internal)]: " + src2 + " " + dir.resolve(src2.getFileName().toString() + "_C"));
        Path src3 = Files.copy(src2, dir.resolve(src2.getFileName().toString() + "_C")); // SPEC target should be file
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(2, Files.list(dir).count());

System.out.println("$ [copy (internal)]: " + src2 + " " + dir.resolve(src2.getFileName().toString() + "_C2"));
        Path src3_2 = Files.copy(src2, dir.resolve(src2.getFileName().toString() + "_C2"));
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(3, Files.list(dir).count());

System.out.println("$ [rename (internal)]:" + src3 + " " + dir.resolve(src2.getFileName().toString() + "_R"));
        Path src4 = Files.move(src3, dir.resolve(src2.getFileName().toString() + "_R"));
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(3, Files.list(dir).count());

System.out.println("$ [createDirectory]: " + dir.resolve("SUB_DIR"));
        Path dir2 = Files.createDirectory(dir.resolve("SUB_DIR"));
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(4, Files.list(dir).count());

System.out.println("$ [move (internal)]:" + src4 + " " + dir2);
        Path src5 = Files.move(src4, dir2); // SPEC: move returns target
        src5 = src5.resolve(src4.getFileName());
Thread.sleep(1000);
System.out.println("$ [list]: " + dir2);
Files.list(dir2).forEach(System.out::println);
        assertEquals(1, Files.list(dir2).count());

System.out.println("$ [move (internal)]:" + src3_2 + " " + dir2.resolve("World.java"));
        Path src6 = Files.move(src3_2, dir2.resolve("World.java"));
Thread.sleep(1000);
System.out.println("$ [list]: " + dir2);
Files.list(dir2).forEach(System.out::println);
        assertEquals(2, Files.list(dir2).count());

        Path dst2 = Paths.get("tmp", "Hello.java");
        if (Files.exists(dst2)) {
            Files.delete(dst2);
        }
System.out.println("$ [copy (download)]: " + src6 + " " + dst2);
        dst2 = Files.copy(src6, dst2);
Thread.sleep(300);
System.out.println("$ [list]: " + dst2.getParent());
Files.list(dst2.getParent()).forEach(System.out::println);
        assertTrue(Files.exists(dst2));
        assertEquals(Files.size(src6), Files.size(dst2));

System.out.println("$ [delete file]: " + src2);
        Files.delete(src2);
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(1, Files.list(dir).count());

System.out.println("$ [delete file]:" + src5);
        Files.delete(src5);
Thread.sleep(300);
System.out.println("$ [list]: " + dir2);
Files.list(dir2).forEach(System.out::println);
        assertEquals(1, Files.list(dir2).count());

System.out.println("$ [delete file]:" + src6);
        Files.delete(src6);
Thread.sleep(300);
System.out.println("$ [list]: " + dir2);
Files.list(dir2).forEach(System.out::println);
        assertEquals(0, Files.list(dir2).count());

System.out.println("$ [delete directory]: " + dir2);
        Files.delete(dir2);
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);
        assertEquals(0, Files.list(dir).count());

System.out.println("$ [delete directory]: " + dir);
        Files.delete(dir);
Thread.sleep(600);
System.out.println("$ [list]: " + dir.getParent());
Files.list(dir.getParent()).forEach(System.out::println);
        assertEquals(count - 1, Files.list(dir.getParent()).count());
    }
}