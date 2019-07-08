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

import vavi.nio.file.onedrive4.OneDriveFileSystemProvider;

import static vavi.nio.file.Base.testAll;

import co.paralleluniverse.javafs.JavaFS;


/**
 * OneDrive JavaFS. (v2.0 api, ms graph api engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/xx umjammer initial version <br>
 */
public class Main5 {

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

       testAll(new OneDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP));
    }

    void test02() throws Exception {
        String email = "vavivavi@live.jp";

        URI uri = URI.create("onedrive:///?id=" + email);

        try (FileSystem onedrivefs = new OneDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP)) {

            Path src = Paths.get(System.getenv("HOME") , "Music/0/rc.wav");
            Path dst = onedrivefs.getPath("/").resolve("音楽").resolve(src.getFileName().toString());
            Files.copy(src, dst);
        }
    }
}