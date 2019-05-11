/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

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
 * Main.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/03 umjammer initial version <br>
 */
public class Main {

    public static void main(final String... args) throws IOException {
        String email = args[1];

        /*
         * Create the necessary elements to create a filesystem.
         * Note: the URI _must_ have a scheme of "googledrive", and
         * _must_ be hierarchical.
         *
         * When you got "invalid_grant" during authoring,
         * remove "~/.vavifuse/googledrive/StoredCredential".
         * It's because of expiring of access token.
         */
        final URI uri = URI.create("googledrive://foo/");
        final Map<String, Object> env = new HashMap<>();
        env.put("email", email);
        env.put("ignoreAppleDouble", true);

        /*
         * Create the FileSystemProvider; this will be more simple once
         * the filesystem is registered to the JRE, but right now you
         * have to do like that, sorry...
         */
        final FileSystemRepository repository = new GoogleDriveFileSystemRepository();
        final FileSystemProvider provider = new GoogleDriveFileSystemProvider(repository);

        final FileSystem fs = provider.newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "googledrive_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);
        //options.put("noapplexattr", null);

        JavaFS.mount(fs, Paths.get(args[0]), false, false, options);
    }
}