/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.dropbox;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.fge.fs.dropbox.provider.DropBoxFileSystemProvider;

import vavi.net.auth.oauth2.BasicAppCredential;
import vavi.net.auth.oauth2.dropbox.DropBoxLocalAppCredential;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.nio.file.Base.testAll;

import co.paralleluniverse.javafs.JavaFS;


/**
 * Main. (java fs, dropbox)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/11 umjammer initial version <br>
 */
public class Main {

    /**
     * @param args 0: mount point, 1: email
     */
    public static void main(final String... args) throws IOException {
        String email = args[1];

        // Create the necessary elements to create a filesystem.
        // Note: the URI _must_ have a scheme of "dropbox", and
        // _must_ be hierarchical.
        URI uri = URI.create("dropbox:///?id=" + email);

        BasicAppCredential appCredential = new DropBoxLocalAppCredential();
        PropsEntity.Util.bind(appCredential);

        Map<String, Object> env = new HashMap<>();
        env.put(DropBoxFileSystemProvider.ENV_CREDENTIAL, appCredential);

        FileSystem fs = new DropBoxFileSystemProvider().newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "dropbox_fs" + "@" + System.currentTimeMillis());

        JavaFS.mount(fs, Paths.get(args[0]), false, true, options);
    }

    @Test
    void test01() throws Exception {
        String email = "umjammer@gmail.com";

        URI uri = URI.create("dropbox:///?id=" + email);

        BasicAppCredential appCredential = new DropBoxLocalAppCredential();
        PropsEntity.Util.bind(appCredential);

        Map<String, Object> env = new HashMap<>();
        env.put(DropBoxFileSystemProvider.ENV_CREDENTIAL, appCredential);

        testAll(new DropBoxFileSystemProvider().newFileSystem(uri, env));
    }
}