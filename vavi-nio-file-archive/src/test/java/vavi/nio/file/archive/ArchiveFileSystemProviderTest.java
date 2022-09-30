/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.archive;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import vavi.util.Debug;
import vavi.util.archive.zip.JdkZipArchive;
import vavi.util.archive.zip.JdkZipArchiveSpi;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * ArchiveFileSystemProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/23 umjammer initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class ArchiveFileSystemProviderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "test.archive")
    String file;

    @Property(name = "test.comic")
    String comic;

    @Property(name = "test.ms932")
    String ms932;

    @BeforeEach
    void setup() throws Exception {
        PropsEntity.Util.bind(this);
    }

    @Test
    void test() throws Exception {
        URI uri = URI.create("archive:file:/tmp/jar/exam.jar!/img/sample.png");
        assertEquals("archive", uri.getScheme());
System.err.println("-- " + uri);
System.err.println("host: " + uri.getHost());
System.err.println("path: " + uri.getPath());
System.err.println("query: " + uri.getQuery());
System.err.println("fragment: " + uri.getFragment());
System.err.println("authority: " + uri.getAuthority());
System.err.println("port: " + uri.getPort());
System.err.println("rawAuthority: " + uri.getRawAuthority());
System.err.println("rawFragment: " + uri.getRawFragment());
System.err.println("rawPath: " + uri.getRawPath());
System.err.println("rawSchemeSpecificPart: " + uri.getRawSchemeSpecificPart());
    }

    @Test
    void test2() throws Exception {
        URI uri = URI.create("archive:///?id=umjammer@gmail.com&foo=bar#buz");
System.err.println("-- " + uri);
System.err.println("host: " + uri.getHost());
System.err.println("path: " + uri.getPath());
System.err.println("query: " + uri.getQuery());
System.err.println("fragment: " + uri.getFragment());
System.err.println("rawAuthority: " + uri.getRawAuthority());
System.err.println("rawFragment: " + uri.getRawFragment());
        assertEquals("buz", uri.getFragment());
        assertEquals("id=umjammer@gmail.com&foo=bar", uri.getQuery());
    }

    @Test
    void test3() throws Exception {
        URL url = ArchiveFileSystemProviderTest.class.getResource("/test.lzh");
        URI uri = URI.create("archive:file:" + url.getPath());
        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
        Path root = fs.getRootDirectories().iterator().next();
        Files.list(root).forEach(System.err::println);
        assertEquals(7, Files.list(root).count());
    }

    @Test
    void test4() throws Exception {
Debug.println(comic);
        Path path = Paths.get(comic);
Debug.println(path + ", " + Files.exists(path));
Debug.println("archive:" + path.toUri());
        URI uri = URI.create("archive:" + path.toUri());
        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
        Files.walk(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }

    @Test
    @DisplayName("env, failsafe encoding not set")
    void test51() throws Exception {
        Path path = Paths.get("src/test/resources/ms932.zip");
Debug.println(path + ", " + Files.exists(path));
        URI uri = URI.create("archive:" + path.toUri());
        IOException e = assertThrows(IOException.class, () -> {
            FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
            Files.walk(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
        });
Debug.println("exception cause: " + e.getMessage());
        assertInstanceOf(IllegalArgumentException.class, e.getCause());
        assertEquals("MALFORMED", e.getCause().getMessage());
    }

    @Test
    @DisplayName("env, failsafe encoding")
    void test52() throws Exception {
        System.setProperty(JdkZipArchive.ZIP_ENCODING, "utf-8");
        Path path = Paths.get("src/test/resources/ms932.zip");
//        Path path = Paths.get(ms932);
Debug.println(path + ", " + Files.exists(path));
        Map<String, Object> env = new HashMap<>();
        env.put(ArchiveFileSystemProvider.ENV_KEY_FAILSAFE_ENCODING, "ms932");
        URI uri = URI.create("archive:" + path.toUri());
        FileSystem fs = FileSystems.newFileSystem(uri, env);
        Files.walk(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }

    /**
     * @param args archive
     */
    public static void main(String[] args) throws Exception {
        ArchiveFileSystemProviderTest app = new ArchiveFileSystemProviderTest();
        PropsEntity.Util.bind(app);
        URI uri = URI.create("archive:file:" + app.file);
        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }
}

/* */
