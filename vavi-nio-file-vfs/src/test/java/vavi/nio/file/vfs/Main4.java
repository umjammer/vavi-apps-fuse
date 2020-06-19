/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vavi.net.fuse.Base;
import vavi.net.fuse.Fuse;


/**
 * Main4. (fuse)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
@DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
public class Main4 {

    String mountPoint;
    FileSystem fs;
    Map<String, Object> options;

    @BeforeEach
    public void before() throws Exception {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod", "co\\.paralleluniverse\\.fuse\\.LoggedFuseFilesystem#log");

        mountPoint = System.getenv("TEST4_MOUNT_POINT");
        String username = URLEncoder.encode(System.getenv("TEST4_SFTP_ACCOUNT"), "utf-8");
        String passPhrase = URLEncoder.encode(System.getenv("TEST4_SFTP_PASSPHRASE"), "utf-8");
        String host = System.getenv("TEST4_SFTP_HOST");
        String keyPath = URLEncoder.encode(System.getenv("TEST4_SFTP_KEYPATH"), "utf-8");
        String path = System.getenv("TEST4_SFTP_PATH");

        URI uri = URI.create(String.format("vfs:sftp://%s@%s%s?keyPath=%s&passphrase=%s", username, host, path, keyPath, passPhrase));

        Map<String, Object> env = new HashMap<>();
        env.put("ignoreAppleDouble", true);

        fs = FileSystems.newFileSystem(uri, env);

        options = new HashMap<>();
        options.put("fsname", "vfs_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);
        //options.put("noapplexattr", null);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_DEBUG, false);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_READ_ONLY, false);
        // vfs io uses ThreadLocal to keep internal info when read/write, so this option must be set
        options.put(vavi.net.fuse.Fuse.ENV_SINGLE_THREAD, true);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "vavi.net.fuse.javafs.JavaFSFuseProvider",
        "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider",
        "vavi.net.fuse.fusejna.FuseJnaFuseProvider",
    })
    public void test01(String providerClassName) throws Exception {
        System.setProperty("vavi.net.fuse.FuseProvider.class", providerClassName);

        Base.testFuse(fs, mountPoint, options);

        fs.close();
    }

    //

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

//        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.javafs.JavaFSFuseProvider");
//        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider");

        Map<String, Object> options = new HashMap<>();
        options.put("fsname", "vfs_fs" + "@" + System.currentTimeMillis());
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_DEBUG, false);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_READ_ONLY, false);
        // vfs io uses ThreadLocal to keep internal info when read/write, so this option must be set
        options.put(vavi.net.fuse.Fuse.ENV_SINGLE_THREAD, true);

        Fuse.getFuse().mount(fs, mountPoint, options);
    }
}

/* */
