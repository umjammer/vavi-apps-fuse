/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.gathered;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
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
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;
import vavi.util.Debug;

import static vavi.nio.file.Util.toPathString;


/**
 * GatheredFsFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class GatheredFileSystemDriver extends UnixLikeFileSystemDriverBase {

    /** should be unaccessible from outer */
    private final Map<String, FileSystem> fileSystems;

    private NameMap nameMap;

    /**
     * @param env
     */
    public GatheredFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final Map<String, FileSystem> fileSystems,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.fileSystems = fileSystems;
        if (env.containsKey(GatheredFileSystemProvider.ENV_NAME_MAP)) {
            this.nameMap = NameMap.class.cast(env.get(GatheredFileSystemProvider.ENV_NAME_MAP));
        }
    }

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final Object entry = getPathMetadata(path);

        // TODO: metadata driver
        if (!Path.class.isInstance(entry) || Files.isDirectory(Path.class.cast(entry))) {
            throw new IsDirectoryException("path: " + path);
        }

        return Files.newInputStream(Path.class.cast(entry));
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        // TODO we can implement using Files
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
        if (options != null && (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND))) {
            return new Util.SeekableByteChannelForWriting(newOutputStream(path, options)) {
                @Override
                protected long getLeftOver() throws IOException {
                    long leftover = 0;
                    if (options.contains(StandardOpenOption.APPEND)) {
                        Object entry = getPathMetadata(path);
                        if (entry != null && Path.class.isInstance(entry) && Files.isRegularFile(Path.class.cast(entry))) {
                            leftover = Files.size(Path.class.cast(entry));
                        }
                    }
                    return leftover;
                }

                @Override
                public void close() throws IOException {
Debug.println("SeekableByteChannelForWriting::close");
                    if (written == 0) {
                        // TODO no mean
Debug.println("SeekableByteChannelForWriting::close: scpecial: " + path);
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
            Object entry = getPathMetadata(path);
            if (!Path.class.isInstance(entry) || Files.isDirectory(Path.class.cast(entry))) {
                throw new IsDirectoryException(path.toString());
            }
            return new Util.SeekableByteChannelForReading(newInputStream(path, null)) {
                @Override
                protected long getSize() throws IOException {
                    return Files.size(Path.class.cast(entry));
                }
            };
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        // TODO we can implement using Files
        throw new UnsupportedOperationException("createDirectory is not supported by the file system");
    }

    @Override
    public void delete(final Path path) throws IOException {
        // TODO we can implement using Files
        throw new UnsupportedOperationException("delete is not supported by the file system");
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        // TODO we can implement using Files
        throw new UnsupportedOperationException("copy is not supported by the file system");
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        // TODO we can implement using Files
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
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        // TODO currently check read only?
    }

    @Override
    public void close() throws IOException {
        // TODO: what to do here? gathered fs does not implement Closeable :(
    }

    /**
     * @throws IOException you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    public Object getPathMetadata(final Path path) throws IOException {
//Debug.println("path: " + path);
        if (path.getNameCount() < 2) {
            return getFileSystemOf(path);
        } else {
            return toLocalPath(path);
        }
    }

    /** */
    private List<Path> getDirectoryEntries(final Path dir) throws IOException {
        if (dir.getNameCount() == 0) {
            List<Path> list = new ArrayList<>(fileSystems.size());

            for (String id : fileSystems.keySet()) {
                Path childPath = dir.resolve(nameMap.encodeFsName(id));
                list.add(childPath);
            }

            return list;
        } else {
            return Files.list(toLocalPathForDir(dir)).collect(Collectors.toList());
        }
    }

    /** */
    private FileSystem getFileSystemOf(Path path) throws IOException {
//Debug.println("path: " + path);
        if (path.getNameCount() == 0) {
//Debug.println("fs: " + path.getFileSystem());
            return path.getFileSystem();
        } else {
            String first = nameMap.decodeFsName(path.getName(0).toString());
            if (!fileSystems.containsKey(first)) {
                throw new NoSuchFileException(path.toString());
            }
//Debug.println("first: " + fileSystems.get(first));
            return fileSystems.get(first);
        }
    }

    /** */
    private Path toLocalPathForDir(Path path) throws IOException {
        String subParhString = toPathString(path.subpath(1, path.getNameCount()));
//Debug.println("subParhString:" + subParhString);
        FileSystem fileSystem = getFileSystemOf(path);
        return fileSystem.getPath(subParhString);
    }

    /** */
    private Path toLocalPath(Path path) throws IOException {
        if (path.getNameCount() == 1) {
            return path;
        } else {
            return toLocalPathForDir(path);
        }
    }
}
