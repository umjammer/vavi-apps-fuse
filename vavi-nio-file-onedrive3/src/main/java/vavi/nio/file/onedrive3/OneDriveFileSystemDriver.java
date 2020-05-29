/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.nuxeo.onedrive.client.OneDriveAPI;
import org.nuxeo.onedrive.client.OneDriveCopyOperation;
import org.nuxeo.onedrive.client.OneDriveDrive;
import org.nuxeo.onedrive.client.OneDriveFile;
import org.nuxeo.onedrive.client.OneDriveFolder;
import org.nuxeo.onedrive.client.OneDriveItem;
import org.nuxeo.onedrive.client.OneDriveItem.ItemIdentifierType;
import org.nuxeo.onedrive.client.OneDriveLongRunningAction;
import org.nuxeo.onedrive.client.OneDrivePatchOperation;
import org.nuxeo.onedrive.client.OneDriveUploadSession;
import org.nuxeo.onedrive.client.facets.FileSystemInfoFacet;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Cache;
import vavi.nio.file.Util;
import vavi.util.Debug;

import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;


/**
 * OneDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class OneDriveFileSystemDriver extends UnixLikeFileSystemDriverBase {

    private final OneDriveAPI client;
    private final OneDriveDrive drive;
    private boolean ignoreAppleDouble = false;

    @SuppressWarnings("unchecked")
    public OneDriveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final OneDriveAPI client,
            final OneDriveDrive drive,
            final Map<String, ?> env) {
        super(fileStore, provider);
        this.client = client;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
        this.drive = drive;
    }

    /** */
    private Cache<OneDriveItem> cache = new Cache<OneDriveItem>() {
        /**
         * @see #ignoreAppleDouble
         * @throws NoSuchFileException must be thrown when the path is not found in this cache
         */
        public OneDriveItem getEntry(Path path) throws IOException {
            if (cache.containsFile(path)) {
                return cache.getFile(path);
            } else {
                if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                    throw new NoSuchFileException("ignore apple double file: " + path);
                }

//                String pathString = toPathString(path);
//Debug.println("path: " + pathString);
                OneDriveItem entry;
                if (path.getNameCount() == 0) {
                    entry = drive.getRoot();
                    cache.putFile(path, entry);
                    return entry;
                } else {
                    List<Path> siblings;
                    if (!cache.containsFolder(path.getParent())) {
                        siblings = getDirectoryEntries(path.getParent());
                    } else {
                        siblings = cache.getFolder(path.getParent());
                    }
                    Optional<Path> found = siblings.stream().filter(p -> p.getFileName().equals(path.getFileName())).findFirst();
                    if (found.isPresent()) {
                        return cache.getEntry(found.get());
                    } else {
                        throw new NoSuchFileException(path.toString());
                    }
                }
            }
        }
    };

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final OneDriveItem entry = cache.getEntry(path);

        // TODO: metadata driver
        if (entry.getMetadata().isFolder()) {
            throw new IsDirectoryException("path: " + path);
        }

        return OneDriveFile.class.cast(entry).download();
    }

    /* */
    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        OneDriveItem entry = null;
        try {
            entry = cache.getEntry(path);

            if (entry.getMetadata().isFolder()) {
                throw new IsDirectoryException("path: " + path);
            } else {
                throw new FileAlreadyExistsException("path: " + path);
            }
        } catch (NoSuchFileException e) {
Debug.println("newOutputStream: " + e.getMessage());
//new Exception("*** DUMMY ***").printStackTrace();
        }

        return new Util.OutputStreamForUploading() {
            @Override
            protected void upload(InputStream is) throws IOException {
                OneDriveFolder dirEntry = OneDriveFolder.class.cast(cache.getEntry(path.getParent()));
                OneDriveFile entry = new OneDriveFile(client, dirEntry, toFilenameString(path), ItemIdentifierType.Path);
                final OneDriveUploadSession uploadSession = entry.createUploadSession();
                OutputStream os = new OneDriveOutputStream(uploadSession, path, is.available(), newEntry -> {
                    try {
                        cache.addEntry(path, newEntry);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
                Util.transfer(is, os);
            }
        };
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
                        OneDriveItem entry = cache.getEntry(path);
                        if (entry != null && entry.getMetadata().getSize() >= 0) {
                            leftover = entry.getMetadata().getSize();
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
                        File file = new File(toPathString(path));
                        FileInputStream fis = new FileInputStream(file);
                        FileChannel fc = fis.getChannel();
                        fc.transferTo(0, file.length(), this);
                        fis.close();
                    }
                    super.close();
                }
            };
        } else {
            OneDriveItem entry = cache.getEntry(path);
            if (entry.getMetadata().isFolder()) {
                throw new IsDirectoryException(path.toString());
            }
            return new Util.SeekableByteChannelForReading(newInputStream(path, null)) {
                @Override
                protected long getSize() throws IOException {
                    return OneDriveFile.class.cast(entry).getMetadata().getSize();
                }
            };
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        OneDriveItem parentEntry = cache.getEntry(dir.getParent());

        // TODO: how to diagnose?
        OneDriveFolder.Metadata dirEntry = OneDriveFolder.class.cast(parentEntry).create(toFilenameString(dir));

        cache.addEntry(dir, OneDriveItem.class.cast(dirEntry.getResource()));
    }

    @Override
    public void delete(final Path path) throws IOException {
        removeEntry(path);
    }

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
            if (cache.getEntry(target).getMetadata().isFolder()) {
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
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        final OneDriveItem entry = cache.getEntry(path);

        if (!entry.getMetadata().isFile()) {
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
        // TODO: what to do here? OneDriveClient does not implement Closeable :(
    }

    /**
     * @throws IOException if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    public Object getPathMetadata(final Path path) throws IOException {
        return cache.getEntry(path);
    }

    /** */
    private List<Path> getDirectoryEntries(Path dir) throws IOException {
        final OneDriveItem entry = cache.getEntry(dir);

        if (!entry.getMetadata().isFolder()) {
            throw new NotDirectoryException(dir.toString());
        }

        List<Path> list = null;
        if (cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
            list = new ArrayList<>();

            for (final OneDriveItem.Metadata child : OneDriveFolder.class.cast(entry).getChildren()) {
                Path childPath = dir.resolve(child.getName());
                list.add(childPath);
//System.err.println("child: " + childPath.toRealPath().toString());

                cache.putFile(childPath, child.getResource());
            }

            cache.putFolder(dir, list);
        }

        return list;
    }

    /** */
    private OneDriveItem getEntry(Path path) throws IOException {
        final OneDriveItem parentEntry = cache.getEntry(path.getParent());

        Optional<OneDriveItem.Metadata> found = StreamSupport.stream(OneDriveFolder.class.cast(parentEntry).getChildren().spliterator(), false)
            .filter(child -> {
                try {
//System.out.println(child.getName() + ", " + toFilenameString(path));
                    return child.getName().equals(toFilenameString(path));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }).findFirst();
        if (found.isPresent()) {
            return found.get().getResource();
        } else {
            throw new NoSuchFileException(path.toString());
        }
    }

    /** */
    private void removeEntry(Path path) throws IOException {
        OneDriveItem entry = cache.getEntry(path);
        if (entry.getMetadata().isFolder()) {
            // TODO use cache
            final List<OneDriveItem> children = StreamSupport.stream(OneDriveFolder.class.cast(entry).getChildren().spliterator(), false)
                    .map(e -> e.getResource()).collect(Collectors.toList());

            if (children.size() > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        // TODO: unknown what happens when a move operation is performed
        // and the target already exists
        entry.delete();

        cache.removeEntry(path);
    }

    /** */
    private void copyEntry(final Path source, final Path target) throws IOException {
        OneDriveItem sourceEntry = cache.getEntry(source);
        OneDriveItem targetParentEntry = cache.getEntry(target.getParent());
        if (sourceEntry.getMetadata().isFile()) {
            OneDriveCopyOperation operation = new OneDriveCopyOperation();
            operation.rename(toFilenameString(target));
            operation.copy(OneDriveFolder.class.cast(targetParentEntry));
            OneDriveLongRunningAction action = OneDriveFile.class.cast(sourceEntry).copy(operation);
            action.await(statusObject -> {
                Debug.printf("Copy Progress Operation %s progress %f status %s",
                    statusObject.getOperation(),
                    statusObject.getPercentage(),
                    statusObject.getStatus());
            });
            cache.addEntry(target, getEntry(target));
        } else if (sourceEntry.getMetadata().isFolder()) {
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /**
     * @param targetIsParent if the target is folder
     */
    private void moveEntry(final Path source, final Path target, boolean targetIsParent) throws IOException {
        OneDriveItem sourceEntry = cache.getEntry(source);
        OneDriveItem targetParentEntry = cache.getEntry(targetIsParent ? target : target.getParent());
        if (sourceEntry.getMetadata().isFile()) {
            OneDrivePatchOperation operation = new OneDrivePatchOperation();
            operation.rename(targetIsParent ? toFilenameString(source) : toFilenameString(target));
            operation.move(OneDriveFolder.class.cast(targetParentEntry));
            final FileSystemInfoFacet info = new FileSystemInfoFacet();
            info.setLastModifiedDateTime(Instant.ofEpochMilli(sourceEntry.getMetadata().getLastModifiedDateTime().toEpochSecond()).atOffset(ZoneOffset.UTC));
            operation.facet("fileSystemInfo", info);
            OneDriveFile.class.cast(sourceEntry).patch(operation);
            cache.removeEntry(source);
            if (targetIsParent) {
                cache.addEntry(target.resolve(source.getFileName()), getEntry(target.resolve(source.getFileName())));
            } else {
                cache.addEntry(target, getEntry(target));
            }
        } else if (sourceEntry.getMetadata().isFolder()) {
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException {
        OneDriveItem sourceEntry = cache.getEntry(source);

        OneDrivePatchOperation operation = new OneDrivePatchOperation();
        operation.rename(toFilenameString(target));
        sourceEntry.patch(operation);
        cache.removeEntry(source);
        cache.addEntry(target, getEntry(target));
    }
}
