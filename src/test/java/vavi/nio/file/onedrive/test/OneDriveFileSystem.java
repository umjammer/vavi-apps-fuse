/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessMode;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import vavi.util.properties.annotation.PropsEntity;

import de.tuberlin.onedrivesdk.common.OneItem;


/**
 * OneDriveFileSystem. 
 *
 * @depends "file://${HOME}.vavifuse/onedrive/[email]"
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 * @see "https://account.live.com/developers/applications/index"
 */
@PropsEntity(url = "file://${user.home}/.vavifuse/onedrive.properties")
public class OneDriveFileSystem extends FileSystem {

    /** */
    private final OneDriveFileSystemProvider provider;
    /** */
    private final Path ofpath;
    /** */
    private boolean readOnly = false;

    /**
     * @throws IOException
     */
    public OneDriveFileSystem(OneDriveFileSystemProvider provider,
                              Path ofpath,
                              Map<String, ?> env) throws IOException {
        this.provider = provider;
        this.ofpath = ofpath;
        
        if (Files.notExists(ofpath)) {
            throw new FileSystemNotFoundException(ofpath.toString());
        }
        // sm and existence check
       ofpath.getFileSystem().provider().checkAccess(ofpath, AccessMode.READ);
        if (!Files.isWritable(ofpath)) {
            this.readOnly = true;       
        }
    }
    
    /* @see java.nio.file.FileSystem#provider() */
    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    /* @see java.nio.file.FileSystem#close() */
    @Override
    public void close() throws IOException {
    }

    /* @see java.nio.file.FileSystem#isOpen() */
    @Override
    public boolean isOpen() {
        return false;
    }

    /* @see java.nio.file.FileSystem#isReadOnly() */
    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    /* @see java.nio.file.FileSystem#getSeparator() */
    @Override
    public String getSeparator() {
        return File.pathSeparator;
    }

    /* @see java.nio.file.FileSystem#getRootDirectories() */
    @Override
    public Iterable<Path> getRootDirectories() {
        ArrayList<Path> paths = new ArrayList<>();
        paths.add(new OneDrivePath(this, new byte[]{ '/' }));
        return paths;
    }

    /* @see java.nio.file.FileSystem#getFileStores() */
    @Override
    public Iterable<FileStore> getFileStores() {
        ArrayList<FileStore> list = new ArrayList<>(1);
        list.add(new OneDriveFileStore(new OneDrivePath(this, new byte[]{ '/' })));
        return list;
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
        String path;
        if (more.length == 0) {
            path = first;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment: more) {
                if (segment.length() > 0) {
                    if (sb.length() > 0)
                        sb.append('/');
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return new OneDrivePath(this, path.getBytes());
    }

    private static final String GLOB_SYNTAX = "glob";
    private static final String REGEX_SYNTAX = "regex";

    /* @see java.nio.file.FileSystem#getPathMatcher(java.lang.String) */
    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        int pos = syntaxAndPattern.indexOf(':');
        if (pos <= 0 || pos == syntaxAndPattern.length()) {
            throw new IllegalArgumentException();
        }
        String syntax = syntaxAndPattern.substring(0, pos);
        String input = syntaxAndPattern.substring(pos + 1);
        String expr;
        if (syntax.equals(GLOB_SYNTAX)) {
            expr = input;
        } else {
            if (syntax.equals(REGEX_SYNTAX)) {
                expr = input;
            } else {
                throw new UnsupportedOperationException("Syntax '" + syntax +
                    "' not recognized");
            }
        }
        // return matcher
        final Pattern pattern = Pattern.compile(expr);
        return new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                return pattern.matcher(path.toString()).matches();
            }
        };
    }

    /* @see java.nio.file.FileSystem#getUserPrincipalLookupService() */
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    /* @see java.nio.file.FileSystem#newWatchService() */
    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * @param resolvedPath
     * @return
     */
    public OneDriveFileAttributes getFileAttributes(byte[] resolvedPath) {
        OneItem e = null;
        return new OneDriveFileAttributes(e);
    }

    /**
     * @param resolvedPath
     * @param lastModifiedTime
     * @param lastAccessTime
     * @param createTime
     */
    public void setTimes(byte[] resolvedPath, FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) {
    }
}
