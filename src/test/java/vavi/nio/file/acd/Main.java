/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

import com.github.fge.filesystem.provider.FileSystemRepository;

import co.paralleluniverse.javafs.JavaFS;


/**
 * Amazon Cloud Drive JavaFS.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/08/08 umjammer initial version <br>
 */
public class Main {

    public static void main(final String... args) throws IOException {
        String email = args[1];

        /*
         * Create the necessary elements to create a filesystem.
         * Note: the URI _must_ have a scheme of "acd", and
         * _must_ be hierarchical.
         */
        final URI uri = URI.create("acd://foo/");
        final Map<String, Object> env = new HashMap<>();
        env.put("email", email);
        env.put("ignoreAppleDouble", true);

        /*
         * Create the FileSystemProvider; this will be more simple once
         * the filesystem is registered to the JRE, but right now you
         * have to do like that, sorry...
         */
        final FileSystemRepository repository = new AcdFileSystemRepository();
        final FileSystemProvider provider = new AcdFileSystemProvider(repository);

        final FileSystem fs = provider.newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "acd_fs" + "@" + System.currentTimeMillis());

        JavaFS.mount(fs, Paths.get(args[0]), false, true, options);
    }
}