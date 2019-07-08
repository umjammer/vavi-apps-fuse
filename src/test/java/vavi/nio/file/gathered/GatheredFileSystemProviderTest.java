/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.gathered;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;


/**
 * GatheredFileSystemProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/23 umjammer initial version <br>
 */
class GatheredFileSystemProviderTest {

    private FileSystem getFileSystem(String id) throws IOException {
        String[] part1s = id.split(":");
        if (part1s.length < 2) {
            throw new IllegalArgumentException("bad 2nd path component: should be 'scheme:id' i.e. 'onedrive:foo@bar.com'");
        }
        String scheme = part1s[0];
        String idForScheme = part1s[1];

        URI uri = URI.create(scheme + ":///?id=" + idForScheme);
        Map<String, Object> env = new HashMap<>();
        switch (scheme) {
        case "onedrive":
            env.put("ignoreAppleDouble", true);
            break;
        case "googledrive":
            env.put("ignoreAppleDouble", true);
            break;
        case "vfs":
            break;
        case "box":
            break;
        case "dropbox":
            break;
        default:
            throw new IllegalArgumentException("unsupported scheme: " + scheme);
        }

        FileSystem fs = FileSystems.newFileSystem(uri, env);
        return fs;
    }

    @Test
    void test() {
        Map<String, FileSystem> fileSystems = new HashMap<>();
        Arrays.asList(
            "googledrive:umjammer@gmail.com",
            "onedrive:snaohide@hotmail.com",
            "onedrive:vavivavi@live.jp"
        ).forEach(id -> {
            try {
                fileSystems.put(id, getFileSystem(id));
System.err.println("ADD: " + id);
            } catch (IOException e) {
                System.err.println(e);
            }
        });
    }

}

/* */
