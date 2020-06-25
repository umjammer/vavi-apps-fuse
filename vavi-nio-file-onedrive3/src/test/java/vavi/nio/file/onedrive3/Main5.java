/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import vavi.nio.file.Base;
import vavi.nio.file.Util;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * OneDrive. (v2.0 graph api, cyberduck engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/31 umjammer initial version <br>
 */
public class Main5 {

    @Test
    void test01() throws Exception {
        String email = System.getenv("TEST5_ACCOUNT");

        URI uri = URI.create("onedrive:///?id=" + email);

        Path src;
        Path dstDir;
        Path dst;
        String a, b;
        try (FileSystem onedrivefs = new OneDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP)) {

            src = Paths.get("src/test/resources/Hello.java");
            dstDir = onedrivefs.getPath("/").resolve("TEST_FUSE_5");
            dst = dstDir.resolve("テスト 001");

            if (Files.exists(dstDir)) {
                Base.removeTree(dstDir);
            }
System.out.println("$ mkdir " + dstDir);
            Files.createDirectory(dstDir);

System.out.println("$ cp " + src + " " + dst);
            Files.copy(src, dst);
System.out.println("$ ls " + dstDir);
Files.list(dstDir).forEach(System.out::println);
            a = Util.toFilenameString(Files.list(dstDir).findFirst().get());
        }

        try (FileSystem onedrivefs = new OneDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP)) {
            dstDir = onedrivefs.getPath("/").resolve("TEST_FUSE_5");
            dst = dstDir.resolve("テスト 001");

System.out.println("$ ls " + dstDir);
Files.list(dstDir).forEach(System.out::println);
            b = Util.toFilenameString(Files.list(dstDir).findFirst().get());

            assertTrue(Files.exists(dst));
Debug.println(a + ", " + b);
            assertEquals(a, b);

System.out.println("$ rm " + dst);
            Files.delete(dst);
System.out.println("$ rmdir " + dstDir);
            Files.delete(dstDir);
        }
    }
}