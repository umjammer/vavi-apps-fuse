/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import vavi.util.Debug;

import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Base.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/06/05 umjammer initial version <br>
 */
public class Base {

    /** */
    private Base() {
    }

    /** */
    private static int exec(String... commandLine) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.inheritIO();
        System.out.println("$ " + String.join(" ", commandLine));
        Process process = processBuilder.start();
        process.waitFor();
        return process.exitValue();
    }

    /** */
    public static void testFuse(FileSystem fs, String mountPoint, Map<String, Object> options) throws Exception {
        try (Fuse fuse = Fuse.getFuse()) {
            fuse.mount(fs, mountPoint, options);

            // local -> remote
            Path local = Paths.get("src/test/resources/Hello.java");
            Path remoteDir = Paths.get(mountPoint, "VAVI_FUSE_TEST4");
            if (!Files.exists(remoteDir)) {
Debug.println("[mkdir] " + remoteDir);
                assertEquals(0, exec("/bin/mkdir", remoteDir.toString()));
            }
            Path remote = remoteDir.resolve(local.getFileName());
            if (Files.exists(remote)) {
Debug.println("[rm] " + remote);
                assertEquals(0, exec("/bin/rm", remote.toString()));
            }

Debug.println("[cp] " + local + " " + remote);
            assertEquals(0, exec("/bin/cp", local.toString(), remote.toString()));
            assertEquals(0, exec("/bin/ls", "-l", remote.toString()));
            assertEquals(0, exec("/bin/ls", "-l", local.toString()));
            assertTrue(Files.exists(remote));
            assertEquals(Files.size(local), Files.size(remote));
            assertEquals(Checksum.getChecksum(local), Checksum.getChecksum(remote));

            // remote -> local
            Path localTmpDir = Paths.get("tmp");
            if (!Files.exists(localTmpDir)) {
Debug.println("[_mkdir] " + localTmpDir);
                Files.createDirectory(localTmpDir);
            }
            Path localTmp = localTmpDir.resolve(remote.getFileName().toString());
            if (Files.exists(localTmp)) {
Debug.println("[_rm] " + localTmp);
                Files.delete(localTmp);
            }

Debug.println("[cp] " + remote + " " + localTmp);
            assertEquals(0, exec("/bin/cp", remote.toString(), localTmp.toString()));
            assertEquals(0, exec("/bin/ls", "-l", remoteDir.toString()));
Debug.println("[_chmod] 644 " + localTmp);
            assertEquals(0, exec("/bin/ls", "-l", localTmp.toString()));
            assertTrue(Files.exists(localTmp));
            assertEquals(Files.size(remote), Files.size(localTmp));
            assertEquals(Checksum.getChecksum(remote), Checksum.getChecksum(localTmp));

            // clean up
Debug.println("[rm] " + remote);
            assertEquals(0, exec("/bin/rm", remote.toString()));
Debug.println("[rmdir] " + localTmpDir);
            assertEquals(0, exec("/bin/rmdir", remoteDir.toString()));
            assertFalse(Files.exists(remote));
            assertFalse(Files.exists(remoteDir));
Debug.println("[_rm] " + localTmp);
            Files.delete(localTmp);
            assertFalse(Files.exists(localTmp));
        }
    }
}

/* */
