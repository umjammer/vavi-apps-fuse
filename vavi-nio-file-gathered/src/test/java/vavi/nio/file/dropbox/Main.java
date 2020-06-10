/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.dropbox;

import java.net.URI;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.github.fge.fs.dropbox.provider.DropBoxFileSystemProvider;

import static vavi.nio.file.Base.testAll;


/**
 * Main. (java fs, dropbox)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/11 umjammer initial version <br>
 */
public class Main {

    @Test
    void test01() throws Exception {
        String email = System.getenv("DROPBOX_TEST_ACCOUNT");

        URI uri = URI.create("dropbox:///?id=" + email);

        testAll(new DropBoxFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP));
    }
}