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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vavi.nio.file.Util;
import vavi.nio.file.googledrive.GoogleDriveFileSystemProvider;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * CloudDriveFilename.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/01/26 umjammer initial version <br>
 */
public class GoogleDriveFilename {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod",
                "(" +
                "com\\.microsoft\\.graph\\.logger\\.DefaultLogger#logDebug" + "|" +
                "vavi\\.nio\\.file\\.onedrive4\\.graph\\.MyLogger#logDebug" +
                ")");
    }

    /**
     * @param args url dir
     */
    public static void main(String[] args) throws Exception {

        String url = args[0];
        String start = args[1];

        URI uri = URI.create(url);
        Map<String, Object> options = new HashMap<>();
        options.put(GoogleDriveFileSystemProvider.ENV_NORMALIZE_FILENAME, false);
        try (FileSystem fs = FileSystems.newFileSystem(uri, options)) {

            GoogleDriveFilename app = new GoogleDriveFilename();
            Path dir = fs.getPath(start);
            Files.walkFileTree(dir, app.new MyFileVisitor());

System.err.println("\ndone: " + app.done);
        }
    }

    class MyFileVisitor extends SimpleFileVisitor<Path> {
        @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            exec(dir);
            return CONTINUE;
        }

        @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
                exec(file);
            }
            return CONTINUE;
        }

        private void exec(Path path) {
            try {
                if (filter2(path)) {
                    func2(path);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // filters

    static final Pattern pattern = Pattern.compile("^.+\\(1\\).+$");

    /** filter 1: {@link #pattern} */
    boolean filter1(Path file) {
        String filename = file.getFileName().toString();
//System.err.println(filename);
        Matcher matcher = pattern.matcher(filename);
        return matcher.find();
    }

    int count;
    int done;

    /** filter 2: only in NFD */
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

    /** func 1: just print a name */
    void func1(Path file) throws IOException {
        System.out.println(file);
    }

    /** func 2: to NFC */
    void func2(Path file) throws IOException {
        if (!DRY_RUN) {
            // onedrive cannot move nfd to nfc, so take 2 step.
            String normalized = Util.toNormalizedString(file.getFileName().toString());
            Path tmp = file.getParent().resolve("RENAMING-TEMPORARY-" + normalized);
            Files.move(file, tmp);
            Files.move(tmp, file.getParent().resolve(normalized));
            done++;
        }
    }

    static final boolean DRY_RUN = false;
    static final boolean OVERWRITE = false;
}

/* */
