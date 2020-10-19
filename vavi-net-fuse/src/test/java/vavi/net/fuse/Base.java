/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;

import vavi.nio.fuse.Fuse;
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
        Path local = null;
        Path localTmp = null;
        try (Fuse fuse = Fuse.getFuse()) {
            fuse.mount(fs, mountPoint, options);

            Path localTmpDir = Paths.get("tmp");
            if (!Files.exists(localTmpDir)) {
Debug.println("[_mkdir] " + localTmpDir);
                Files.createDirectory(localTmpDir);
            }

            // local -> remote
            local = Files.createTempFile(localTmpDir, "vavifuse-1-", ".tmp");
            byte[] bytes = new byte[5 * 1024 * 1024 + 12345];
            Random random = new Random(System.currentTimeMillis());
            random.nextBytes(bytes);
            Files.write(local, bytes);

            Path remoteDir = Paths.get(mountPoint, "VAVI_FUSE_TEST4");
            if (!Files.exists(remoteDir)) {
Debug.println("[mkdir] " + remoteDir);
                assertEquals(0, exec("/bin/mkdir", remoteDir.toString()));
            }
Debug.println("[_rm] " + remoteDir + "/*");
            Files.list(remoteDir).forEach(p -> {
                try {
Debug.println("{_rm} " + p);
                    Files.delete(p);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
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
            localTmp = Files.createTempFile(localTmpDir, "vavifuse-2-", ".tmp");
            if (Files.exists(localTmp)) {
Debug.println("[_rm] " + localTmp);
                Files.delete(localTmp);
            }

Debug.println("[cp] " + remote + " " + localTmp);
            assertEquals(0, exec("/bin/cp", remote.toString(), localTmp.toString()));
            assertEquals(0, exec("/bin/ls", "-l", remoteDir.toString()));
            assertEquals(0, exec("/bin/ls", "-l", localTmp.toString()));
            assertTrue(Files.exists(localTmp));
            assertEquals(Files.size(remote), Files.size(localTmp));
            assertEquals(Checksum.getChecksum(remote), Checksum.getChecksum(localTmp));

            // clean up
Debug.println("[rm] " + remote);
            assertEquals(0, exec("/bin/rm", remote.toString()));
Debug.println("[_rm] " + remoteDir + "/*");
            Files.list(remoteDir).forEach(p -> {
                try {
Debug.println("{_rm} " + p);
                    Files.delete(p);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
Debug.println("[rmdir] " + remoteDir);
            assertEquals(0, exec("/bin/rmdir", remoteDir.toString()));
            assertFalse(Files.exists(remote));
            assertFalse(Files.exists(remoteDir));
        } finally {
            if (localTmp != null && Files.exists(localTmp)) {
Debug.println("[_rm] " + localTmp);
                Files.delete(localTmp);
            }
            if (local != null && Files.exists(local)) {
Debug.println("[_rm] " + local);
                Files.delete(local);
            }
        }
    }
}

/* */
