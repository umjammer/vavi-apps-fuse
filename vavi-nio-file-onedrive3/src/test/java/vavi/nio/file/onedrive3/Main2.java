/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static vavi.nio.file.Base.testLargeFile;


/**
 * OneDrive. (v2.0 graph api, cyberduck engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/xx umjammer initial version <br>
 */
public class Main2 {

    @Test
    void test01() throws Exception {
        String email = System.getenv("MICROSOFT3_TEST_ACCOUNT");

        URI uri = URI.create("onedrive:///?id=" + email);
        FileSystem fs = new OneDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP);

        testLargeFile(fs, OneDriveUploadOption.class);
    }
}