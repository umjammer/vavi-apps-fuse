/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive2;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;


/**
 * GoogleDriveFileSystem.
 *
 * @depends "file://${HOME}.vavifuse/googledrive/?"
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/05 umjammer initial version <br>
 */
public class GoogleDriveFileSystem extends FileSystem {

    /* @see java.nio.file.FileSystem#provider() */
    @Override
    public FileSystemProvider provider() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.FileSystem#close() */
    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub

    }

    /* @see java.nio.file.FileSystem#isOpen() */
    @Override
    public boolean isOpen() {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.FileSystem#isReadOnly() */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /* @see java.nio.file.FileSystem#getSeparator() */
    @Override
    public String getSeparator() {
        return java.io.File.pathSeparator;
    }

    /* @see java.nio.file.FileSystem#getRootDirectories() */
    @Override
    public Iterable<Path> getRootDirectories() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.FileSystem#getFileStores() */
    @Override
    public Iterable<FileStore> getFileStores() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.FileSystem#supportedFileAttributeViews() */
    @Override
    public Set<String> supportedFileAttributeViews() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.FileSystem#getPath(java.lang.String, java.lang.String[]) */
    @Override
    public Path getPath(String first, String... more) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.FileSystem#getPathMatcher(java.lang.String) */
    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.FileSystem#getUserPrincipalLookupService() */
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.FileSystem#newWatchService() */
    @Override
    public WatchService newWatchService() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }
}
