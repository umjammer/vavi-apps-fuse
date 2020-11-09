/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.archive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
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

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;
import vavi.util.archive.Archive;
import vavi.util.archive.Entry;


/**
 * ArchiveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class ArchiveFileSystemDriver extends ExtendedFileSystemDriverBase {

    private final Archive archive;

    /**
     * @param env
     */
    public ArchiveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final Archive archive,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.archive = archive;
    }

    /** */
    private Entry<?> getEntry(Path path) {
        return archive.getEntry(path.toAbsolutePath().toString().substring(1));
    }

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        return archive.getInputStream(getEntry(path));
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        throw new UnsupportedOperationException("newOutputStream is not supported by the file system");
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
                        Entry<?> entry = getEntry(path);
                        if (entry != null && entry.getSize() >= 0) {
                            leftover = entry.getSize();
                        }
                    }
                    return leftover;
                }
            };
        } else {
            Entry<?> entry = getEntry(path);
            if (entry.isDirectory()) {
                throw new NoSuchFileException(path.toString());
            }
            return new Util.SeekableByteChannelForReading(newInputStream(path, null)) {
                @Override
                protected long getSize() throws IOException {
                    return entry.getSize();
                }
            };
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("createDirectory is not supported by the file system");
    }

    @Override
    public void delete(final Path path) throws IOException {
        throw new UnsupportedOperationException("delete is not supported by the file system");
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        throw new UnsupportedOperationException("copy is not supported by the file system");
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        throw new UnsupportedOperationException("move is not supported by the file system");
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
    protected void checkAccessImpl(final Path path, final AccessMode... modes) throws IOException {
    }

    @Override
    public void close() throws IOException {
        archive.close();
    }

    /**
     * @return null when path is root
     * @throws IOException if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     */
    @Override
    protected Object getPathMetadataImpl(final Path path) throws IOException {
        return getEntry(path);
    }

    /** */
    private List<Path> getDirectoryEntries(final Path dir) throws IOException {
        List<Path> list = new ArrayList<>(archive.size());

        for (Entry<?> entry : archive.entries()) {
            Path childPath = dir.resolve(entry.getName());
            list.add(childPath);
        }

        return list;
    }
}
