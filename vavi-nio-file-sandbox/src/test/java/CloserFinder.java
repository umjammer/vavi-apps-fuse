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
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vavi.nio.file.Util;
import vavi.util.LevenshteinDistance;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * onedrive finding name close to
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/14 umjammer initial version <br>
 */
public final class CloserFinder {

    /**
     * @param args 0: email, 1: dir
     */
    public static void main(String[] args) throws IOException {
        String email = args[0];
        String cwd = args[1];

        URI uri = URI.create("onedrive:///?id=" + email);

        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

        Path root = fs.getPath(cwd);
        MyFileVisitor fileSearcher = new MyFileVisitor();
        Files.walkFileTree(root, fileSearcher);
        fileSearcher.result().parallelStream()
            .forEach(path1 -> {
                fileSearcher.result().parallelStream()
                    .forEach(path2 -> {
                        try {
                            if (!path1.equals(path2)) {
                                String filename1 = Util.toFilenameString(path1);
                                String filename2 = Util.toFilenameString(path2);
                                int d = LevenshteinDistance.calculate(filename1, filename2);
                                if (d > 1 && d < 5) {
                                    System.err.println(path1 + ": " + path2);
                                }
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    });
            });

        fs.close();
    }

    static class MyFileVisitor extends SimpleFileVisitor<Path> {

        private List<Path> list = new ArrayList<>();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
                list.add(file);
            }
            return CONTINUE;
        }

        public List<Path> result() {
            return list;
        }
    }
}
