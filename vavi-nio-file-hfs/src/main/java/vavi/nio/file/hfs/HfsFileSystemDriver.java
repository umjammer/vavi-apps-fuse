/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.hfs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.catacombae.storage.fs.FSEntry;
import org.catacombae.storage.fs.FSFolder;
import org.catacombae.storage.fs.FSForkType;
import org.catacombae.storage.fs.hfscommon.HFSCommonFileSystemHandler;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;

import static vavi.nio.file.Util.isAppleDouble;
import static vavi.nio.file.Util.toPathString;


/**
 * HfsFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class HfsFileSystemDriver extends UnixLikeFileSystemDriverBase {

    private final HFSCommonFileSystemHandler handler;

    private boolean ignoreAppleDouble = false;

    /**
     * @param env
     */
    @SuppressWarnings("unchecked")
    public HfsFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final HFSCommonFileSystemHandler handler,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.handler = handler;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
    }

    /** */
    private boolean isFolder(FSEntry entry) {
        return FSFolder.class.isInstance(entry);
    }

    /** */
    private FSFolder asFolder(FSEntry entry) {
        return FSFolder.class.cast(entry);
    }

    /** */
    private FSEntry getEntry(Path path) throws IOException {
        if (ignoreAppleDouble && path.getFileName() != null && isAppleDouble(path)) {
            throw new NoSuchFileException("ignore apple double file: " + path);
        }

        if (path.getNameCount() == 0) {
            return handler.getRoot();
        } else {
            FSEntry entry = handler.getEntry(toPathString(path));
            if (entry != null) {
                return entry;
            } else {
                throw new NoSuchFileException(path.toString());
            }
        }
    }

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final FSEntry entry = getEntry(path);

        if (isFolder(entry)) {
            throw new IsDirectoryException(path.toString());
        }

        return entry.getForkByType(FSForkType.DATA).getInputStream();
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        throw new UnsupportedOperationException("this file system is read only");
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir), filter);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs) throws IOException {
        if (options != null && Util.isWriting(options)) {
            return new Util.SeekableByteChannelForWriting(newOutputStream(path, options)) {
                @Override
                protected long getLeftOver() throws IOException {
                    long leftover = 0;
                    if (options.contains(StandardOpenOption.APPEND)) {
                        FSEntry entry = getEntry(path);
                        if (entry != null && entry.getForkByType(FSForkType.DATA).getLength() >= 0) {
                            leftover = entry.getForkByType(FSForkType.DATA).getLength();
                        }
                    }
                    return leftover;
                }

                @Override
                public void close() throws IOException {
System.out.println("SeekableByteChannelForWriting::close");
                    if (written == 0) {
                        // TODO no mean
System.out.println("SeekableByteChannelForWriting::close: scpecial: " + path);
                        java.io.File file = new java.io.File(toPathString(path));
                        FileInputStream fis = new FileInputStream(file);
                        FileChannel fc = fis.getChannel();
                        fc.transferTo(0, file.length(), this);
                        fis.close();
                    }
                    super.close();
                }
            };
        } else {
            FSEntry entry = getEntry(path);
            if (isFolder(entry)) {
                throw new IsDirectoryException(path.toString());
            }
            return new Util.SeekableByteChannelForReading(newInputStream(path, null)) {
                @Override
                protected long getSize() throws IOException {
                    return entry.getForkByType(FSForkType.DATA).getLength();
                }
            };
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("this file system is read only");
    }

    @Override
    public void delete(final Path path) throws IOException {
        throw new UnsupportedOperationException("this file system is read only");
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        throw new UnsupportedOperationException("this file system is read only");
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        throw new UnsupportedOperationException("this file system is read only");
    }

    /**
     * Check access modes for a path on this filesystem
     * <p>
     * If no modes are provided to check for, this simply checks for the
     * existence of the path.
     * </p>
     *
     * @param path the path to check
     * @param modes the modes to check for, if any
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        final FSEntry entry = getEntry(path);

        if (isFolder(entry)) {
            return;
        }

        // TODO: assumed; not a file == directory
        for (final AccessMode mode : modes) {
            if (mode == AccessMode.EXECUTE) {
                throw new AccessDeniedException(path.toString());
            }
        }
    }

    @Override
    public void close() throws IOException {
        handler.close();
    }

    /**
     * @throws IOException if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    public Object getPathMetadata(final Path path) throws IOException {
        return getEntry(path);
    }

    /** */
    private List<Path> getDirectoryEntries(final Path dir) throws IOException {
        final FSEntry dirEntry = getEntry(dir);

        if (!isFolder(dirEntry)) {
            throw new NotDirectoryException(dir.toString());
        }

        List<Path> list = new ArrayList<>();

        for (FSEntry entry : asFolder(dirEntry).listEntries()) {
            Path childPath = dir.resolve(entry.getName());
            list.add(childPath);
        }

        return list;
    }
}
