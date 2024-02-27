/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.gathered;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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

import vavi.util.Debug;


/**
 * {@link BasicFileAttributes} implementation for Gathered FS
 */
public final class GatheredBasicFileAttributesProvider extends BasicFileAttributesProvider implements PosixFileAttributes {

    private final Object entry;

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
            if (entry instanceof FileSystem fs) {
                if (fs.provider() instanceof GatheredFileSystemProvider) {
                    return FileTime.fromMillis(System.currentTimeMillis());
                } else {
                    return Files.getLastModifiedTime(fs.getRootDirectories().iterator().next());
                }
            } else if (entry instanceof Path path) {
                //System.err.println("@@@: " + entry + ", " + path.getFileSystem().provider());
                return Files.getLastModifiedTime(path);
            } else {
                throw new IllegalStateException("unsupported type: " + entry.getClass().getName());
            }
        } catch (NoSuchFileException e) {
if (!e.getMessage().contains("ignore apple double file")) {
 Debug.println(e);
}
            return UNIX_EPOCH;
        } catch (IOException e) {
e.printStackTrace();
            return UNIX_EPOCH;
        }
    }

    /**
     * Tells whether the file is a regular file with opaque content.
     */
    @Override
    public boolean isRegularFile() {
        if (entry instanceof FileSystem) {
            return false;
        } else if (entry instanceof Path path) {
            return Files.isRegularFile(path);
        } else {
            throw new IllegalStateException("unsupported type: " + entry.getClass().getName());
        }
    }

    /**
     * Tells whether the file is a directory.
     */
    @Override
    public boolean isDirectory() {
        if (entry instanceof FileSystem) {
            return true;
        } else if (entry instanceof Path path) {
            return Files.isDirectory(path);
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
            if (entry instanceof FileSystem) {
                return ((FileSystem) entry).getFileStores().iterator().next().getTotalSpace();
            } else if (entry instanceof Path) {
                return Files.size((Path) entry);
            } else {
                throw new IllegalStateException("unsupported type: " + entry.getClass().getName());
            }
        } catch (NoSuchFileException e) {
if (!e.getMessage().contains("ignore apple double file")) {
 Debug.println(e);
}
            return 0;
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
