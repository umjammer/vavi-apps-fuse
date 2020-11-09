/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.archive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import vavi.net.fuse.Fuse;


/**
 * Main4. (fuse)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/10/07 umjammer initial version <br>
 */
@DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
public class Main4 {

    public static void main(String[] args) throws IOException {
        String file = "/Users/nsano/Downloads/Kairatune-1.2.6-macOS.pkg";

        URI uri = URI.create("archive:file:" + file);

        FileSystem fs = new ArchiveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP);

        Map<String, Object> options = new HashMap<>();
        options.put("fsname", "archive_fs" + "@" + System.currentTimeMillis());

        Fuse.getFuse().mount(fs, args[0], options);
    }
}

/* */
