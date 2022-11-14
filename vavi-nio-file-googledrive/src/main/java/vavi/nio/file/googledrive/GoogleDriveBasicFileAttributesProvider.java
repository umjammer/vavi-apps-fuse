/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

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

import com.github.fge.filesystem.attributes.provider.BasicFileAttributesProvider;
import com.google.api.services.drive.model.File;

import vavi.nio.file.googledrive.GoogleDriveFileAttributesFactory.Metadata;


/**
 * {@link BasicFileAttributes} implementation for GoogleDrive
 *
 * <p>
 * Note: GoogleDrive has poor support for file times; as required by the {@link
 * BasicFileAttributes} contract, all methods returning a {@link FileTime} for
 * which there is no support will return Unix's epoch (that is, Jan 1st, 1970
 * at 00:00:00 GMT).
 * </p>
 */
public final class GoogleDriveBasicFileAttributesProvider extends BasicFileAttributesProvider implements PosixFileAttributes {

    private final GoogleDriveFileSystemDriver driver;
    private final File entry;

    public GoogleDriveBasicFileAttributesProvider(@Nonnull final Metadata entry) throws IOException {
        this.driver = Objects.requireNonNull(entry).driver;
        this.entry = Objects.requireNonNull(entry).file;
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
        return entry.getModifiedTime() != null ? FileTime.fromMillis(entry.getModifiedTime().getValue()) : FileTime.fromMillis(entry.getCreatedTime() != null ? entry.getCreatedTime().getValue() : 0);
    }

    /**
     * Tells whether the file is a regular file with opaque content.
     */
    @Override
    public boolean isRegularFile() {
        return !driver.isFolder(entry);
    }

    /**
     * Tells whether the file is a directory.
     */
    @Override
    public boolean isDirectory() {
        return driver.isFolder(entry);
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
        return entry.getSize() == null ? 0 : entry.getSize();
    }

    @Override
    public UserPrincipal owner() {
        return null;
    }

    @Override
    public GroupPrincipal group() {
        return null;
    }

    @Override
    public Set<PosixFilePermission> permissions() {
        return isDirectory() ? PosixFilePermissions.fromString("rwxr-xr-x") : PosixFilePermissions.fromString("rw-r--r--");
    }
}
