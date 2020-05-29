/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */



import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import vavi.net.fuse.Fuse;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test4. (jimfs, fuse)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
@DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
public class Test4 {

    Fuse fuse;
    String mountPoint;

    @BeforeEach
    public void before() throws Exception {
//        System.setProperty("fuseClassName", "vavi.net.fuse.javafs.JavaFSFuse");
        System.setProperty("fuseClassName", "vavi.net.fuse.jnrfuse.JnrFuseFuse");

        mountPoint = System.getenv("TEST4_MOUNT_POINT");
Debug.println("mountPoint: " + mountPoint);
        FileSystem fs = Jimfs.newFileSystem(Configuration.osX());

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "jimfs_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);
        //options.put("noapplexattr", null);

        fuse = Fuse.Factory.getFuse();
        fuse.mount(fs, mountPoint, options);
    }

    /** */
    private int exec(String... commandLine) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.command().forEach(System.err::println);
        Process process = processBuilder.start();
        process.waitFor();
Thread.sleep(1000);
        return process.exitValue();
    }

    @Test
    public void testCopyFromLocalToTarget() throws Exception {
        Path from = Paths.get("src/test/resources/Hello.java");
        Path toDir = Paths.get(mountPoint, "VAVI_FUSE_TEST4");
        if (!Files.exists(toDir)) {
            assertEquals(0, exec("/bin/mkdir", toDir.toString()));
        }
        Path to = toDir.resolve(from.getFileName());
        if (Files.exists(to)) {
            assertEquals(0, exec("/bin/rm", to.toString()));
        }
        assertEquals(0, exec("/bin/cp", from.toString(), to.toString()));
        assertTrue(Files.exists(to));
        assertEquals(Files.size(from), Files.size(to));
        assertEquals(0, exec("/bin/rm", to.toString()));
        assertEquals(0, exec("/bin/rmdir", toDir.toString()));
        assertFalse(Files.exists(to));
        assertFalse(Files.exists(toDir));
    }

    @Test
    @Disabled
    public void testCopyFromLocalToTarget2() throws Exception {
        Path from = Paths.get("src/test/resources/Hello.java");
        Path to = Paths.get(mountPoint);
        System.err.println(Files.exists(from) + ", " + Files.exists(to));
        String[] commandLine = { "/bin/cp", from.toString(), to.toString() };
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.command().forEach(System.err::println);
        Process process = processBuilder.start();
        process.waitFor();
        assertEquals(0, process.exitValue());
        assertTrue(Files.exists(Paths.get(mountPoint, from.getFileName().toString())));
    }

    @AfterEach
    public void after() throws Exception {
        fuse.unmount();
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
//      System.setProperty("fuseClassName", "vavi.net.fuse.javafs.JavaFSFuse");
//      System.setProperty("fuseClassName", "vavi.net.fuse.jnrfuse.JnrFuseFuse");

        Test4 app = new Test4();
        app.before();
        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await();
    }
}

/* */
