/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod", "(" +
                "co\\.paralleluniverse\\.fuse\\.LoggedFuseFilesystem#log" + "|" +
                "org\\.apache\\.commons\\.logging\\.impl\\.Jdk14Logger#(log|info|warn)" + "|" +
                "org\\.apache\\.commons\\.vfs2\\.provider\\.sftp\\.SftpClientFactory\\$JSchLogger#log"
                + ")");
    }

    String mountPoint;
    FileSystem fs;
    Map<String, Object> options;

    @BeforeEach
    public void before() throws Exception {
        mountPoint = System.getenv("TEST4_MOUNT_POINT");
        String username = URLEncoder.encode(System.getenv("TEST4_SFTP_ACCOUNT"), StandardCharsets.UTF_8);
        String passPhrase = URLEncoder.encode(System.getenv("TEST4_SFTP_PASSPHRASE"), StandardCharsets.UTF_8);
        String host = System.getenv("TEST4_SFTP_HOST");
        String keyPath = URLEncoder.encode(System.getenv("TEST4_SFTP_KEYPATH"), StandardCharsets.UTF_8);
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

    @ParameterizedTest
    @ValueSource(strings = {
        "vavi.net.fuse.javafs.JavaFSFuseProvider",
        "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider",
        "vavi.net.fuse.fusejna.FuseJnaFuseProvider",
    })
    public void test02(String providerClassName) throws Exception {
        System.setProperty("vavi.net.fuse.FuseProvider.class", providerClassName);

        Base.testLargeFile(fs, mountPoint, options);

        fs.close();
    }

    //

    /**
     * <pre>
     * ~/.vavifuse/credentials.properties
     * vfs.username.sftp=host_account
     * vfs.host.sftp=hots_name
     * ssh.keyPath.sftp=/Users/you/.ssh/id_rsa
     * ssh.passphrase.sftp=pass_phrase
     * </pre>
     *
     * @param args 0: alias, args 1: mount point (should be replaced by alias)
     *             e.g. `sftp /Users/you/mnt/hostFoo_%s`
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
        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.fusejna.FuseJnaFuseProvider");

        Map<String, Object> options = new HashMap<>();
        options.put("fsname", "vfs_fs" + "@" + System.currentTimeMillis());
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_DEBUG, false);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_READ_ONLY, false);
        // vfs io uses ThreadLocal to keep internal info when read/write, so this option must be set
        options.put(vavi.net.fuse.Fuse.ENV_SINGLE_THREAD, true);
        options.put("allow_root", null);

        Fuse.getFuse().mount(fs, mountPoint, options);
    }
}

/* */
