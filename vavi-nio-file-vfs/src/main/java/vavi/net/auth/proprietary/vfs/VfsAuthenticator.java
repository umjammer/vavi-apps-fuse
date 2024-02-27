/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.proprietary.vfs;

import java.net.URI;

import org.apache.commons.vfs2.FileSystemOptions;

import vavi.net.auth.Authenticator;


/**
 * VfsAuthenticator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/02/15 umjammer initial version <br>
 */
public interface VfsAuthenticator extends Authenticator<VfsCredential, FileSystemOptions> {

    /** */
    VfsCredential getCredential(String alias, URI uri);

    /** factory */
    static VfsAuthenticator getAuthenticator(URI uri) {
        String scheme = uri.getScheme();
        return switch (scheme) {
            case "smb", "cifs" -> new SmbVfsAuthenticator();
            case "sftp" -> new SftpVfsAuthenticator();
            case "webdav4s" -> new WebdavVfsAuthenticator();
            default -> throw new IllegalArgumentException(scheme);
        };
    }
}

/* */
