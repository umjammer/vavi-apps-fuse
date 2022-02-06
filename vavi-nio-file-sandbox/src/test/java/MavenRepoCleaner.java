/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.StreamSupport;

import vavi.util.Debug;

import static java.nio.file.FileVisitResult.CONTINUE;

import static com.rainerhahnekamp.sneakythrow.Sneaky.sneaked;


/**
 * maven repo cleaner.
 * <p>
 * fu*king eclipse content assist leaves half way named junk files and folders
 * in the repository.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2021/11/12 umjammer initial version <br>
 */
public class MavenRepoCleaner {

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        Files.walkFileTree(Paths.get(System.getProperty("user.home"), ".m2/repository"), new MyFileVisitor());
Debug.println("Done");
    }

    static class MyFileVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            try {
                long c1 = Files.list(dir).count();

                DirectoryStream<Path> iterable = Files.newDirectoryStream(dir, "*.lastUpdated");
                long c2 = StreamSupport.stream(iterable.spliterator(), false).count();
                iterable.close();

                iterable = Files.newDirectoryStream(dir, ".DS_Store");
                long c3 = StreamSupport.stream(iterable.spliterator(), false).count();
                iterable.close();

                iterable = Files.newDirectoryStream(dir, "resolver-status.properties");
                long c4 = StreamSupport.stream(iterable.spliterator(), false).count();
                iterable.close();

                if (c1 == 0) {
                    System.out.println("DIR0: " + dir);

System.err.println("DEL0: " + dir);
                    Files.delete(dir);
                } else if (c1 == c2) {
                    System.out.println("DIR1: " + dir);

System.err.println("DEL1: " + dir);
                    Files.list(dir).forEach(sneaked(Files::delete));
                    Files.delete(dir);
                } else if (c1 == c2 + c4) {
                    System.out.println("DIR2: " + dir);

System.err.println("DEL2: " + dir);
                    Files.list(dir).forEach(sneaked(Files::delete));
                    Files.delete(dir);
                } else if (c1 == c3) {
                    System.out.println("DIR1X " + dir);

System.err.println("DELX: " + dir);
                    Files.list(dir).forEach(sneaked(Files::delete));
                    Files.delete(dir);
                }
            } catch (IOException e) {
                System.err.println("ERROR: " + dir + ", " + e.getMessage());
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }
    }
}