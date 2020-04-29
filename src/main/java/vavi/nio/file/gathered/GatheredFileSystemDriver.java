/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.gathered;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import vavi.nio.file.Util;
import vavi.util.Debug;

import static vavi.nio.file.Util.toPathString;


/**
 * GatheredFsFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class GatheredFileSystemDriver extends UnixLikeFileSystemDriverBase {

    /** should be unaccessible from outer */
    private final Map<String, FileSystem> fileSystems;

    private BiMap<String, String> nameMap;

    /**
     * @param env 
     */
    @SuppressWarnings("unchecked")
    public GatheredFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final Map<String, FileSystem> fileSystems,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.fileSystems = fileSystems;
        if (env.containsKey(GatheredFileSystemProvider.ENV_NAME_MAP)) {
            this.nameMap = HashBiMap.create(Map.class.cast(env.get(GatheredFileSystemProvider.ENV_NAME_MAP)));
        }
    }

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        throw new UnsupportedOperationException("newInputStream is not supported by the file system");
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
        throw new UnsupportedOperationException("newByteChannel is not supported by the file system");
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
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
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
Debug.println("path: " + path);
        if (path.getNameCount() < 2) {
            return getFileSystemOf(path);
        } else {
            return toLocalPath(path);
        }
    }

    /** id -> display name */
    private String encodeFsName(String id) throws IOException {
        if (nameMap != null) {
            return nameMap.get(id);
        } else {
            return URLEncoder.encode(id, "utf-8");
        }
    }

    /** display name -> id */
    private String decodeFsName(String path) throws IOException {
        if (nameMap != null) {
            return nameMap.inverse().get(path);
        } else {
            return URLDecoder.decode(path, "utf-8");
        }
    }

    /** */
    private List<Path> getDirectoryEntries(final Path dir) throws IOException {
        if (dir.getNameCount() == 0) {
            List<Path> list = new ArrayList<>(fileSystems.size());

            for (String id : fileSystems.keySet()) {
                Path childPath = dir.resolve(encodeFsName(id));
                list.add(childPath);
            }

            return list;
        } else {
            return Files.list(toLocalPathForDir(dir)).collect(Collectors.toList());
        }
    }

    /** */
    FileSystem getFileSystemOf(Path path) throws IOException {
Debug.println("path: " + path);
        if (path.getNameCount() == 0) {
            return path.getFileSystem();
        } else {
            String first = decodeFsName(path.getName(0).toString());
Debug.println("first: " + first);
            if (!fileSystems.containsKey(first)) {
                throw new NoSuchFileException(path.toString());
            }
            return fileSystems.get(first);
        }
    }

    /** */
    private Path toLocalPathForDir(Path path) throws IOException {
        String subParhString = toPathString(path.subpath(1, path.getNameCount()));
Debug.println("subParhString:" + subParhString);
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
