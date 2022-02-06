/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.jupiter.api.Test;


/**
 * GoogleDrive attribute user:thumbnail
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/01/26 umjammer initial version <br>
 */
class Main9 {

    @Test
    void test01() throws Exception {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        FileSystem fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        // write
        byte[] bytes = Files.readAllBytes(Paths.get(Main9.class.getResource("/duke.jpg").toURI()));

        Path path = fs.getPath("/tmp/test.zip");

        Files.setAttribute(path, "user:thumbnail", bytes);

        fs.close();

        // read
        fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        path = fs.getPath("/tmp/test.zip");

        bytes = (byte[]) Files.getAttribute(path, "user:thumbnail");
        Files.write(Paths.get("tmp/thumbnail.jpg"), bytes);

        fs.close();
    }
}

/* */
