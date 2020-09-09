/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static vavi.nio.file.Base.testDescription;


/**
 * GoogleDrive.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/31 umjammer initial version <br>
 */
public class Main6 {

    @Test
    void test01() throws Exception {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        FileSystem fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP);

        testDescription(fs);
    }

    //

    public static void main(String[] args) throws IOException {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        FileSystem fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP);

        Files.setAttribute(fs.getPath("tmp/amazon_parchase_history.txt"), "user:description", ("説明テスト " + System.currentTimeMillis()).getBytes());

        fs.close();
    }
}