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

import vavi.nio.file.Util;
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

//        String email = System.getenv("GOOGLE_TEST_ACCOUNT");
        String email = System.getenv("MICROSOFT_TEST_ACCOUNT");
Debug.println("email: " + email);

//        URI uri = URI.create("googledrive:///?id=" + email);
        URI uri = URI.create("onedrive:///?id=" + email);
        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

//        String start = args[0];
        String start = "/Books";

        GoogleDriveFilename app = new GoogleDriveFilename();
        Path dir = fs.getPath(start);
        Files.walkFileTree(dir, app.new MyFileVisitor());

        fs.close();
    }

    class MyFileVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
                try {
                    if (filter2(file)) {
                        func2(file);
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
    boolean filter1(Path file) {
        String filename = file.getFileName().toString();
//System.err.println(filename);
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return true;
        } else {
            return false;
        }
    }

    int count;

    boolean filter2(Path file) throws IOException {
        count++;
        String filename = file.getFileName().toString();
        String filename2 = Util.toNormalizedString(filename);
        if (!filename.equals(filename2)) {
System.err.println("\n" + filename + ", " + filename2);
            return true;
        } else {
System.err.print(".");
            if (count % 100 == 0) {
System.err.println();
            }
            return false;
        }
    }

    // functions

    /** get thumbnail of the file and save it to local */
    void func1(Path file) throws IOException {
        System.out.println(file);
    }

    void func2(Path file) throws IOException {
        if (!DRY_RUN) {
            Files.move(file, file.getParent().resolve(Util.toNormalizedString(file.getFileName().toString())));
        }
    }

    static final boolean DRY_RUN = false;
    static final boolean OVERWRITE = false;
}

/* */
