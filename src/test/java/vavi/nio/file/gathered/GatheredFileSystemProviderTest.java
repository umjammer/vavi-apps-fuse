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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import vavi.net.auth.oauth2.BasicAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
import vavi.nio.file.onedrive4.OneDriveFileSystemProvider;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.paralleluniverse.javafs.JavaFS;


/**
 * GatheredFileSystemProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/23 umjammer initial version <br>
 */
class GatheredFileSystemProviderTest {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        GatheredFileSystemProviderTest app = new GatheredFileSystemProviderTest();

        URI uri = URI.create("gatheredfs:///");

        Map<String, FileSystem> fileSystems = new HashMap<>();
        Map<String, String> nameMap = new HashMap<>();
        Arrays.asList(
            "googledrive:umjammer@gmail.com",
            "onedrive:snaohide@hotmail.com",
            "onedrive:vavivavi@live.jp"
        ).forEach(id -> {
            try {
                fileSystems.put(id, app.getFileSystem(id));
                nameMap.put(id, id.replaceAll("[:@\\.]", "_"));
System.err.println("ADD: " + id + ", " + nameMap.get(id));
            } catch (IOException e) {
                System.err.println(e);
            }
        });

        Map<String, Object> env = new HashMap<>();
        env.put(GatheredFileSystemProvider.ENV_FILESYSTEMS, fileSystems);
        env.put(GatheredFileSystemProvider.ENV_NAME_MAP, nameMap);

        FileSystem fs = FileSystems.newFileSystem(uri, env, Thread.currentThread().getContextClassLoader());

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "gathered_fs" + "@" + System.currentTimeMillis());
        options.put("noappledouble", null);

        JavaFS.mount(fs, Paths.get(args[0]), true, false, options);
    }

    /** */
    private BasicAppCredential onedriveAppCredential = new MicrosoftGraphLocalAppCredential();

    /* */
    {
        try {
            PropsEntity.Util.bind(onedriveAppCredential);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

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
            env.put(OneDriveFileSystemProvider.ENV_APP_CREDENTIAL, onedriveAppCredential);
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
    void test() throws IOException {
        Map<String, FileSystem> fileSystems = new HashMap<>();
        Map<String, String> nameMap = new HashMap<>();
        Arrays.asList(
            "googledrive:umjammer@gmail.com",
            "onedrive:snaohide@hotmail.com",
            "onedrive:vavivavi@live.jp"
        ).forEach(id -> {
            try {
                fileSystems.put(id, getFileSystem(id));
                nameMap.put(id, id.replaceAll("[:@\\.]", "_"));
System.err.println("ADD: " + id + ", " + nameMap.get(id));
            } catch (IOException e) {
                System.err.println(e);
            }
        });

        URI uri = URI.create("gatheredfs:///");
        Map<String, Object> env = new HashMap<>();
        env.put(GatheredFileSystemProvider.ENV_FILESYSTEMS, fileSystems);
        env.put(GatheredFileSystemProvider.ENV_NAME_MAP, nameMap);
        FileSystem fs = FileSystems.newFileSystem(uri, env, Thread.currentThread().getContextClassLoader());

        Files.list(fs.getPath("/")).forEach(p -> {
            try {
//                Files.list(p).forEach(System.out::println);
                System.err.println(p.getFileName() + " " + Files.getLastModifiedTime(p));
                Files.list(p).forEach(f -> {
                    try {
                        System.err.println(f.getFileName() + " " + Files.getLastModifiedTime(f));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    void test01() throws IOException {
        Path path = Paths.get("/onedrive%3Avavivavi%40live.jp/sub1/sub2");
        assertEquals(3, path.getNameCount());
        assertEquals("onedrive%3Avavivavi%40live.jp", path.getName(0).toString());
        assertEquals("sub1/sub2", path.subpath(1, path.getNameCount()).toString());

        Path root = Paths.get("/");
        assertEquals(0, root.getNameCount());
    }
}

/* */
