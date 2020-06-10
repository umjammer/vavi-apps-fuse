/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static vavi.nio.file.Base.testAll;


/**
 * Main. (java fs, google drive)
 * <p>
 * When you got "invalid_grant" during authoring,
 * remove "~/.vavifuse/googledrive/StoredCredential".
 * It's because of expiring of access token.
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/03 umjammer initial version <br>
 */
public class Main {

    @Test
    void test01() throws Exception {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);

        testAll(new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP));
    }

    @Test
    @Disabled
    void test02() throws Exception {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT2");

        URI uri = URI.create("googledrive:///?id=" + email);

        FileSystem fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP);
        Files.list(fs.getPath("/")).forEach(System.out::println);
    }
}