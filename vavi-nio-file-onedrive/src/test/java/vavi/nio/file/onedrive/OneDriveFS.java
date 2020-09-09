/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftLocalAppCredential;
import vavi.net.fuse.Fuse;
import vavi.util.properties.annotation.PropsEntity;


/**
 * test onedrive on fuse.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
public class OneDriveFS {

    /**
     * @param args 0: mount point, 1: email
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: OneDrive <mountpoint> <email>");
            System.exit(1);
        }

        String email = args[1];

        URI uri = URI.create("onedrive1:///?id=" + email);

        OAuth2AppCredential appCredential = new MicrosoftLocalAppCredential();
        PropsEntity.Util.bind(appCredential);

        Map<String, Object> env = new HashMap<>();
        env.put(OneDriveFileSystemProvider.ENV_APP_CREDENTIAL, appCredential);
        env.put("ignoreAppleDouble", true);

        FileSystem fs = new OneDriveFileSystemProvider().newFileSystem(uri, env);

        Fuse.getFuse().mount(fs, args[0], Collections.EMPTY_MAP);
    }
}

/* */
