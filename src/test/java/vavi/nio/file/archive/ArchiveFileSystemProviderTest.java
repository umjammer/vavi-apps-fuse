/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.archive;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * ArchiveFileSystemProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/23 umjammer initial version <br>
 */
class ArchiveFileSystemProviderTest {

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
        URI uri = URI.create("archive:file:/Users/nsano/src/vavi/vavi-util-archive/tmp/ugca010c.lzh");
        FileSystem fs = FileSystems.newFileSystem(uri, Collections.EMPTY_MAP);
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String file = "/Users/nsano/Documents/Games/PC98/bacumed/88/ALPHO.D88";
        URI uri = URI.create("archive:file:" + file);
        FileSystem fs = FileSystems.newFileSystem(uri, Collections.EMPTY_MAP);
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }
}

/* */
