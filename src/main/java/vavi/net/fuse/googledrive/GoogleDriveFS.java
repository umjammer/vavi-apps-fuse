/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.googledrive;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About.StorageQuota;
import com.google.api.services.drive.model.File;

import vavi.net.auth.oauth2.google.AuthorizationCodeInstalledApp;
import vavi.util.Debug;

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.StructStatvfs.StatvfsWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterAssumeImplemented;


/**
 * GoogleDriveFS.
 *
 * @depends "file://${HOME}.vavifuse/googledrive/?"
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
public class GoogleDriveFS extends FuseFilesystemAdapterAssumeImplemented {

    /** Application name. */
    private static final String APPLICATION_NAME = "vavi-apps-fuse";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".vavifuse/googledrive");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /**
     * Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.vavifuse/googledrive
     */
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE_METADATA_READONLY);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Drive drive;

    /**
     * @throws IOException
     */
    public GoogleDriveFS(String email) throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveFS.class.getResourceAsStream("/googledrive.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                .authorize(email);
Debug.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());

        drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Map<String, File> cache = new HashMap<>();

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
            File one = cache.get(path);
            stat.setMode(NodeType.DIRECTORY, true, true, true, true, false, true, true, false, true)
                .setAllTimesSec(one.getModifiedTime().getValue() / 1000);
            stat.setMode(NodeType.FILE, true, true, false, true, false, false, true, false, false)
                .setAllTimesSec(one.getModifiedTime().getValue() / 1000)
                .size(File.class.cast(one).getSize());
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
        try {
Debug.println("path: " + path);
            File folder = cache.get(path);
            return 0;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return -ErrorCodes.EACCES();
        }
    }

    @Override
    public int rename(final String path, final String newName) {
Debug.println("path: " + path);
//        final GoogleDrivePath p = getPath(path);
//        if (p == null) {
//            return -ErrorCodes.ENOENT();
//        }
//        final GoogleDrivePath newParent = getParentPath(newName);
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
//        final GoogleDrivePath p = getPath(path);
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
//        final GoogleDrivePath p = getPath(path);
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
//        final GoogleDrivePath p = getPath(path);
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
//        final GoogleDrivePath p = getPath(path);
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
        try {
            long block_size = 512;
            StorageQuota quota = drive.about().get().execute().getStorageQuota();
            long total = quota.getLimit() / block_size;
            long used = quota.getUsage() / block_size;
            long free = total - used;

            wrapper.bsize(block_size);
            wrapper.blocks(total);
            wrapper.frsize(block_size);
            wrapper.bfree(free);
            wrapper.bavail(free);

            return 0;
        } catch (Exception e) {
            return -ErrorCodes.EIO();
        }
    }
}
