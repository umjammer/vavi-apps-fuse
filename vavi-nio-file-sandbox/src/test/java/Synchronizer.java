/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vavi.nio.file.Util;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * onedrive synchronizer
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/14 umjammer initial version <br>
 */
public final class Synchronizer {

    /**
     * @param args 0: dir, 1: dir1, 2: dir2
     */
    public static void main(String[] args) throws IOException {
        String email = args[0];
        String cwd1 = args[1];
        String cwd2 = args[2];
        boolean dryRun = false;

        URI uri = URI.create("onedrive:///?id=" + email);

        FileSystem onedrivefs = FileSystems.newFileSystem(uri, Collections.emptyMap());

        // onedrive
        Path root1 = onedrivefs.getPath(cwd1);
        MyFileVisitor fileSearcher1 = new MyFileVisitor();
        Files.walkFileTree(root1, fileSearcher1);

        // local
        Path root2 = Paths.get(cwd2);
        MyFileVisitor fileSearcher2 = new MyFileVisitor();
        Files.walkFileTree(root2, fileSearcher2);

        fileSearcher2.result().parallelStream()
            .filter(path2 -> fileSearcher1.result().parallelStream().anyMatch(path1 -> {
                try {
                    return Util.toFilenameString(path2).equals(Util.toFilenameString(path1));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }))
            .forEach(path -> {
                try {
                    System.err.println("rm " + path);
                    if (!dryRun) {
                        Files.delete(path);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
    }

    static class MyFileVisitor extends SimpleFileVisitor<Path> {

        private final List<Path> list = new ArrayList<>();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
//                System.err.println(file);
                list.add(file);
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }

        public List<Path> result() {
            return list;
        }
    }
}
