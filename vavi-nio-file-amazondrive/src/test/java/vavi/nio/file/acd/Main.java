/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

import vavi.net.fuse.Fuse;


/**
 * Amazon Cloud Drive (fuse).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/08/08 umjammer initial version <br>
 */
public class Main {

    public static void main(final String... args) throws IOException {
        String email = args[1];

        URI uri = URI.create("acd:///?id=" + email);

        Map<String, Object> env = new HashMap<>();
        env.put("ignoreAppleDouble", true);

        FileSystem fs = FileSystems.newFileSystem(uri, env);

        Map<String, Object> options = new HashMap<>();
        options.put("fsname", "acd_fs" + "@" + System.currentTimeMillis());
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_DEBUG, true);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_READ_ONLY, false);

        Fuse.getFuse().mount(fs, args[0], options);
    }
}