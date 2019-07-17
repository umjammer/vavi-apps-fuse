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
import java.nio.file.StandardCopyOption;
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
    }

    /**
     * VFS might have cache?
     * @see #ignoreAppleDouble
     * @throws NoSuchFileException apple double file
     */
    private FileObject getEntry(Path path) throws IOException {
        try {
            String pathString = Util.toPathString(path);
            if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                throw new NoSuchFileException("ignore apple double file: " + path);
            }

            FileObject entry = manager.resolveFile(baseUrl + pathString, opts);
            return entry;
        } catch (org.apache.commons.vfs2.FileNotFoundException e) {
            throw (IOException) new NoSuchFileException(path.toString()).initCause(e);
        }
    }

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final FileObject entry = getEntry(path);

        if (entry.isFolder()) {
            throw new IsDirectoryException(path.toString());
        }

        return entry.getContent().getInputStream();
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final FileObject entry = getEntry(path);

        if (entry.exists()) {
            if (entry.isFolder()) {
                throw new IsDirectoryException(path.toString());
            } else {
                throw new FileAlreadyExistsException(path.toString());
            }
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
        if (dirEntry.exists()) { // TODO check necessity
            throw new FileAlreadyExistsException(dir.toString());
        }

        dirEntry.createFolder();
    }

    @Override
    public void delete(final Path path) throws IOException {
        removeEntry(path);
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        FileObject targetEntry = getEntry(target);
        if (targetEntry.exists()) {
            if (options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                removeEntry(target);
            } else {
                throw new FileAlreadyExistsException(target.toString());
            }
        }
        copyEntry(source, target);
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        FileObject targetEntry = getEntry(target);
        if (targetEntry.exists()) {
            if (targetEntry.isFolder()) {
                if (options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                    // replace the target
                    if (targetEntry.getChildren().length > 0) {
                        throw new DirectoryNotEmptyException(target.toString());
                    } else {
                        removeEntry(target);
                        moveEntry(source, target, false);
                    }
                } else {
                    // move into the target
                    moveEntry(source, target, true);
                }
            } else {
                if (options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                    removeEntry(target);
                    moveEntry(source, target, false);
                } else {
                    throw new FileAlreadyExistsException(target.toString());
                }
            }
        } else {
            if (source.getParent().equals(target.getParent())) {
                // rename
                renameEntry(source, target);
            } else {
                moveEntry(source, target, false);
            }
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
        final FileObject entry = getEntry(path);

        if (entry.isFolder()) {
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
        // TODO: what to do here? Commons VFS does not implement Closeable :(
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

    /** */
    private void removeEntry(Path path) throws IOException {
        final FileObject entry = getEntry(path);

        if (entry.getType().equals(FileType.FOLDER)) {
            final FileObject[] list = entry.getChildren();

            if (list.length > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        if (!entry.delete()) {
            throw new IOException("delete: " + path);
        }
    }

    /** */
    private void copyEntry(final Path source, final Path target) throws IOException {
        FileObject targetEntry = getEntry(target);
        FileObject sourceEntry = getEntry(source);

        if (sourceEntry.isFile()) {
            targetEntry.copyFrom(sourceEntry, Selectors.SELECT_ALL);
        } else if (sourceEntry.isFolder()) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException(source.toString());
        }
    }

    /**
     * @param targetIsParent if the target is folder
     */
    private void moveEntry(final Path source, final Path target, boolean targetIsParent) throws IOException {
        FileObject sourceEntry = getEntry(source);
        FileObject targetEntry = getEntry(targetIsParent ? target.getParent().resolve(Util.toFilenameString(source)): target);

        if (sourceEntry.isFile()) {
            sourceEntry.moveTo(targetEntry);
        } else if (sourceEntry.isFolder()) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException(source.toString());
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException {
        FileObject sourceEntry = getEntry(source);
        FileObject targetEntry = getEntry(target);

        if (sourceEntry.isFile()) {
            sourceEntry.moveTo(targetEntry);
        } else if (sourceEntry.isFolder()) {
            throw new IsDirectoryException(source.toString());
        }
    }
}
