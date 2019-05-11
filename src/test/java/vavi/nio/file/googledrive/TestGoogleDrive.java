/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.fge.filesystem.provider.FileSystemRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.paralleluniverse.javafs.JavaFS;


/**
 * TestGoogleDrive.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
public class TestGoogleDrive {

    String mountPoint;

    @BeforeAll
    public void before() throws Exception {
        String email = System.getProperty("email");
        mountPoint = System.getProperty("mount_point");

        /*
         * Create the necessary elements to create a filesystem.
         * Note: the URI _must_ have a scheme of "googledrive", and
         * _must_ be hierarchical.
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

        JavaFS.mount(fs, Paths.get(mountPoint), false, true, options);
    }

    @Test
    public void testCopyFromLocalToTarget() throws Exception {
        Path from = Paths.get("/Users/nsano/tmp/2/java7.java");
        Path to = Paths.get(mountPoint, from.getFileName().toString());
        System.err.println(Files.exists(from) + ", " + Files.exists(to));
        String[] commandLine = { "/bin/cp", from.toString(), to.toString() };
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.command().forEach(System.err::println);
        Process process = processBuilder.start();
        process.waitFor();
        assertEquals(0, process.exitValue());
        assertTrue(Files.exists(to));
        assertEquals(Files.size(to), Files.size(to));
    }

    //@Test
    public void testCopyFromLocalToTarget2() throws Exception {
        Path from = Paths.get("/Users/nsano/tmp/2/java7.java");
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

    @AfterAll
    public void after() throws Exception {
        JavaFS.unmount(Paths.get(mountPoint));
    }
}

/* */
