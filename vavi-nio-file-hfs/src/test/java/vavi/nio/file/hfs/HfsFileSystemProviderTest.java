/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.hfs;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * HfsFileSystemProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/23 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.dir}/local.properties")
class HfsFileSystemProviderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Test
    void test3() throws Exception {
        URL url = HfsFileSystemProviderTest.class.getResource("/test.dmg");
Debug.println("file: " + url.getPath());
        URI uri = URI.create("hfs:file:" + url.getPath());
        FileSystem fs = new HfsFileSystemProvider().newFileSystem(uri, Collections.emptyMap());
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }

    @Test
    @Disabled("doesn't work")
    void test() throws Exception {
        Path path = Paths.get(file);
Debug.println("file: " + path);
        URI uri = URI.create("hfs:" + path.toUri());
Debug.println("uri: " + uri);
        FileSystem fs = new HfsFileSystemProvider().newFileSystem(uri, Collections.emptyMap());
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }

    @Test
    void test0() throws Exception {
        URI fileUri = Paths.get( "src/test/resources/test.dmg").toUri();
Debug.println("fileUri: " + fileUri);
        URI uri = URI.create("hfs:" + fileUri + "!/CarbonOrCocoa.app");
Debug.println("uri: " + uri);
        String[] rawSchemeSpecificParts = uri.getRawSchemeSpecificPart().split("!");
Debug.println("rawSchemeSpecificParts[0]: " + rawSchemeSpecificParts[0]);
        URI file = URI.create(rawSchemeSpecificParts[0]);
        if (!"file".equals(file.getScheme())) {
            // currently only support "file"
            throw new IllegalArgumentException(file.toString());
        }
        // TODO virtual relative directory from rawSchemeSpecificParts[1]

Debug.println("file: " + Paths.get(file).toAbsolutePath());
    }

    //----

    @Property(name = "test.dmg")
    String file = "src/test/resources/test.dmg";

    @BeforeEach
    void setup() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    /** */
    public static void main(String[] args) throws Exception {
        HfsFileSystemProviderTest app = new HfsFileSystemProviderTest();
        app.setup();
Debug.println("file: " + app.file);
        URI uri = URI.create("hfs:file:" + app.file);
        FileSystem fs = new HfsFileSystemProvider().newFileSystem(uri, Collections.emptyMap());
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }
}

/* */
