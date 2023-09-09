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
 * Checks that trashed files' original ones exist or not.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-10 nsano initial version <br>
 */
public class GoogleDriveDeletedChecker {

    /**
     * @param args dir
     */
    public static void main(String[] args) throws Exception {

        String email = System.getenv("GOOGLE_TEST_ACCOUNT");
        Debug.println("email: " + email);

        URI uri = URI.create("googledrive:///?id=" + email);
        Map<String, Object> options = new HashMap<>();
        options.put(GoogleDriveFileSystemProvider.ENV_NORMALIZE_FILENAME, false);
        try (FileSystem fs = FileSystems.newFileSystem(uri, options)) {

            GoogleDriveDeletedChecker app = new GoogleDriveDeletedChecker();
            app.root = fs.getRootDirectories().iterator().next();

            String query = "name contains '.zip' and trashed=true";
            List<Path> results = fileSearcher.search(app.root, query);

            results.forEach(app::func1);
Debug.println("done: " + app.count);
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
System.out.println(normalized);
            String query = String.format("name = '%s' and trashed=false", normalized);
            List<Path> results = fileSearcher.search(root, query);
            if (results.size() > 1) {
Debug.println(Level.WARNING, "ambiguous: " + normalized);
results.forEach(System.err::println);
            } else if (results.size() == 0) {
Debug.println(Level.WARNING, "none: " + normalized);
            } else {
                count++;
            }
        } catch (IOException e) {
e.printStackTrace();
        }
    }
}
