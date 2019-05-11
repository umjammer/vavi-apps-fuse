/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.Selectors;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;


/**
 * VfsFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class VfsFileSystemDriver extends UnixLikeFileSystemDriverBase {

    private final FileSystemManager manager;

    private final FileSystemOptions opts;

    private boolean ignoreAppleDouble = false;

    private String baseUrl;

    /**
     * @param env { "baseUrl": "smb://10.3.1.1/Temporary Share/", "ignoreAppleDouble": boolean }
     */
    @SuppressWarnings("unchecked")
    public VfsFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final FileSystemManager manager,
            final VfsFileSystemRepository.Options options,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.manager = manager;
        this.opts = options.getFileSystemOptions();
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
        baseUrl = options.buildBaseUrl((String) env.get("baseUrl"));
        manager.resolveFile(baseUrl + "/", opts);
    }

    /**
     * @see #ignoreAppleDouble
     */
    private FileObject getEntry(Path path) throws IOException {
        String pathString = Util.toPathString(path);
        if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
            throw new NoSuchFileException("ignore apple double file: " + path);
        }

        FileObject entry = manager.resolveFile(baseUrl + pathString, opts);
        return entry;
    }

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final FileObject entry = getEntry(path);

        if (entry.getType().equals(FileType.FOLDER))
            throw new IsDirectoryException("path: " + path);

        return entry.getContent().getInputStream();
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final FileObject entry = getEntry(path);

        if (entry.exists()) {
            if (entry.getType().equals(FileType.FOLDER))
                throw new IsDirectoryException("path: " + path);
            else
                throw new FileAlreadyExistsException("path: " + path);
        } else {
            entry.createFile();
        }

        return entry.getContent().getOutputStream();
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir));
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs) throws IOException {
        if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
            return new Util.SeekableByteChannelForWriting(newOutputStream(path, options)) {
                @Override
                protected long getLeftOver() throws IOException {
                    long leftover = 0;
                    if (options.contains(StandardOpenOption.APPEND)) {
                        FileObject entry = getEntry(path);
                        if (entry != null && entry.getContent().getSize() >= 0) {
                            leftover = entry.getContent().getSize();
                        }
                    }
                    return leftover;
                }
            };
        } else {
            FileObject entry = getEntry(path);
            if (entry.getType().equals(FileType.FOLDER)) {
                throw new NoSuchFileException(path.toString());
            }
            return new Util.SeekableByteChannelForReading(newInputStream(path, null)) {
                @Override
                protected long getSize() throws IOException {
                    return entry.getContent().getSize();
                }
            };
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        FileObject dirEntry = getEntry(dir);
        if (dirEntry.exists())
            throw new FileAlreadyExistsException("dir: " + dir);
        dirEntry.createFolder();
    }

    @Override
    public void delete(final Path path) throws IOException {
        final String pathString = Util.toPathString(path);
        final FileObject entry = getEntry(path);

        if (entry.getType().equals(FileType.FOLDER)) {
            final FileObject[] list = entry.getChildren();

            if (list.length > 0)
                throw new DirectoryNotEmptyException(pathString);
        }

        if (!entry.delete())
            throw new VfsIOException("delete: " + path);
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        FileObject targetEntry = getEntry(target);

        if (targetEntry.getType().equals(FileType.FOLDER)) {
            final FileObject[] list = targetEntry.getChildren();

            if (list.length > 0)
                throw new DirectoryNotEmptyException("path: " + target);
        }

        if (!targetEntry.delete())
            throw new VfsIOException("delete: " + target);
        targetEntry = getEntry(target.getParent());

        final FileObject sourceEntry = getEntry(source);

        if (!sourceEntry.getType().equals(FileType.FOLDER)) {
            targetEntry.copyFrom(sourceEntry, Selectors.SELECT_ALL);
        } else {
            throw new UnsupportedOperationException("source can not be a folder");
        }
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        final String targetString = Util.toPathString(source);
        FileObject targetEntry = getEntry(target);

        if (targetEntry.getType().equals(FileType.FOLDER)) {
            final FileObject[] list = targetEntry.getChildren();

            if (list.length > 0)
                throw new DirectoryNotEmptyException(targetString);
        }
        // TODO: unknown what happens when a move operation is performed
        // and the target already exists
        if (!targetEntry.delete())
            throw new VfsIOException("delete: " + target);
        targetEntry = getEntry(target.getParent());

        final FileObject sourceEntry = getEntry(source);

        // TODO: how to diagnose?
        if (!sourceEntry.getType().equals(FileType.FOLDER)) {
            sourceEntry.moveTo(targetEntry);
        } else {
            throw new UnsupportedOperationException("source can not be a folder");
        }
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
     * @throws IOException filesystem level error, or a plain I/O error
     *                     if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        try {
            final String pathString = Util.toPathString(path);
            final FileObject entry = getEntry(path);

            if (entry.getType().equals(FileType.FOLDER)) {
                return;
            }

            // TODO: assumed; not a file == directory
            for (final AccessMode mode : modes) {
                if (mode == AccessMode.EXECUTE) {
                    throw new AccessDeniedException(pathString);
                }
            }

        } catch (org.apache.commons.vfs2.FileNotFolderException e) {
            throw (IOException) new NoSuchFileException("path: " + path).initCause(e);
        }
    }

    @Override
    public void close() throws IOException {
        // TODO: what to do here? Commons VFS does not implement Closeable :(
    }

    /**
     * @throws IOException if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    public Object getPathMetadata(final Path path) throws IOException {
        try {
            return getEntry(path);
        } catch (org.apache.commons.vfs2.FileNotFolderException e) {
            throw (IOException) new NoSuchFileException("path: " + path).initCause(e);
        }
    }

    /** */
    private List<Path> getDirectoryEntries(final Path dir) throws IOException {
        final FileObject entry = getEntry(dir);

        if (!entry.getType().equals(FileType.FOLDER)) {
            throw new NotDirectoryException("dir: " + dir);
        }

        List<Path> list = null;
        final FileObject[] children = entry.getChildren();
        list = new ArrayList<>(children.length);

        // TODO nextPageToken
        for (final FileObject child : children) {
            Path childPath = dir.resolve(child.getName().toString());
            list.add(childPath);
//System.err.println("child: " + childPath.toRealPath().toString());
        }

        return list;
    }
}
