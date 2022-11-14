/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.FileSystem;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vavi.net.fuse.Base;
import vavi.net.fuse.Fuse;
import vavi.util.Debug;


/**
 * Test2. (jimfs, fuse)
 * <p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
public class Main2 {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod",
                           "co\\.paralleluniverse\\.fuse\\.LoggedFuseFilesystem#log");
    }

    FileSystem fs;
    String mountPoint;
    Map<String, Object> options;

    @BeforeEach
    public void before() throws Exception {

        mountPoint = System.getenv("FUSE_MOUNT_POINT");
Debug.println("mountPoint: " + mountPoint);

        fs = Jimfs.newFileSystem(Configuration.unix());

        options = new HashMap<>();
        options.put("fsname", "jimfs_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);
        //options.put("noapplexattr", null);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_DEBUG, false);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_READ_ONLY, false);
    }

    @ParameterizedTest
    @EnabledIfEnvironmentVariable(named = "FUSE_MOUNT_POINT", matches = ".+")
    @ValueSource(strings = {
        "vavi.net.fuse.javafs.JavaFSFuseProvider",
        "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider",
        "vavi.net.fuse.fusejna.FuseJnaFuseProvider",
    })
    public void test01(String providerClassName) throws Exception {
        System.setProperty("vavi.net.fuse.FuseProvider.class", providerClassName);
System.err.println("--------------------------- " + providerClassName + " ---------------------------");

        Base.testLargeFile(fs, mountPoint, options);

        fs.close();
    }

    /**
     * @param args none
     */
    public static void main(String[] args) throws Exception {
        Main2 app = new Main2();
        app.before();

        Fuse.getFuse().mount(app.fs, app.mountPoint, app.options);
    }
}

/* */
