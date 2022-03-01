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

import static org.junit.jupiter.api.Assertions.assertNotEquals;


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

        Path sourcePath = Paths.get(Main9.class.getResource("/test.zip").toURI());

//        String name = "/tmp/test.zip";
        String name = "/tmp/test" + System.currentTimeMillis() + ".zip";
        Path targetPath = fs.getPath(name);

        Files.copy(sourcePath, targetPath);

        Thread.sleep(10000);

        Files.setAttribute(targetPath, "user:thumbnail", bytes);

        int retry = 3;
        while (retry > 0) {
            bytes = (byte[]) Files.getAttribute(targetPath, "user:thumbnail");
            if (bytes.length == 0) {
                fs.close();

                fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

                targetPath = fs.getPath(name);

                Thread.sleep(10000);

                Files.setAttribute(targetPath, "user:thumbnail", bytes);

                retry--;
            } else {
                break;
            }
        }

        fs.close();

        Thread.sleep(1000);

        // read
        fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        targetPath = fs.getPath(name);

        bytes = (byte[]) Files.getAttribute(targetPath, "user:thumbnail");

        Files.delete(targetPath);
        fs.close();

        assertNotEquals(0, bytes.length);
        Files.write(Paths.get("tmp/thumbnail.jpg"), bytes);
    }
}

/* */
