/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static vavi.nio.file.Base.testLargeFile;


/**
 * OneDrive. (v2.0 graph api, msgraph engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/31 umjammer initial version <br>
 */
public class Main2 {

    @Test
    void test01() throws Exception {
        String email = System.getenv("MICROSOFT_TEST_ACCOUNT");

        URI uri = URI.create("onedrive:///?id=" + email);
        FileSystem fs = new OneDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        testLargeFile(fs, OneDriveUploadOption.class);
    }
}