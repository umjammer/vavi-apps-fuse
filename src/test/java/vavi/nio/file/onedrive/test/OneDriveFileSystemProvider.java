/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive.test;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import vavi.net.auth.oauth2.Authenticator;
import vavi.net.auth.oauth2.microsoft.OneDriveAuthenticator;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveFactory;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneDriveScope;
import de.tuberlin.onedrivesdk.folder.OneFolder;
import de.tuberlin.onedrivesdk.networking.OneDriveAuthenticationException;


/**
 * OneDriveFileSystemProvider. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/05 umjammer initial version <br>
 */
public class OneDriveFileSystemProvider extends FileSystemProvider {

    @Property(name = "onedrive.clientId")
    private String clientId;
    @Property(name = "onedrive.clientSecret")
    private transient String clientSecret;
    @Property(name = "onedrive.redirectUrl")
    private String redirectUrl;

    /** */
    private transient OneDriveSDK api;

    /** */
    private final java.io.File file;

    /** */
    public OneDriveFileSystemProvider(String email) throws IOException {
        
        file = new java.io.File(System.getProperty("user.home"), ".vavifuse/onedrive/" + email);
        
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
//            cache.put("/", OneItem.class.cast(folder));

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

    /* @see java.nio.file.spi.FileSystemProvider#getScheme() */
    @Override
    public String getScheme() {
        return "onedrive";
    }

    /* @see java.nio.file.spi.FileSystemProvider#newFileSystem(java.net.URI, java.util.Map) */
    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#getFileSystem(java.net.URI) */
    @Override
    public FileSystem getFileSystem(URI uri) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#getPath(java.net.URI) */
    @Override
    public Path getPath(URI uri) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#newByteChannel(java.nio.file.Path, java.util.Set, java.nio.file.attribute.FileAttribute[]) */
    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#newDirectoryStream(java.nio.file.Path, java.nio.file.DirectoryStream.Filter) */
    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#createDirectory(java.nio.file.Path, java.nio.file.attribute.FileAttribute[]) */
    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
Debug.println("path: " + dir);
//      if (getPath(path) != null) {
//          return -ErrorCodes.EEXIST();
//      }
//      final OneDrivePath parent = getParentPath(path);
//      if (parent instanceof OneDriveDirectory) {
//          ((OneFolder) parent).mkdir(getLastComponent(path));
//          return 0;
//      }
    }

    /* @see java.nio.file.spi.FileSystemProvider#delete(java.nio.file.Path) */
    @Override
    public void delete(Path path) throws IOException {
Debug.println("path: " + path);

    }

    /* @see java.nio.file.spi.FileSystemProvider#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[]) */
    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        // TODO Auto-generated method stub

    }

    /* @see java.nio.file.spi.FileSystemProvider#move(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[]) */
    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
Debug.println("path: " + source);
//      final OneDrivePath p = getPath(path);
//      if (p == null) {
//          return -ErrorCodes.ENOENT();
//      }
//      final OneDrivePath newParent = getParentPath(newName);
//      if (newParent == null) {
//          return -ErrorCodes.ENOENT();
//      }
//      if (!(newParent instanceof OneFolder)) {
//          return -ErrorCodes.ENOTDIR();
//      }
//      p.delete();
//      p.rename(newName.substring(newName.lastIndexOf("/")));
//      ((OneFolder) newParent).add(p);
    }

    /* @see java.nio.file.spi.FileSystemProvider#isSameFile(java.nio.file.Path, java.nio.file.Path) */
    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.spi.FileSystemProvider#isHidden(java.nio.file.Path) */
    @Override
    public boolean isHidden(Path path) throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.spi.FileSystemProvider#getFileStore(java.nio.file.Path) */
    @Override
    public FileStore getFileStore(Path path) throws IOException {
Debug.println("path: " + path);
        writeRefreshToken();
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#checkAccess(java.nio.file.Path, java.nio.file.AccessMode[]) */
    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
Debug.println("path: " + path);
    }

    /* @see java.nio.file.spi.FileSystemProvider#getFileAttributeView(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[]) */
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#readAttributes(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[]) */
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path,
                                                            Class<A> type,
                                                            LinkOption... options) throws IOException {
Debug.println("path: " + path);
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#readAttributes(java.nio.file.Path, java.lang.String, java.nio.file.LinkOption[]) */
    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#setAttribute(java.nio.file.Path, java.lang.String, java.lang.Object, java.nio.file.LinkOption[]) */
    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        // TODO Auto-generated method stub

    }

}

/* */
