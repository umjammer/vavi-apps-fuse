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

import static vavi.nio.file.Base.testMoveFolder;


/**
 * OneDrive. move folder (v2.0 graph api, msgraph engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/31 umjammer initial version <br>
 */
public class Main3 {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod",
                           "(" +
                           "com\\.microsoft\\.graph\\.logger\\.DefaultLogger#logDebug" + "|" +
                           "vavi\\.nio\\.file\\.onedrive4\\.graph\\.MyLogger#logDebug" +
                           ")");
    }

    @Test
    void test01() throws Exception {
        String email = System.getenv("MICROSOFT4_TEST_ACCOUNT");

        URI uri = URI.create("onedrive:///?id=" + email);
        FileSystem fs = new OneDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        testMoveFolder(fs);
    }
}