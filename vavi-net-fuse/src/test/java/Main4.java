/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import vavi.net.fuse.Base;
import vavi.net.fuse.Fuse;
import vavi.util.Debug;


/**
 * Test4. (jimfs, fuse)
 * <p>
 * upload
 * <ul>
 * <li> create
 * <li> write
 * <li> chmod
 * <li> chmod
 * <li> chmod
 * <li> flush
 * <li> lock
 * <li> release
 * </ul>
 * download
 * <ul>
 * <li> open
 * <li> read
 * <li> flush
 * <li> lock
 * <li> release
 * </ul>
 *
 * TODO read is not called because of cache???
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
@DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
public class Main4 {

    FileSystem fs;
    String mountPoint;
    Map<String, Object> options;

    @BeforeEach
    public void before() throws Exception {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod", "co\\.paralleluniverse\\.fuse\\.LoggedFuseFilesystem#log");

        mountPoint = System.getenv("TEST4_MOUNT_POINT");
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

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        Main4 app = new Main4();
        app.before();

        Fuse.getFuse().mount(app.fs, app.mountPoint, app.options);

        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await();
    }
}

/* */
