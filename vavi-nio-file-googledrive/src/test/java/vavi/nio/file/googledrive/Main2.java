/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static vavi.nio.file.Base.testLargeFile;


/**
 * GoogleDrive. (v3)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/xx umjammer initial version <br>
 */
public class Main2 {

    /**
     * caution! google drive api quota is small for repeating tests.
     */
    @Test
    @DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
    void test01() throws Exception {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        FileSystem fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        testLargeFile(fs, null);
    }
}