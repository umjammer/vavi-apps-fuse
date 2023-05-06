/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;

import vavi.nio.file.Util;
import vavi.nio.file.googledrive.GoogleDriveFileSystemProvider;
import vavi.nio.file.googledrive.GoogleDriveOpenOption;
import vavi.nio.file.googledrive.GoogleDriveUserDefinedFileAttributesProvider;
import vavi.util.Debug;

import static vavi.nio.file.googledrive.GoogleDriveFileSystemDriver.fileSearcher;


/**
 * GoogleDriveUploader.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-05 nsano initial version <br>
 */
public class GoogleDriveUploader {

    /**
     * @param args dir
     */
    public static void main(String[] args) throws Exception {

        String email = System.getenv("GOOGLE_TEST_ACCOUNT");
//        String email = System.getenv("MICROSOFT_TEST_ACCOUNT");
        Debug.println("email: " + email);

        URI uri = URI.create("googledrive:///?id=" + email);
//        URI uri = URI.create("onedrive:///?id=" + email);
        Map<String, Object> options = new HashMap<>();
        options.put(GoogleDriveFileSystemProvider.ENV_NORMALIZE_FILENAME, false);
        try (FileSystem fs = FileSystems.newFileSystem(uri, options)) {

//        String start = args[0];
            String start = "/Volumes/nsano/Downloads/JDownloader/wip";

            GoogleDriveUploader app = new GoogleDriveUploader();
            app.root = fs.getRootDirectories().iterator().next();

            Path dir = Paths.get(start);
            try (Stream<Path> s = Files.list(dir)) {
                s.filter(p -> p.getFileName().toString().endsWith(".zip")).forEach(app::func1);
Debug.println("done: " + app.count);
            }
        }
    }

    // functions

    /** number of processed file */
    int count;

    /** google drive root */
    Path root;

    /** func 1: upload as new revision */
    void func1(Path file) {
        try {
            String normalized = Util.toFilenameString(file);
            String query = String.format("name contains '%s'", normalized);
            List<Path> results = fileSearcher.search(root, query);
            Path target = null;
            if (results.size() > 1) {
                Debug.println(Level.WARNING, "ambiguous: " + query);
                return;
            } else if (results.size() == 0) {
                Debug.println(Level.WARNING, "none: " + query);
                return;
            } else {
                target = results.get(0);
            }

            if (Files.exists(target)) {
System.out.print(target);
                if (!DRY_RUN) {
                    byte[] in = (byte[]) Files.getAttribute(target, "user:revisions");
                    String[] revisions = GoogleDriveUserDefinedFileAttributesProvider.RevisionsUtil.split(in);
                    int rcB = revisions.length;

                    // OutputStream#close() is important
                    try (OutputStream os = Files.newOutputStream(target, GoogleDriveOpenOption.IMPORT_AS_NEW_REVISION)) {
                        Files.copy(file, os);
                    }

                    in = (byte[]) Files.getAttribute(target, "user:revisions");
                    revisions = GoogleDriveUserDefinedFileAttributesProvider.RevisionsUtil.split(in);
                    int rcA = revisions.length;
System.out.println(" ... " + (rcA - rcB == 1 ? "OK" : "NG") + ", " + rcB + " -> " + rcA);
                } else {
System.out.println();
                }
                count++;
            } else {
                Debug.println(Level.WARNING, "target doesn't exists: " + target);
            }

        } catch (IOException e) {
e.printStackTrace();
        }
    }

    static final boolean DRY_RUN = false;
}
