/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive2;

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
import java.util.Set;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.Drive;

import vavi.net.auth.oauth2.google.GoogleDriveLocalAuthenticator;

import static vavi.net.auth.oauth2.google.GoogleDriveLocalAuthenticator.HTTP_TRANSPORT;
import static vavi.net.auth.oauth2.google.GoogleDriveLocalAuthenticator.JSON_FACTORY;


/**
 * GoogleDriveFileSystemProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/05 umjammer initial version <br>
 */
public class GoogleDriveFileSystemProvider extends FileSystemProvider {

    /** Application name. */
    private static final String APPLICATION_NAME = "vavi-fuse";

    private Drive drive;

    /**
     * @throws IOException
     */
    public GoogleDriveFileSystemProvider(String email) throws IOException {
        GoogleDriveLocalAuthenticator authenticator = new GoogleDriveLocalAuthenticator();
        Credential credential = authenticator.authorize(email);
        drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
    }

    /* @see java.nio.file.spi.FileSystemProvider#getScheme() */
    @Override
    public String getScheme() {
        return "googledrive";
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
        // TODO Auto-generated method stub

    }

    /* @see java.nio.file.spi.FileSystemProvider#delete(java.nio.file.Path) */
    @Override
    public void delete(Path path) throws IOException {
        // TODO Auto-generated method stub

    }

    /* @see java.nio.file.spi.FileSystemProvider#copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[]) */
    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        // TODO Auto-generated method stub

    }

    /* @see java.nio.file.spi.FileSystemProvider#move(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[]) */
    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        // TODO Auto-generated method stub

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
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#checkAccess(java.nio.file.Path, java.nio.file.AccessMode[]) */
    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        // TODO Auto-generated method stub

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
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#readAttributes(java.nio.file.Path, java.lang.String, java.nio.file.LinkOption[]) */
    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.spi.FileSystemProvider#setAttribute(java.nio.file.Path, java.lang.String, java.lang.Object, java.nio.file.LinkOption[]) */
    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        // TODO Auto-generated method stub

    }
}

/* */
