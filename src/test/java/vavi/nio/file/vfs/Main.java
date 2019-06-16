/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

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
 * Commons VFS JavaFS.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/03 umjammer initial version <br>
 */
public class Main {

    /**
     * @param args 0: mount point (should be replaced by alias), 1: baseUrl 2: alias
     */
    public static void main(final String... args) throws IOException {
        String baseUrl = args[1];
        String alias = args[2];
        String mountPoint = String.format(args[0], alias);

        /*
         * Create the necessary elements to create a filesystem.
         * Note: the URI _must_ have a scheme of "vfs", and
         * _must_ be hierarchical.
         */
        final URI uri = URI.create("vfs://foo/");
        final Map<String, Object> env = new HashMap<>();
        env.put("alias", alias);
        env.put("baseUrl", baseUrl);
        env.put("ignoreAppleDouble", true);

        /*
         * Create the FileSystemProvider; this will be more simple once
         * the filesystem is registered to the JRE, but right now you
         * have to do like that, sorry...
         */
        final FileSystemRepository repository = new VfsFileSystemRepository();
        final FileSystemProvider provider = new VfsFileSystemProvider(repository);

        final FileSystem fs = provider.newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "vfs_fs" + "@" + System.currentTimeMillis());

        JavaFS.mount(fs, Paths.get(mountPoint), false, true, options);
    }
}