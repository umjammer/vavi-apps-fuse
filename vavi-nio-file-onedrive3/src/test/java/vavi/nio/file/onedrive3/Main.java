/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
import vavi.net.fuse.Fuse;

import static vavi.nio.file.Base.testAll;


/**
 * OneDrive JavaFS. (v2.0 graph api, cyberduck engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/xx umjammer initial version <br>
 */
public class Main {

    @Test
    void test01() throws Exception {
        String email = System.getenv("MICROSOFT3_TEST_ACCOUNT");

        URI uri = URI.create("onedrive:///?id=" + email);

        testAll(new OneDriveFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP));
    }
}