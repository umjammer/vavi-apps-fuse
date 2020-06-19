/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static vavi.nio.file.Base.testMoveFolder;


/**
 * onedrive move folder
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/20 umjammer initial version <br>
 */
public final class Main3 {

    // TODO after move directory, child's name is changed. wtf ???
    @Test
    void test01() throws Exception {
        String email = System.getenv("MICROSOFT_TEST_ACCOUNT");

        URI uri = URI.create("onedrive:///?id=" + email);
        FileSystem fs = new OneDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP);

        testMoveFolder(fs);
    }
}
