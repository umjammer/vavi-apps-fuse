/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.gathered;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;

import static vavi.nio.file.Util.toPathString;


/**
 * GatheredFsFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/03/30 umjammer initial version <br>
 */
public final class GatheredFileSystemDriver extends ExtendedFileSystemDriverBase {

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
            this.nameMap = (NameMap) env.get(GatheredFileSystemProvider.ENV_NAME_MAP);
        }
    }

    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final Object entry = getPathMetadata(path);

        // TODO: metadata driver
        if (!(entry instanceof Path) || Files.isDirectory((Path) entry)) {
            throw new IsDirectoryException("path: " + path);
        }

        return Files.newInputStream((Path) entry);
    }

    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        // TODO we can implement using Files
        throw new UnsupportedOperationException("newOutputStream is not supported by the file system");
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir), filter);
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

    @Override
    protected void checkAccessImpl(final Path path, final AccessMode... modes) throws IOException {
        // TODO currently check read only?
    }

    /**
     * @throws IOException you should throw {@link NoSuchFileException} when the file not found.
     */
    @Override
    protected Object getPathMetadataImpl(final Path path) throws IOException {
//Debug.println("path: " + path);
        if (path.getNameCount() < 2) {
            return getFileSystemOf(path);
        } else {
            return toLocalPath(path);
        }
    }

    @Override
    public void close() throws IOException {
        // TODO: what to do here? gathered fs does not implement Closeable :(
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
        String subPathString = toPathString(path.subpath(1, path.getNameCount()));
//Debug.println("subPathString: " + subPathString);
        FileSystem fileSystem = getFileSystemOf(path);
        return fileSystem.getPath(subPathString);
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
