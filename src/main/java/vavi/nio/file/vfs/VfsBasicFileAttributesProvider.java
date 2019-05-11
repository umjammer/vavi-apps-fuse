/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;

import com.github.fge.filesystem.attributes.provider.BasicFileAttributesProvider;


/**
 * {@link BasicFileAttributes} implementation for Commons VFS
 *
 * <p>
 * Note: VFS has poor support for file times; as required by the {@link
 * BasicFileAttributes} contract, all methods returning a {@link FileTime} for
 * which there is no support will return Unix's epoch (that is, Jan 1st, 1970
 * at 00:00:00 GMT).
 * </p>
 */
public final class VfsBasicFileAttributesProvider extends BasicFileAttributesProvider implements PosixFileAttributes {

    private final FileObject entry;

    public VfsBasicFileAttributesProvider(@Nonnull final FileObject entry) throws IOException {
        this.entry = Objects.requireNonNull(entry);
    }

    /**
     * Returns the time of last modification.
     * <p>
     * If the file system implementation does not support a time stamp
     * to indicate the time of last modification then this method returns an
     * implementation specific default value, typically a {@code FileTime}
     * representing the epoch (1970-01-01T00:00:00Z).
     *
     * @return a {@code FileTime} representing the time the file was last
     *         modified
     */
    @Override
    public FileTime lastModifiedTime() {
        try {
            return FileTime.fromMillis(entry.getContent().getLastModifiedTime());
        } catch (FileSystemException e) {
            System.err.println("error code: " + e.getCode());
            e.printStackTrace();
            return FileTime.fromMillis(0);
        }
    }

    /**
     * Tells whether the file is a regular file with opaque content.
     */
    @Override
    public boolean isRegularFile() {
        try {
            return entry.getType().equals(FileType.FILE);
        } catch (FileSystemException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tells whether the file is a directory.
     */
    @Override
    public boolean isDirectory() {
        try {
            return entry.getType().equals(FileType.FOLDER);
        } catch (FileSystemException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns the size of the file (in bytes). The size may differ from the
     * actual size on the file system due to compression, support for sparse
     * files, or other reasons. The size of files that are not {@link
     * #isRegularFile regular} files is implementation specific and
     * therefore unspecified.
     *
     * @return the file size, in bytes
     */
    @Override
    public long size() {
        try {
            return isDirectory() ? 0 : entry.getContent().getSize();
        } catch (FileSystemException e) {
            System.err.println("error code: " + e.getCode());
            e.printStackTrace();
            return 0;
        }
    }

    /* @see java.nio.file.attribute.PosixFileAttributes#owner() */
    @Override
    public UserPrincipal owner() {
        return null;
    }

    /* @see java.nio.file.attribute.PosixFileAttributes#group() */
    @Override
    public GroupPrincipal group() {
        return null;
    }

    /* @see java.nio.file.attribute.PosixFileAttributes#permissions() */
    @Override
    public Set<PosixFilePermission> permissions() {
        return isDirectory() ? PosixFilePermissions.fromString("rwxr-xr-x") : PosixFilePermissions.fromString("rw-r--r--");
    }
}
