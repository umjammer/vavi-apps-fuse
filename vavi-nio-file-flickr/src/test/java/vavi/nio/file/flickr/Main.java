/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.flickr;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import co.paralleluniverse.javafs.JavaFS;


/**
 * Flickr JavaFS.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/08/08 umjammer initial version <br>
 */
public class Main {

    public static void main(String[] args) throws IOException {
        String email = args[1];

        URI uri = URI.create("flickr://foo?id=" + email);

        Map<String, Object> env = new HashMap<>();
        env.put("ignoreAppleDouble", true);

        final FileSystem fs = FileSystems.newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "flickr_fs" + "@" + System.currentTimeMillis());

        JavaFS.mount(fs, Paths.get(args[0]), false, true, options);
    }
}