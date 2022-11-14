/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
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
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vavi.util.Debug;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * GoogleDriveFilename.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/01/26 umjammer initial version <br>
 */
public class GoogleDriveFilename {

    /**
     * @param args dir
     */
    public static void main(String[] args) throws Exception {

        String email = System.getenv("GOOGLE_TEST_ACCOUNT");
Debug.println("email: " + email);

        URI uri = URI.create("googledrive:///?id=" + email);
        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

//        String start = args[0];
        String start = "/Books/Comics";

        Path dir = fs.getPath(start);
        Files.walkFileTree(dir, new MyFileVisitor());

        fs.close();
    }

    static class MyFileVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
                try {
                    if (filter1(file)) {
                        func1(file);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return CONTINUE;
        }
    }

    // filters

    static final Pattern pattern = Pattern.compile("^.+\\(1\\).+$");

    /** {@link #pattern} */
    static boolean filter1(Path file) {
        String filename = file.getFileName().toString();
//System.err.println(filename);
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return true;
        } else {
            return false;
        }
    }

    // functions

    /** get thumbnail of the file and save it to local */
    static void func1(Path file) throws IOException {
        System.out.println(file);
    }

    static final boolean DRY_RUN = false;
    static final boolean OVERWRITE = false;
}

/* */
