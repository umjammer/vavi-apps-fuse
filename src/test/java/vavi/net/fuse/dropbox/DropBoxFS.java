/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.dropbox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vavi.net.auth.oauth2.Authenticator;
import vavi.net.auth.oauth2.dropbox.DropBoxLocalAuthenticator;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterAssumeImplemented;


/**
 * DropBoxFS.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/02 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.home}/.vavifuse/dropbox.properties")
public class DropBoxFS extends FuseFilesystemAdapterAssumeImplemented {

    @Property(name = "dropbox.clientId")
    private String clientId;
    @Property(name = "dropbox.clientSecret")
    private transient String clientSecret;

    /** */
//    private transient OneDriveSDK api;

    /** */
    public DropBoxFS(String email) throws IOException {
//        try {
            PropsEntity.Util.bind(this);

//            api = OneDriveFactory.createOneDriveSDK(clientId,
//                                                    clientSecret,
//                                                    OneDriveScope.OFFLINE_ACCESS);
//            String url = api.getAuthenticationURL();
//            Authenticator authenticator = new DropBoxAuthenticator(email);
//            String code = authenticator.get(url);
//
//            api.authenticate(code);

//            OneFolder folder = api.getRootFolder();
//Debug.println("root: " + folder.getName());
//            cache.put("/", OneItem.class.cast(folder));

//            api.startSessionAutoRefresh();

//        } catch (OneDriveException e) {
//            throw new IllegalStateException(e);
//        }
    }

//    private Map<String, OneItem> cache = new HashMap<>();

    @Override
    public int access(final String path, final int access) {
//Debug.println("path: " + path);
//        if (cache.containsKey(path)) {
            return 0;
//        } else {
//            return -ErrorCodes.ENOENT();
//        }
    }

    @Override
    public int create(final String path, final ModeWrapper mode, final FileInfoWrapper info) {
Debug.println("path: " + path);
//        if (getPath(path) != null) {
//            return -ErrorCodes.EEXIST();
//        }
//        final OneDrivePath parent = getParentPath(path);
//        if (parent instanceof OneDriveDirectory) {
//            ((OneFolder) parent).mkfile(getLastComponent(path));
//            return 0;
//        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int getattr(final String path, final StatWrapper stat) {
Debug.println("path: " + path);
//        if (cache.containsKey(path)) {
//            OneItem one = cache.get(path);
//            if (one.isFolder()) {
//                stat.setMode(NodeType.DIRECTORY, true, true, true, true, false, true, true, false, true)
//                    .setAllTimesSec(one.getLastModifiedDateTime());
//            } else if (one.isFile()) {
//                stat.setMode(NodeType.FILE, true, true, false, true, false, false, true, false, false)
//                    .setAllTimesSec(one.getLastModifiedDateTime())
//                    .size(OneFile.class.cast(one).getSize());
//            }
            return 0;
//        } else {
//Debug.println("enoent: " + path);
//            return -ErrorCodes.ENOENT();
//        }
    }

    @Override
    public int mkdir(final String path, final ModeWrapper mode) {
Debug.println("path: " + path);
//        if (getPath(path) != null) {
//            return -ErrorCodes.EEXIST();
//        }
//        final OneDrivePath parent = getParentPath(path);
//        if (parent instanceof OneDriveDirectory) {
//            ((OneFolder) parent).mkdir(getLastComponent(path));
//            return 0;
//        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int open(final String path, final FileInfoWrapper info) {
Debug.println("path: " + path);
//        if (cache.containsKey(path)) {
            return 0;
//        } else {
//            return -ErrorCodes.ENOENT();
//        }
    }

    @Override
    public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final FileInfoWrapper info) {
Debug.println("path: " + path);
//        final OneDrivePath p = getPath(path);
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
//        try {
Debug.println("path: " + path);
//            OneItem folder = cache.get(path);
//            List<OneItem> items = OneFolder.class.cast(folder).getChildren();
//Debug.println("folder: " + path + ": " + items.size());
//            for (OneItem item : items) {
//                filler.add(item.getName());
//
//                String key = path + (path.endsWith("/") ? "" : "/") + item.getName();
//Debug.println("cache: " + key);
//                cache.put(key, item);
//            }
            return 0;
//        } catch (OneDriveException | IOException e) {
//            e.printStackTrace(System.err);
//            return -ErrorCodes.EACCES();
//        }
    }

    @Override
    public int rename(final String path, final String newName) {
Debug.println("path: " + path);
//        final OneDrivePath p = getPath(path);
//        if (p == null) {
//            return -ErrorCodes.ENOENT();
//        }
//        final OneDrivePath newParent = getParentPath(newName);
//        if (newParent == null) {
//            return -ErrorCodes.ENOENT();
//        }
//        if (!(newParent instanceof OneFolder)) {
//            return -ErrorCodes.ENOTDIR();
//        }
//        p.delete();
//        p.rename(newName.substring(newName.lastIndexOf("/")));
//        ((OneFolder) newParent).add(p);
        return 0;
    }

    @Override
    public int rmdir(final String path) {
Debug.println("path: " + path);
//        final OneDrivePath p = getPath(path);
//        if (p == null) {
//            return -ErrorCodes.ENOENT();
//        }
//        if (!(p instanceof OneFolder)) {
//            return -ErrorCodes.ENOTDIR();
//        }
//        p.delete();
        return 0;
    }

    @Override
    public int truncate(final String path, final long offset) {
Debug.println("path: " + path);
//        final OneDrivePath p = getPath(path);
//        if (p == null) {
//            return -ErrorCodes.ENOENT();
//        }
//        if (!(p instanceof OneItem)) {
//            return -ErrorCodes.EISDIR();
//        }
//        ((OneItem) p).truncate(offset);
        return 0;
    }

    @Override
    public int unlink(final String path) {
Debug.println("path: " + path);
//        final OneDrivePath p = getPath(path);
//        if (p == null) {
//            return -ErrorCodes.ENOENT();
//        }
//        p.delete();
        return 0;
    }

    @Override
    public int write(final String path,
                     final ByteBuffer buf,
                     final long bufSize,
                     final long writeOffset,
                     final FileInfoWrapper wrapper) {
Debug.println("path: " + path);
//        final OneDrivePath p = getPath(path);
//        if (p == null) {
//            return -ErrorCodes.ENOENT();
//        }
//        if (!(p instanceof OneItem)) {
//            return -ErrorCodes.EISDIR();
//        }
//        return ((OneItem) p).write(buf, bufSize, writeOffset);
        return 0;
    }
}
