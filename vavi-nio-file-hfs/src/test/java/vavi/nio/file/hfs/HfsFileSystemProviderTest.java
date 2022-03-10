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
import org.junit.jupiter.api.condition.EnabledIf;

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
    @EnabledIf("localPropertiesExists")
    @Disabled("doesn't work")
    void test() throws Exception {
        Path path = Paths.get("/Users/nsano/src/vavi/vavi-nio-file-apfs/src/test/resources/apfs.dmg");
Debug.println("file: " + path);
        URI uri = URI.create("hfs:file:" + path);
        FileSystem fs = new HfsFileSystemProvider().newFileSystem(uri, Collections.emptyMap());
        Files.list(fs.getRootDirectories().iterator().next()).forEach(System.err::println);
    }

    //----

    @Property(name = "test.dmg")
    String file;

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
