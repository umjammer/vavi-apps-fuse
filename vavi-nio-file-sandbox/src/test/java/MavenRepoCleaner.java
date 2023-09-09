/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import vavi.util.Debug;

import static java.nio.file.FileVisitResult.CONTINUE;


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
     * @param args none
     */
    public static void main(String[] args) throws IOException {
        Files.walkFileTree(Paths.get(System.getProperty("user.home"), ".m2/repository"), new MyFileVisitor());
Debug.println("Done");
    }

    static class MyFileVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            try {
                long c1;
                try (Stream<Path> files = Files.list(dir)) {
                    c1 = files.count();
                }

                // pom
                DirectoryStream<Path> iterable = Files.newDirectoryStream(dir, "*.pom");
                long cP = StreamSupport.stream(iterable.spliterator(), false).count();
                iterable.close();

                // contents
                iterable = Files.newDirectoryStream(dir, "*.{jar,dylib,dll,so}");
                long cC = StreamSupport.stream(iterable.spliterator(), false).count();
                iterable.close();

                // subdir
                long cD;
                try (Stream<Path> files = Files.list(dir).filter(p -> p.toFile().isDirectory())) {
                    cD = files.count();
                }

                // garbage by 3rd parties
                iterable = Files.newDirectoryStream(dir, ".DS_Store");
                long c3 = StreamSupport.stream(iterable.spliterator(), false).count();
                iterable.close();

                iterable = Files.newDirectoryStream(dir, "resolver-status.properties");
                long c4 = StreamSupport.stream(iterable.spliterator(), false).count();
                iterable.close();

                iterable = Files.newDirectoryStream(dir, "maven-metadata-jitpack.io.xml*");
                c4 += StreamSupport.stream(iterable.spliterator(), false).count();
                iterable.close();

                if (c1 == 0) {
                    // no files
                    System.out.println("DIR0: " + dir);

                    Files.delete(dir);
                } else if (cP == 0 && cD == 0 && c1 != c4) {
                    // no pom
                    System.out.println("DIRP: " + dir);

//try (Stream<Path> files = Files.list(dir)) {
// files.forEach(System.out::println);
//}
                    rm(dir);
//                } else if (cC == 0 && cD == 0) {
//                    // no contents
//                    System.out.println("DIRC: " + dir);
//
//try (Stream<Path> files = Files.list(dir)) {
// files.forEach(System.out::println);
//}
//                    rm(dir);
                } else if (c1 == c3) {
                    // only mac files
                    System.out.println("DIRM " + dir);

                    rm(dir);
                }
            } catch (IOException e) {
                System.err.println("ERROR: " + dir);
                e.printStackTrace();
            }
            return CONTINUE;
        }
    }

    /** rm -r */
    static void rm(Path pathToBeDeleted) throws IOException {
        Files.walk(pathToBeDeleted)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}