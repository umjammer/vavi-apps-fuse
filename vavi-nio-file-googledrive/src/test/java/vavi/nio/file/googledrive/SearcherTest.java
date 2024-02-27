/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.nio.file.Util;
import vavi.util.Debug;

import static vavi.nio.file.googledrive.GoogleDriveFileSystemDriver.fileSearcher;


/**
 * SearcherTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-18 nsano initial version <br>
 */
public class SearcherTest {

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");
Debug.println("email: " + email);

        URI uri = URI.create("googledrive:///?id=" + email);
        Map<String, Object> options = new HashMap<>();
        options.put(GoogleDriveFileSystemProvider.ENV_NORMALIZE_FILENAME, false);

        try (FileSystem fs = FileSystems.newFileSystem(uri, options)) {
            Path root = fs.getRootDirectories().iterator().next();

            Path file = Paths.get(System.getProperty("user.home"), "Downloads/JDownloader/wip4", "(一般コミック) [成田芋虫] IT'S MY LIFE 第03巻.zip");
            String normalized = Util.toFilenameString(file).replace("\\", "\\\\").replace("'", "\\'");
            String query = String.format("name = '%s' and trashed=false", normalized);

            List<Path> results = fileSearcher.search(root, query);
            if (results.size() > 1) {
Debug.println(Level.WARNING, "ambiguous: " + query);
                results.forEach(System.err::println);
            } else if (results.isEmpty()) {
Debug.println(Level.WARNING, "none: " + query);
            } else {
Debug.println(Level.INFO, "found: " + query);
                System.err.println(results.get(0));
Debug.println(Level.INFO, "fs?: " + results.get(0).getFileSystem());
Debug.println(Level.INFO, "exists?: " + Files.exists(results.get(0)));
            }
        }
    }
}
