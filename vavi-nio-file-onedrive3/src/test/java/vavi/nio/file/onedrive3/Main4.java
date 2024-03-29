/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
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
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod",
                           "co\\.paralleluniverse\\.fuse\\.LoggedFuseFilesystem#log");
    }

    String mountPoint;
    FileSystem fs;
    Map<String, Object> options;

    @BeforeEach
    public void before() throws Exception {
        String email = System.getenv("TEST4_ACCOUNT");
        mountPoint = System.getenv("TEST4_MOUNT_POINT");

        URI uri = URI.create("onedrive:///?id=" + email);

        Map<String, Object> env = new HashMap<>();
        env.put("ignoreAppleDouble", true);

        fs = new OneDriveFileSystemProvider().newFileSystem(uri, env);

        options = new HashMap<>();
        options.put("fsname", "onedrive_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);
        //options.put("noapplexattr", null);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_DEBUG, false);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_READ_ONLY, false);
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

    public static void main(final String... args) throws IOException {
        String email = args[1];

        URI uri = URI.create("onedrive3:///?id=" + email);

        OAuth2AppCredential appCredential = new MicrosoftGraphLocalAppCredential();

        Map<String, Object> env = new HashMap<>();
        env.put(OneDriveFileSystemProvider.ENV_APP_CREDENTIAL, appCredential);
        env.put("ignoreAppleDouble", true);

        FileSystem fs = new OneDriveFileSystemProvider().newFileSystem(uri, env);

//        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.javafs.JavaFSFuseProvider");
//        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider");

        Map<String, Object> options = new HashMap<>();
        options.put("fsname", "onedrive_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_DEBUG, true);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_READ_ONLY, false);

        Fuse.getFuse().mount(fs, args[0], options);
    }
}

/* */
