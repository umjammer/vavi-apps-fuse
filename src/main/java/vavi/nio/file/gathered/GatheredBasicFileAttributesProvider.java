/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.gathered;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.fge.filesystem.attributes.provider.BasicFileAttributesProvider;


/**
 * {@link BasicFileAttributes} implementation for Gathered FS
 */
public final class GatheredBasicFileAttributesProvider extends BasicFileAttributesProvider implements PosixFileAttributes {

    private Object entry;

    public GatheredBasicFileAttributesProvider(@Nonnull final Object entry) throws IOException {
        this.entry = entry;
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
            if (FileSystem.class.isInstance(entry)) {
                return FileTime.fromMillis(0);
            } else if (Path.class.isInstance(entry)) {
                return Files.getLastModifiedTime(Path.class.cast(entry));
            } else {
                throw new IllegalStateException("unsupported type: " + entry.getClass().getName());
            }
        } catch (IOException e) {
e.printStackTrace();
            return FileTime.fromMillis(0);
        }
    }

    /**
     * Tells whether the file is a regular file with opaque content.
     */
    @Override
    public boolean isRegularFile() {
        if (FileSystem.class.isInstance(entry)) {
            return false;
        } else if (Path.class.isInstance(entry)) {
            return Files.isRegularFile(Path.class.cast(entry));
        } else {
            throw new IllegalStateException("unsupported type: " + entry.getClass().getName());
        }
    }

    /**
     * Tells whether the file is a directory.
     */
    @Override
    public boolean isDirectory() {
        if (FileSystem.class.isInstance(entry)) {
            return true;
        } else if (Path.class.isInstance(entry)) {
            return Files.isRegularFile(Path.class.cast(entry));
        } else {
            throw new IllegalStateException("unsupported type: " + entry.getClass().getName());
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
            if (FileSystem.class.isInstance(entry)) {
                return FileSystem.class.cast(entry).getFileStores().iterator().next().getTotalSpace();
            } else if (Path.class.isInstance(entry)) {
                return Files.size(Path.class.cast(entry));
            } else {
                throw new IllegalStateException("unsupported type: " + entry.getClass().getName());
            }
        } catch (IOException e) {
e.printStackTrace();
            return 0;
        }
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
