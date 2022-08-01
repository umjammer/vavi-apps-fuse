/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.hfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
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

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase;
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
public final class HfsFileSystemDriver extends ExtendedFileSystemDriverBase {

    private final HFSCommonFileSystemHandler handler;

    /**
     * @param env extra parameter for this file system
     */
    public HfsFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final HFSCommonFileSystemHandler handler,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        setEnv(env);
        this.handler = handler;
    }

    /** */
    private boolean isFolder(FSEntry entry) {
        return entry instanceof FSFolder;
    }

    /** */
    private FSFolder asFolder(FSEntry entry) {
        return (FSFolder) entry;
    }

    /** */
    private FSEntry getEntry(Path path) throws IOException {
        if (ignoreAppleDouble && path.getFileName() != null && isAppleDouble(path)) {
            throw new NoSuchFileException("ignore apple double file: " + path);
        }

        if (path.getNameCount() == 0) {
            return handler.getRoot();
        } else {
//Debug.println(Arrays.toString(toPathString(path).replaceFirst("^/", "").split("/", -1)));
            FSEntry entry = handler.getEntry(toPathString(path).replaceFirst("^/", "").split("/", -1));
            if (entry != null) {
//Debug.println("entry: " + entry.getName() + ", " + isFolder(entry));
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

    @Override
    protected void checkAccessImpl(final Path path, final AccessMode... modes) throws IOException {
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

    @Nonnull
    @Override
    protected Object getPathMetadataImpl(final Path path) throws IOException {
        return getEntry(path);
    }

    @Override
    public void close() throws IOException {
        handler.close();
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
