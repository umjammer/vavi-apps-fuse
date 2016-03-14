/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.onedrive;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import vavi.net.auth.oauth2.Authenticator;
import vavi.net.auth.oauth2.microsoft.OneDriveAuthenticator;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveFactory;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneDriveScope;
import de.tuberlin.onedrivesdk.common.OneItem;
import de.tuberlin.onedrivesdk.drive.DriveQuota;
import de.tuberlin.onedrivesdk.file.OneFile;
import de.tuberlin.onedrivesdk.folder.OneFolder;
import de.tuberlin.onedrivesdk.networking.OneDriveAuthenticationException;
import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.StructStatvfs.StatvfsWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterAssumeImplemented;


/**
 * OneDriveFS. (fuse-jna)
 *
 * @depends "file://${HOME}.vavifuse/onedrive/[email]"
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 * @see "https://account.live.com/developers/applications/index"
 */
@PropsEntity(url = "classpath:onedrive.properties")
public class OneDriveFS extends FuseFilesystemAdapterAssumeImplemented {

    @Property(name = "onedrive.clientId")
    private String clientId;
    @Property(name = "onedrive.clientSecret")
    private transient String clientSecret;
    @Property(name = "onedrive.redirectUrl")
    private String redirectUrl;

    /** */
    private transient OneDriveSDK api;

    /** */
    private final File file;

    /**
     * @param email
     */
    public OneDriveFS(String email) throws IOException {
        
        file = new File(System.getProperty("user.home"), ".vavifuse/onedrive/" + email);
        
        try {
            // TODO why not work?
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> writeRefreshToken()));
            
            PropsEntity.Util.bind(this, email);

            api = OneDriveFactory.createOneDriveSDK(clientId,
                                                    clientSecret,
                                                    redirectUrl,
                                                    OneDriveScope.OFFLINE_ACCESS);
            String url = api.getAuthenticationURL();

            String refreshToken = readRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                authenticateByBrowser(url, email);
            } else {
                try {
                    api.authenticateWithRefreshToken(refreshToken);
                } catch (OneDriveAuthenticationException e) {
Debug.println("refreshToken: timeout?");
                    authenticateByBrowser(url, email);
                }
            }

            OneFolder folder = api.getRootFolder();
Debug.println("root: " + folder.getName());
            cache.put("/", OneItem.class.cast(folder));

            api.startSessionAutoRefresh();

        } catch (OneDriveException e) {
            throw new IllegalStateException(e);
        }
    }
    
    /** */
    private void authenticateByBrowser(String url, String email) throws IOException, OneDriveException {
        Authenticator authenticator = new OneDriveAuthenticator(email, redirectUrl);
        String code = authenticator.get(url);

        api.authenticate(code);
    }

    /** */
    private void writeRefreshToken() {
        try {
Debug.println("here");
            String oldRefreshToken = readRefreshToken();
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            String refreshToken = api.getRefreshToken();
            if (!oldRefreshToken.equals(refreshToken)) {
                FileWriter writer = new FileWriter(file);
Debug.println("refreshToken: " + refreshToken);
                writer.write("onedrive.refreshToken=" + refreshToken);
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new IllegalStateException(e);
        }
    }

    /** */
    private String readRefreshToken() throws IOException {
        String refreshToken = null;
        if (file.exists()) {
            FileReader reader = new FileReader(file);
            Properties props = new Properties();
            props.load(reader);
            refreshToken = props.getProperty("onedrive.refreshToken");
            reader.close();
        }
        return refreshToken;
    }

    /** */
    private Map<String, OneItem> cache = new HashMap<>();
    
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
        if (cache.containsKey(path)) {
            OneItem one = cache.get(path);
            if (one.isFolder()) {
                stat.setMode(NodeType.DIRECTORY, true, true, true, true, false, true, true, false, true)
                    .setAllTimesSec(one.getLastModifiedDateTime());
            } else if (one.isFile()) {
                stat.setMode(NodeType.FILE, true, true, false, true, false, false, true, false, false)
                    .setAllTimesSec(one.getLastModifiedDateTime())
                    .size(OneFile.class.cast(one).getSize());
            }
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
        if (cache.containsKey(path)) {
            return 0; 
        } else {
            return -ErrorCodes.ENOENT();
        }
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
        try {
Debug.println("path: " + path);
            OneItem folder = cache.get(path);
            List<OneItem> items = OneFolder.class.cast(folder).getChildren();
Debug.println("folder: " + path + ": " + items.size());
            for (OneItem item : items) {
                filler.add(item.getName());

                String key = path + (path.endsWith("/") ? "" : "/") + item.getName();
Debug.println("cache: " + key);
                cache.put(key, item);
            }
            return 0;
        } catch (OneDriveException e) { // means OneDriveAuthenticationException
            // TODO relogin?
            return -ErrorCodes.EACCES();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return -ErrorCodes.EACCES();
        }
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

    @Override
    public int statfs(final String path, final StatvfsWrapper wrapper) {
Debug.println("path: " + path);
writeRefreshToken();
        try {
            DriveQuota quota = api.getDefaultDrive().getQuota();
//Debug.println("total: " + quota.getTotal());
//Debug.println("used: " + quota.getUsed());

            long blockSize = 512;

            long total = quota.getTotal() / blockSize;
            long used = quota.getUsed() / blockSize;
            long free = total - used;

            wrapper.bavail(total - free);
            wrapper.bfree(free);
            wrapper.blocks(total);
            wrapper.bsize(blockSize);
            wrapper.favail(-1);
            wrapper.ffree(-1);
            wrapper.files(-1);
            wrapper.frsize(1);

            return 0;
        } catch (OneDriveException | IOException e) {
            return -ErrorCodes.EIO();
        }
    }
}
