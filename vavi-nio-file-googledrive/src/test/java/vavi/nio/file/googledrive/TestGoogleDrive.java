/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.paralleluniverse.javafs.JavaFS;


/**
 * TestGoogleDrive. (fuse-jnr)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
@DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
public class TestGoogleDrive {

    String mountPoint;

    @BeforeEach
    public void before() throws Exception {
        String email = System.getenv("TEST4_ACCOUNT");
        mountPoint = System.getenv("TEST4_MOUNT_POINT");

        URI uri = URI.create("googledrive:///?id=" + email);

        Map<String, Object> env = new HashMap<>();
        env.put("ignoreAppleDouble", true);

        FileSystem fs = FileSystems.newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "googledrive_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);
        //options.put("noapplexattr", null);

        JavaFS.mount(fs, Paths.get(mountPoint), false, true, options);
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
        JavaFS.unmount(Paths.get(mountPoint));
    }
}

/* */
