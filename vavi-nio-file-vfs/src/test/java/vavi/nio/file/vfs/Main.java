/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static vavi.nio.file.Base.testAll;

import co.paralleluniverse.javafs.JavaFS;


/**
 * Commons VFS JavaFS.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/03 umjammer initial version <br>
 */
public class Main {

    /**
     * @param args 0: alias, args 1: mount point (should be replaced by alias)
     */
    public static void main(final String... args) throws IOException {
        String alias = args[0];
        String mountPoint = String.format(args[1], alias);

        final URI uri = URI.create("vfs:sftp:///Users/nsano/tmp/vfs?alias=" + alias);

        final Map<String, Object> env = new HashMap<>();
        env.put("ignoreAppleDouble", true);

        FileSystem fs = new VfsFileSystemProvider().newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "vfs_fs" + "@" + System.currentTimeMillis());

        JavaFS.mount(fs, Paths.get(mountPoint), false, true, options);
    }

    @Test
    @Disabled
    void test00() throws Exception {
        URI uri = URI.create("vfs:sftp://user:password@nsanomac4.local:10022/Users/nsano?alias=alias");
        System.err.println(uri.getScheme());
        System.err.println(uri.getHost());
        System.err.println(uri.getPath());
        System.err.println(uri.getPort());
        System.err.println(uri.getQuery());
        System.err.println(uri.getFragment());
        System.err.println(uri.getAuthority());
        System.err.println(uri.getUserInfo());

        String uriString = uri.toString();
        URI subUri = URI.create(uriString.substring(uriString.indexOf(':') + 1));
        System.err.println(subUri.getScheme());
        System.err.println(subUri.getHost());
        System.err.println(subUri.getPath());
        System.err.println(subUri.getPort());
        System.err.println(subUri.getQuery());
        System.err.println(subUri.getFragment());
        System.err.println(subUri.getAuthority());
        System.err.println(subUri.getUserInfo());
    }

    /**
     * environment variable
     * <ul>
     * <li> TEST_SFTP_ACCOUNT
     * <li> TEST_SFTP_PASSPHRASE
     * <li> TEST_SFTP_HOST
     * <li> TEST_SFTP_KEYPATH
     * <li> TEST_SFTP_PATH
     * </ul>
     */
    @Test
    void test01() throws Exception {
        String username = URLEncoder.encode(System.getenv("TEST_SFTP_ACCOUNT"), "utf-8");
        String passPhrase = URLEncoder.encode(System.getenv("TEST_SFTP_PASSPHRASE"), "utf-8");
        String host = System.getenv("TEST_SFTP_HOST");
        String keyPath = URLEncoder.encode(System.getenv("TEST_SFTP_KEYPATH"), "utf-8");
        String path = System.getenv("TEST_SFTP_PATH");

        URI uri = URI.create(String.format("vfs:sftp://%s@%s%s?keyPath=%s&passphrase=%s", username, host, path, keyPath, passPhrase));

        testAll(new VfsFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP));
    }
}