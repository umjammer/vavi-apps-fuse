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
import java.util.Random;

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
public abstract class Base {

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
                Files.createDirectories(localTmpDir);
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
Debug.println("{_rm} " + p);
                try {
                    assertEquals(0, exec("/bin/rm", p.toString()));
                } catch (Exception e) {
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
                assertEquals(0, exec("/bin/rm", localTmp.toString()));
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
                    assertEquals(0, exec("/bin/rm", p.toString()));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
Debug.println("[rmdir] " + remoteDir);
            assertEquals(0, exec("/bin/rmdir", remoteDir.toString()));
            assertFalse(Files.exists(remote));
            assertFalse(Files.exists(remoteDir));
        } finally {
            if (localTmp != null && Files.exists(localTmp)) {
                try {
Debug.println("[_rm] " + localTmp);
                    assertEquals(0, exec("/bin/rm", localTmp.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (local != null && Files.exists(local)) {
Debug.println("[_rm] " + local);
                assertEquals(0, exec("/bin/rm", local.toString()));
            }
        }
    }

    /** */
    public static void testLargeFile(FileSystem fs, String mountPoint, Map<String, Object> options) throws Exception {
        try (Fuse fuse = Fuse.getFuse()) {
            fuse.mount(fs, mountPoint, options);

            Path tmpDir = Paths.get("tmp");
            if (!Files.exists(tmpDir)) {
System.out.println("[mkdir -p] " + tmpDir);
                Files.createDirectories(tmpDir);
            }
            Path source = Files.createTempFile(tmpDir, "vavifuse-1-", ".tmp");
            byte[] bytes = new byte[50 * 1024 * 1024 + 12345];
            Random random = new Random(System.currentTimeMillis());
            random.nextBytes(bytes);
System.out.println("[create random file] " + bytes.length);
            Files.write(source, bytes);

            Path dir = Paths.get(mountPoint,"VAVIFUSE_FS_TEST_L");
            Path target = dir.resolve(source.getFileName());
            if (Files.exists(dir)) {
System.out.println("[rm -rf] " + dir);
                assertEquals(0, exec("/bin/rm", "-rf", dir.toString()));
            }

System.out.println("[mkdir] " + dir);
            assertEquals(0, exec("/bin/mkdir", dir.toString()));

System.out.println("[cp(1)] " + source + " " + target);
            assertEquals(0, exec("/bin/cp", source.toString(), target.toString()));
            assertTrue(Files.exists(target));
            assertEquals(Files.size(source), Files.size(target));
            assertEquals(Checksum.getChecksum(source), Checksum.getChecksum(target));

            Path source2 = source.getParent().resolve(source.getFileName().toString().replace("vavifuse-1-", "vavifuse-2-"));

System.out.println("[cp(2)] " + target + " " + source2);
            assertEquals(0, exec("/bin/cp", target.toString(), source2.toString()));
            assertTrue(Files.exists(target));
            assertEquals(Files.size(source2), Files.size(target));
            assertEquals(Checksum.getChecksum(source2), Checksum.getChecksum(target));

            Path target2 = target.getParent().resolve(target.getFileName().toString().replace("vavifuse-1-", "vavifuse-2-"));

System.out.println("[cp(3)] " + target + " " + target2);
            assertEquals(0, exec("/bin/cp", target.toString(), target2.toString()));
            assertTrue(Files.exists(target));
            assertEquals(Files.size(target), Files.size(target2));
            assertEquals(Checksum.getChecksum(target), Checksum.getChecksum(target2));

System.out.println("[rm] " + source2);
            assertEquals(0, exec("/bin/rm", source2.toString()));
System.out.println("[rm] " + target);
            assertEquals(0, exec("/bin/rm", target.toString()));
System.out.println("[rm] " + target2);
            assertEquals(0, exec("/bin/rm", target2.toString()));

System.out.println("[rm -rf] " + dir);
            assertEquals(0, exec("/bin/rm", "-rf", dir.toString()));

            if (Files.exists(source)) {
System.out.println("[rm] " + source);
                Files.delete(source);
            }
        }
    }
}

/* */
