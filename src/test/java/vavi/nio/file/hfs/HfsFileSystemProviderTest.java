/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.hfs;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.jupiter.api.Test;


/**
 * HfsFileSystemProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/23 umjammer initial version <br>
 */
class HfsFileSystemProviderTest {

    @Test
    void test3() throws Exception {
        URI uri = new URI("hfs:file:/Users/nsano/Downloads/Play-20170829.dmg");
        FileSystem fs = new HfsFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP);
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }
}

/* */
