/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.yetiz.lib.acd.ACD;
import org.yetiz.lib.acd.ACDSession;
import org.yetiz.lib.acd.Entity.FolderInfo;
import org.yetiz.lib.acd.Entity.NodeInfo;
import org.yetiz.lib.acd.api.v1.Nodes;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Cache;
import vavi.nio.file.Util;
import vavi.util.Debug;

import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;


/**
 * AcdFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class AcdFileSystemDriver extends ExtendedFileSystemDriverBase {

    private final ACD drive;
    private ACDSession session;

    private boolean ignoreAppleDouble = false;

    @SuppressWarnings("unchecked")
    public AcdFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final ACD drive,
            final ACDSession session,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.drive = drive;
        this.session = session;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
    }

    /** */
    private Cache<NodeInfo> cache = new Cache<NodeInfo>() {
        /**
         * TODO when the parent is not cached
         * @see #ignoreAppleDouble
         * @throws NoSuchFileException must be thrown when the path is not found in this cache
         */
        public NodeInfo getEntry(Path path) throws IOException {
            if (cache.containsFile(path)) {
                return cache.getFile(path);
            } else {
                if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                    throw new NoSuchFileException("ignore apple double file: " + path);
                }

                if (path.getNameCount() == 0) {
                    return Nodes.getRootFolder(session);
                } else {
                    NodeInfo entry = Nodes.getFileMetadata(session, toPathString(path)); // TODO
//System.err.println("GOT: path: " + path + ", id: " + entry.getId());
                    if (entry == null) {
                        // cache
                        if (cache.containsFile(path)) {
                            cache.removeEntry(path);
                        }
                        throw new NoSuchFileException(path.toString());
                    }
                    cache.putFile(path, entry);
                    return entry;
                }
            }
        }
    };

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final NodeInfo entry = cache.getEntry(path);

        if (entry.isFolder()) {
            throw new IsDirectoryException(path.toString());
        }

        return drive.getFile(entry.getId());
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final NodeInfo entry;
        try {
            entry = cache.getEntry(path);

            if (entry.isFolder()) {
                throw new IsDirectoryException(path.toString());
            } else {
                throw new FileAlreadyExistsException(path.toString());
            }
        } catch (IOException e) {
Debug.println("newOutputStream: " + e.getMessage());
        }

        java.io.File temp = java.io.File.createTempFile("vavi-apps-fuse-", ".upload");

        return new AcdOutputStream(drive, temp, toFilenameString(path), FolderInfo.class.cast(cache.getEntry(path.getParent())), file -> {
            try {
System.out.println("file: " + file.getName() + ", " + file.getCreationDate() + ", " + file.getContentProperties().getSize());
                cache.addEntry(path, file);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir), filter);
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        NodeInfo parentEntry = cache.getEntry(dir.getParent());

        // TODO: how to diagnose?
        NodeInfo newEntry = drive.createFolder(parentEntry.getId(), toFilenameString(dir));

        cache.addEntry(dir, newEntry);
    }

    @Override
    public void delete(final Path path) throws IOException {
        removeEntry(path);
    }

    // TODO there is no api???
    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        if (cache.existsEntry(target)) {
            if (options != null && options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                removeEntry(target);
            } else {
                throw new FileAlreadyExistsException(target.toString());
            }
        }
        copyEntry(source, target);
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        if (cache.existsEntry(target)) {
            if (cache.getEntry(target).isFolder()) {
                if (options != null && options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                    // replace the target
                    if (cache.getChildCount(target) > 0) {
                        throw new DirectoryNotEmptyException(target.toString());
                    } else {
                        removeEntry(target);
                        moveEntry(source, target, false);
                    }
                } else {
                    // move into the target
                    // TODO SPEC is FileAlreadyExistsException ?
                    moveEntry(source, target, true);
                }
            } else {
                if (options != null && options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
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
    protected void checkAccessImpl(final Path path, final AccessMode... modes) throws IOException {
        final NodeInfo entry = cache.getEntry(path);

        if (!entry.isFile()) {
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
        drive.destroy();
    }

    /**
     * @throws IOException if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    protected Object getPathMetadataImpl(final Path path) throws IOException {
        return cache.getEntry(path);
    }

    /** */
    private List<Path> getDirectoryEntries(final Path dir) throws IOException {
        final NodeInfo entry = cache.getEntry(dir);

        if (!entry.isFolder()) {
            throw new NotDirectoryException("dir: " + dir);
        }

        List<Path> list = null;
        if (cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
            final List<NodeInfo> children = drive.getList(entry.getId());
            list = new ArrayList<>(children.size());

            // TODO nextPageToken
            for (final NodeInfo child : children) {
                Path childPath = dir.resolve(child.getName());
                list.add(childPath);
                cache.putFile(childPath, child);
            }

            cache.putFolder(dir, list);
        }

        return list;
    }

    /** */
    private void removeEntry(Path path) throws IOException {
        NodeInfo entry = cache.getEntry(path);
        if (entry.isFolder()) {
            if (drive.getList(entry.getId()).size() > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }

            drive.removeFolder(entry.getId());
        } else {
            drive.removeFile(entry.getId());
        }

        cache.removeEntry(path);
    }

    /** */
    private void copyEntry(final Path source, final Path target) throws IOException {
        NodeInfo sourceEntry = cache.getEntry(source);
        NodeInfo targetParentEntry = cache.getEntry(target.getParent());
        if (sourceEntry.isFile()) {
            NodeInfo newEntry = null; // TODO
            cache.addEntry(target, newEntry);
        } else if (sourceEntry.isFolder()) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /**
     * @param targetIsParent if the target is folder
     */
    private void moveEntry(final Path source, final Path target, boolean targetIsParent) throws IOException {
        NodeInfo sourceEntry = cache.getEntry(source);
        if (sourceEntry.isFile()) {
            Path actualPath = targetIsParent ? target.resolve(toFilenameString(source)) : target;
            NodeInfo patchedEntry = drive.renameFile(sourceEntry.getId(), toPathString(actualPath)); // TODO
            cache.removeEntry(source);
            if (targetIsParent) {
                cache.addEntry(target.resolve(source.getFileName()), patchedEntry);
            } else {
                cache.addEntry(target, patchedEntry);
            }
        } else if (sourceEntry.isFolder()) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException {
        NodeInfo sourceEntry = cache.getEntry(source);
//Debug.println(sourceEntry.id + ", " + sourceEntry.name);

        NodeInfo patchedEntry = drive.renameFile(sourceEntry.getId(), toFilenameString(target)); // TODO

        cache.removeEntry(source);
        cache.addEntry(target, patchedEntry);
    }
}
