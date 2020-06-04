/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import vavi.net.fuse.Fuse;
import vavi.util.Debug;

import vavix.util.Checksum;

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
public class Main4 {

    static Fuse fuse;
    static String mountPoint;
    static FileSystem fs;

    @BeforeAll
    public static void before() throws Exception {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod", "co\\.paralleluniverse\\.fuse\\.LoggedFuseFilesystem#log");

//        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.javafs.JavaFSFuseProvider");
        System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider");

        mountPoint = System.getenv("TEST4_MOUNT_POINT");
Debug.println("mountPoint: " + mountPoint);
        fs = Jimfs.newFileSystem(Configuration.unix());

        Map<String, Object> options = new HashMap<>();
        options.put("fsname", "jimfs_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);
        //options.put("noapplexattr", null);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_DEBUG, false);
        options.put(vavi.net.fuse.javafs.JavaFSFuse.ENV_READ_ONLY, false);

        fuse = Fuse.getFuse();
        fuse.mount(fs, mountPoint, options);
    }

    /** */
    private int exec(String... commandLine) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.inheritIO();
        System.out.println("$ " + String.join(" ", commandLine));
        Process process = processBuilder.start();
        process.waitFor();
        return process.exitValue();
    }

    /**
     * create
     * write
     * chmod
     * chmod
     * chmod
     * flush
     * lock
     * release
     */
    @Test
    @Disabled
    public void testCopyFromLocalToTarget() throws Exception {
        Path from = Paths.get("src/test/resources/Hello.java");
        Path toDir = Paths.get(mountPoint, "VAVI_FUSE_TEST4");
        if (!Files.exists(toDir)) {
Debug.println("[mkdir] " + toDir);
            assertEquals(0, exec("/bin/mkdir", toDir.toString()));
        }
        Path to = toDir.resolve(from.getFileName());
        if (Files.exists(to)) {
Debug.println("[rm] " + to);
            assertEquals(0, exec("/bin/rm", to.toString()));
        }
Debug.println("[cp] " + from + " " + to);
        assertEquals(0, exec("/bin/cp", from.toString(), to.toString()));
        assertEquals(0, exec("/bin/ls", "-l", to.toString()));
        assertEquals(0, exec("/bin/ls", "-l", from.toString()));
        assertTrue(Files.exists(to));
        assertEquals(Files.size(from), Files.size(to));
        assertEquals(Checksum.getChecksum(from), Checksum.getChecksum(to));
        Debug.println("[rm] " + to);
        assertEquals(0, exec("/bin/rm", to.toString()));
Debug.println("[rmdir] " + toDir);
        assertEquals(0, exec("/bin/rmdir", toDir.toString()));
        assertFalse(Files.exists(to));
        assertFalse(Files.exists(toDir));
    }

    /**
     * open
     * read
     * flush
     * lock
     * release
     */
    @Test
    public void testCopyFromRemoteToTarget() throws Exception {
        Path from = Paths.get("src/test/resources/Hello.java");
        Path remoteDir = fs.getPath("/VAVI_FUSE_TEST4");
        if (!Files.exists(remoteDir)) {
Debug.println("[_mkdir] " + remoteDir);
            Files.createDirectory(remoteDir);
        }
        Path remote = remoteDir.resolve(from.getFileName().toString());
Debug.println("[_cp] " + from + " " + remote);
        Files.copy(from, remote, StandardCopyOption.REPLACE_EXISTING);
        assertTrue(Files.exists(remote));

        Path toDir = Paths.get("tmp");
        if (!Files.exists(toDir)) {
Debug.println("[_mkdir] " + toDir);
            Files.createDirectory(toDir);
        }
        Path fuseDir = Paths.get(mountPoint, remoteDir.getFileName().toString());
        Path fuse = fuseDir.resolve(remote.getFileName().toString());
        Path to = toDir.resolve(fuse.getFileName().toString());
        if (Files.exists(to)) {
Debug.println("[_rm] " + to);
            Files.delete(to);
        }
Debug.println("[cp] " + fuse + " " + to);
        assertEquals(0, exec("/bin/cp", fuse.toString(), to.toString()));
        assertEquals(0, exec("/bin/ls", "-l", fuseDir.toString()));
Debug.println("[_chmod] 644 " + to);
        Files.setAttribute(to, "posix:permissions", PosixFilePermissions.fromString("rw-r--r--"));
        assertEquals(0, exec("/bin/ls", "-l", to.toString()));
        assertTrue(Files.exists(to));
        assertEquals(Files.size(fuse), Files.size(to));
        assertEquals(Checksum.getChecksum(fuse), Checksum.getChecksum(to));
Debug.println("[rm] " + remote);
        assertEquals(0, exec("/bin/rm", fuse.toString()));
Debug.println("[rmdir] " + toDir);
        assertEquals(0, exec("/bin/rmdir", fuseDir.toString()));
        assertFalse(Files.exists(fuse));
        assertFalse(Files.exists(fuseDir));
    }

    @AfterAll
    public static void after() throws Exception {
        fuse.close();
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
//      System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.javafs.JavaFSFuseProvider");
//      System.setProperty("vavi.net.fuse.FuseProvider.class", "vavi.net.fuse.jnrfuse.JnrFuseFuseProvider");

        Main4.before();
        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await();
    }
}

/* */
