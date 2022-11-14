/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static vavi.nio.file.Base.testDescription;


/**
 * OneDrive. (v2.0 graph api, msgraph engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/31 umjammer initial version <br>
 */
public class Main6 {

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

        testDescription(fs);
    }

    //

    public static void main(String[] args) throws IOException {
        String email = System.getenv("MICROSOFT4_TEST_ACCOUNT");

        URI uri = URI.create("onedrive:///?id=" + email);
        FileSystem fs = new OneDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        Files.setAttribute(fs.getPath("tmp/Cyberduck.jpg"), "user:description", ("説明テスト " + System.currentTimeMillis()).getBytes());

        fs.close();
    }
}
