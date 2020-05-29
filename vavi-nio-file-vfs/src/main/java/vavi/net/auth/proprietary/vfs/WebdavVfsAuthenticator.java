/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.proprietary.vfs;

import java.io.IOException;
import java.net.URI;
import java.util.NoSuchElementException;

import org.apache.commons.vfs2.FileSystemOptions;

import vavi.nio.file.vfs.VfsFileSystemProvider;
import vavi.util.Debug;


/**
 * WebdavVfsAuthenticator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/02 umjammer initial version <br>
 */
public class WebdavVfsAuthenticator implements VfsAuthenticator {

    @Override
    public VfsCredential getCredential(String alias, URI uri) {
        VfsCredential credential;
        if (alias != null) {
            credential = new VfsCredential(alias);
Debug.println("credential: by alias " + alias);
        } else {
            credential = new VfsCredential(uri);
            if (credential.getId() == null || credential.getId().isEmpty()) {
                throw new NoSuchElementException("uri should have a username or a param " + VfsFileSystemProvider.PARAM_ALIAS);
            }
Debug.println("credential: by uri");
        }

        return credential;
    }

    @Override
    public FileSystemOptions authorize(VfsCredential credential) throws IOException {
        FileSystemOptions options = new FileSystemOptions();
        return options;
    }
}

/* */
