/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static vavi.nio.file.Base.testAll;


/**
 * OneDrive JavaFS. (v2.0 graph api, ms graph api engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/xx umjammer initial version <br>
 */
public class Main {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod",
                           "(" +
                           "com\\.microsoft\\.graph\\.logger\\.DefaultLogger#logDebug" + "|" +
                           "vavi\\.nio\\.file\\.onedrive4\\.graph\\.MyLogger#logDebug" +
                           ")");
    }

    @Test
    void test01() throws Exception {
        String email = System.getenv("MICROSOFT_TEST_ACCOUNT");

        URI uri = URI.create("onedrive:///?id=" + email);

        testAll(new OneDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap()));
    }

    /** */
    public static void main(String[] args) throws Exception {
        String email = System.getenv("MICROSOFT_TEST_ACCOUNT");

        URI uri = URI.create("onedrive:///?id=" + email);

        FileSystem fs = new OneDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());
        Files.list(fs.getPath("/")).forEach(System.out::println);
    }
}