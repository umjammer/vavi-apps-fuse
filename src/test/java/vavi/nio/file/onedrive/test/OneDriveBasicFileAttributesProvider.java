
package vavi.nio.file.onedrive.test;

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

import de.tuberlin.onedrivesdk.common.OneItem;
import de.tuberlin.onedrivesdk.file.OneFile;


/**
 * {@link BasicFileAttributes} implementation for DropBox
 *
 * <p>
 * Note: DropBox has poor support for file times; as required by the {@link
 * BasicFileAttributes} contract, all methods returning a {@link FileTime} for
 * which there is no support will return Unix's epoch (that is, Jan 1st, 1970
 * at 00:00:00 GMT).
 * </p>
 */
public final class OneDriveBasicFileAttributesProvider extends BasicFileAttributesProvider implements PosixFileAttributes {

    private final OneFile fileEntry;

    public OneDriveBasicFileAttributesProvider(@Nonnull final OneItem entry) throws IOException {
        fileEntry = Objects.requireNonNull(entry).isFile() ? OneFile.class.cast(entry) : null;
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
        return fileEntry == null ? UNIX_EPOCH : FileTime.fromMillis(fileEntry.getLastModifiedDateTime() * 1000);
    }

    /**
     * Tells whether the file is a regular file with opaque content.
     */
    @Override
    public boolean isRegularFile() {
        return fileEntry != null;
    }

    /**
     * Tells whether the file is a directory.
     */
    @Override
    public boolean isDirectory() {
        return fileEntry == null;
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
        return fileEntry == null ? 0L : fileEntry.getSize();
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
        return fileEntry == null ? PosixFilePermissions.fromString("rwxr-xr-x") : PosixFilePermissions.fromString("rw-r--r--");
    }
}
