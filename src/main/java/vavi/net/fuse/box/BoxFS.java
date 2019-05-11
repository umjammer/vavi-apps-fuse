/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.box;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import vavi.net.auth.oauth2.Authenticator;
import vavi.net.auth.oauth2.box.BoxAuthenticator;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.util.FuseFilesystemAdapterAssumeImplemented;


/**
 * BoxFS.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.home}/.vavifuse/box.properties")
public class BoxFS extends FuseFilesystemAdapterAssumeImplemented {

    @Property(name = "box.clientId")
    private String clientId;
    @Property(name = "box.clientSecret")
    private transient String clientSecret;
    @Property(name = "box.redirectUrl")
    private String redirectUrl;

    /** */
    public BoxFS(String email) {
        try {
            PropsEntity.Util.bind(this);

            String url = null;
            Authenticator authenticator = new BoxAuthenticator(email, redirectUrl);
            String code = authenticator.get(url);

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> cache = new HashMap<>();

    @Override
    public int access(final String path, final int access) {
//Debug.println("path: " + path);
        if (cache.containsKey(path)) {
            return 0;
        } else {
            return -ErrorCodes.ENOENT();
        }
    }

    @Override
    public int create(final String path, final ModeWrapper mode, final FileInfoWrapper info) {
Debug.println("path: " + path);
//        if (getPath(path) != null) {
//            return -ErrorCodes.EEXIST();
//        }
//        final GoogleDrivePath parent = getParentPath(path);
//        if (parent instanceof OneDriveDirectory) {
//            ((OneFolder) parent).mkfile(getLastComponent(path));
//            return 0;
//        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int getattr(final String path, final StatWrapper stat) {
Debug.println("path: " + path);
        if (cache.containsKey(path)) {
            return 0;
        } else {
Debug.println("enoent: " + path);
            return -ErrorCodes.ENOENT();
        }
    }

    @Override
    public int mkdir(final String path, final ModeWrapper mode) {
Debug.println("path: " + path);
//        if (getPath(path) != null) {
//            return -ErrorCodes.EEXIST();
//        }
//        final GoogleDrivePath parent = getParentPath(path);
//        if (parent instanceof OneDriveDirectory) {
//            ((OneFolder) parent).mkdir(getLastComponent(path));
//            return 0;
//        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int open(final String path, final FileInfoWrapper info) {
Debug.println("path: " + path);
        if (cache.containsKey(path)) {
            return 0;
        } else {
            return -ErrorCodes.ENOENT();
        }
    }

    @Override
    public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final FileInfoWrapper info) {
Debug.println("path: " + path);
//        final GoogleDrivePath p = getPath(path);
//        if (p == null) {
//            return -ErrorCodes.ENOENT();
//        }
//        if (!(p instanceof OneItem)) {
//            return -ErrorCodes.EISDIR();
//        }
//        return ((OneItem) p).read(buffer, size, offset);
        return 0;
    }

    @Override
    public int readdir(final String path, final DirectoryFiller filler) {
Debug.println("path: " + path);
        return 0;
    }

    @Override
    public int rename(final String path, final String newName) {
Debug.println("path: " + path);
        return 0;
    }

    @Override
    public int rmdir(final String path) {
Debug.println("path: " + path);
        return 0;
    }

    @Override
    public int truncate(final String path, final long offset) {
Debug.println("path: " + path);
        return 0;
    }

    @Override
    public int unlink(final String path) {
Debug.println("path: " + path);
        return 0;
    }

    @Override
    public int write(final String path,
                     final ByteBuffer buf,
                     final long bufSize,
                     final long writeOffset,
                     final FileInfoWrapper wrapper) {
Debug.println("path: " + path);
        return 0;
    }
}
