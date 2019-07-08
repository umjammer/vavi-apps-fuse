/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import vavi.nio.file.onedrive3.OneDriveFileSystemProvider;

import co.paralleluniverse.javafs.JavaFS;


/**
 * OneDrive JavaFS. (v2.0 api, cyberduck engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/xx umjammer initial version <br>
 */
public class Main6 {

    public static void main(final String... args) throws IOException {
        String email = args[1];

        // Create the necessary elements to create a filesystem.
        // Note: the URI _must_ have a scheme of "onedrive", and
        // _must_ be hierarchical.
        URI uri = URI.create("onedrive:///?id=" + email);

        Map<String, Object> env = new HashMap<>();
        env.put("ignoreAppleDouble", true);

        FileSystem fs = new OneDriveFileSystemProvider().newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "onedrive_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);

        JavaFS.mount(fs, Paths.get(args[0]), false, false, options);
    }

    @Test
    void test01() throws Exception {
        String email = "vavivavi@live.jp";

        URI uri = URI.create("onedrive:///?id=" + email);

        try (FileSystem onedrivefs = new OneDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP)) {

            Path src = Paths.get("src/test/resources" , "Hello.java");
            Path dir = onedrivefs.getPath("/").resolve("ONEDRIVE_FS_TEST");
System.out.println("$ [list]: " + dir);
Files.list(dir.getParent()).forEach(System.out::println);

            if (Files.exists(dir)) {
System.out.println("$ [delete directory]: " + dir);
                Files.delete(dir);
Thread.sleep(300);
            }
System.out.println("$ [createDirectory]: " + dir);
            dir = Files.createDirectory(dir);
Thread.sleep(300); 
System.out.println("$ [list]: " + dir.getParent());
Files.list(dir.getParent()).forEach(System.out::println);

System.out.println("$ [copy (upload)]: " + src + " " + dir.resolve(src.getFileName().toString()));
            Path src2 = Files.copy(src, dir.resolve(src.getFileName().toString()));
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);

System.out.println("$ [copy (internal)]: " + src2 + " " + dir.resolve(src2.getFileName().toString() + "_C"));
            Path src3 = Files.copy(src2, dir.resolve(src2.getFileName().toString() + "_C"));
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);

System.out.println("$ [rename (internal)]:" + src3 + " " + dir.resolve(src2.getFileName().toString() + "_R"));
            Path src4 = Files.move(src3, dir.resolve(src2.getFileName().toString() + "_R"));
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);

System.out.println("$ [delete file]: " + src2);
            Files.delete(src2);
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);

System.out.println("$ [delete file]:" + src4);
            Files.delete(src4);
Thread.sleep(300);
System.out.println("$ [list]: " + dir);
Files.list(dir).forEach(System.out::println);

System.out.println("$ [delete directory]: " + dir);
            Files.delete(dir);
Thread.sleep(300);
Files.list(dir.getParent()).forEach(System.out::println);
        }
    }
}