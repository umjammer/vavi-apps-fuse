/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.Files;
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
import java.time.ZonedDateTime;
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
import org.nuxeo.onedrive.client.OneDriveExpand;
import org.nuxeo.onedrive.client.OneDriveFile;
import org.nuxeo.onedrive.client.OneDriveFolder;
import org.nuxeo.onedrive.client.OneDriveItem;
import org.nuxeo.onedrive.client.OneDriveItem.ItemIdentifierType;
import org.nuxeo.onedrive.client.OneDriveLongRunningAction;
import org.nuxeo.onedrive.client.OneDrivePatchOperation;
import org.nuxeo.onedrive.client.OneDriveUploadSession;
import org.nuxeo.onedrive.client.facets.FileSystemInfoFacet;

import com.eclipsesource.json.JsonObject;
import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Cache;
import vavi.nio.file.UploadMonitor;
import vavi.nio.file.Util;
import vavi.util.Debug;

import static vavi.nio.file.Util.toFilenameString;


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

        dummy = new OneDriveFile(client, drive, "dymmy", ItemIdentifierType.Path) {
            public Metadata getMetadata(OneDriveExpand... expand) {
                return new Metadata(new JsonObject()) {
                    public String getName() {
                        return "vavi-nio-file-onedrive3.dummy";
                    }
                    public ZonedDateTime getLastModifiedDateTime() {
                        return ZonedDateTime.now();
                    }
                };
            }
        }.getMetadata();
    }

    /** */
    private UploadMonitor uploadMonitor = new UploadMonitor();

    /** entry for uploading (for attributes) */
    private final OneDriveItem.Metadata dummy;

    /** */
    private Cache<OneDriveItem.Metadata> cache = new Cache<OneDriveItem.Metadata>() {
        /**
         * @see #ignoreAppleDouble
         * @throws NoSuchFileException must be thrown when the path is not found in this cache
         */
        public OneDriveItem.Metadata getEntry(Path path) throws IOException {
            if (cache.containsFile(path)) {
                return cache.getFile(path);
            } else {
                if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                    throw new NoSuchFileException("ignore apple double file: " + path);
                }

//                String pathString = toPathString(path);
//Debug.println("path: " + pathString);
                OneDriveItem.Metadata entry;
                if (path.getNameCount() == 0) {
                    entry = drive.getRoot().getMetadata();
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
        final OneDriveItem.Metadata entry = cache.getEntry(path);

        // TODO: metadata driver
        if (entry.isFolder()) {
            throw new IsDirectoryException("path: " + path);
        }

        return new BufferedInputStream(OneDriveFile.class.cast(entry.getResource()).download(), Util.BUFFER_SIZE);
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        OneDriveItem.Metadata entry = null;
        try {
            entry = cache.getEntry(path);

            if (entry.isFolder()) {
                throw new IsDirectoryException("path: " + path);
            } else {
                throw new FileAlreadyExistsException("path: " + path);
            }
        } catch (NoSuchFileException e) {
Debug.println("newOutputStream: " + e.getMessage());
//new Exception("*** DUMMY ***").printStackTrace();
        }

        OneDriveUploadOption uploadOption = Util.getOneOfOptions(OneDriveUploadOption.class, options);
        if (uploadOption != null) {
            // java.nio.file is highly abstracted, so here source information is lost.
            // but onedrive graph api requires content length for upload.
            // so reluctantly we provide {@link OneDriveUploadOpenOption} for {@link java.nio.file.Files#copy} options.
            Path source = uploadOption.getSource();
Debug.println("upload w/ option: " + source);
            return uploadEntry(path, (int) Files.size(source));
        } else {
Debug.println("upload w/o option");
            return new Util.OutputStreamForUploading() { // TODO used for only getting file length
                @Override
                protected void onClosed() throws IOException {
                    InputStream is = getInputStream();
Debug.println("upload w/o option: " + is.available());
                    OutputStream os = uploadEntry(path, is.available());
                    Util.transfer(is, os);
                    is.close();
                    os.close();
                }
            };
        }
    }

    /** */
    private OutputStream uploadEntry(Path path, int size) throws IOException {
        OneDriveFolder folder = OneDriveFolder.class.cast(cache.getEntry(path.getParent()).getResource());
        OneDriveFile file = new OneDriveFile(client, folder, URLEncoder.encode(toFilenameString(path), "utf-8"), ItemIdentifierType.Path);
        final OneDriveUploadSession uploadSession = file.createUploadSession();
        return new BufferedOutputStream(new OneDriveOutputStream(uploadSession, path, size, newEntry -> {
            try {
                cache.addEntry(path, newEntry);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }), Util.BUFFER_SIZE);
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
            uploadMonitor.start(path);
            return new Util.SeekableByteChannelForWriting(newOutputStream(path, options)) {
                @Override
                protected long getLeftOver() throws IOException {
                    long leftover = 0;
                    if (options.contains(StandardOpenOption.APPEND)) {
                        OneDriveItem.Metadata entry = cache.getEntry(path);
                        if (entry != null && entry.getSize() >= 0) {
                            leftover = entry.getSize();
                        }
                    }
                    return leftover;
                }
                @Override
                public void close() throws IOException {
                    uploadMonitor.finish(path);
                    super.close();
                }
            };
        } else {
            OneDriveItem.Metadata entry = cache.getEntry(path);
            if (entry.isFolder()) {
                throw new IsDirectoryException(path.toString());
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
        OneDriveItem.Metadata parentEntry = cache.getEntry(dir.getParent());

        // TODO: how to diagnose?
        OneDriveFolder.Metadata dirEntry = OneDriveFolder.class.cast(parentEntry.getResource()).create(toFilenameString(dir));

        cache.addEntry(dir, dirEntry);
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
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        if (uploadMonitor.isUploading(path)) {
Debug.println("uploading... : " + path);
            return;
        }

        final OneDriveItem.Metadata entry = cache.getEntry(path);

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
        // TODO: what to do here? OneDriveClient does not implement Closeable :(
    }

    /**
     * @throws IOException if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    public Object getPathMetadata(final Path path) throws IOException {
        if (uploadMonitor.isUploading(path)) {
Debug.println("uploading... : " + path);
            return dummy;
        }

        return cache.getEntry(path);
    }

    /** */
    private List<Path> getDirectoryEntries(Path dir) throws IOException {
        final OneDriveItem.Metadata entry = cache.getEntry(dir);

        if (!entry.isFolder()) {
            throw new NotDirectoryException(dir.toString());
        }

        List<Path> list = null;
        if (cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
            list = new ArrayList<>();

            for (final OneDriveItem.Metadata child : OneDriveFolder.class.cast(entry.getResource()).getChildren()) {
                Path childPath = dir.resolve(child.getName());
                list.add(childPath);
//System.err.println("child: " + childPath.toRealPath().toString());

                cache.putFile(childPath, child);
            }

            cache.putFolder(dir, list);
        }

        return list;
    }

    /** */
    private OneDriveItem.Metadata getEntry(Path path) throws IOException {
        final OneDriveItem.Metadata parentEntry = cache.getEntry(path.getParent());

        Optional<OneDriveItem.Metadata> found = StreamSupport.stream(OneDriveFolder.class.cast(parentEntry.getResource()).getChildren().spliterator(), false)
            .filter(child -> {
                try {
//System.out.println(child.getName() + ", " + toFilenameString(path));
                    return child.getName().equals(toFilenameString(path));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }).findFirst();
        if (found.isPresent()) {
            return found.get();
        } else {
            throw new NoSuchFileException(path.toString());
        }
    }

    /** */
    private void removeEntry(Path path) throws IOException {
        OneDriveItem.Metadata entry = cache.getEntry(path);
        if (entry.isFolder()) {
            // TODO use cache
            final List<OneDriveItem> children = StreamSupport.stream(OneDriveFolder.class.cast(entry.getResource()).getChildren().spliterator(), false)
                    .map(e -> e.getResource()).collect(Collectors.toList());

            if (children.size() > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        // TODO: unknown what happens when a move operation is performed
        // and the target already exists
        entry.getResource().delete();

        cache.removeEntry(path);
    }

    /** */
    private void copyEntry(final Path source, final Path target) throws IOException {
        OneDriveItem.Metadata sourceEntry = cache.getEntry(source);
        OneDriveItem.Metadata targetParentEntry = cache.getEntry(target.getParent());
        if (sourceEntry.isFile()) {
            OneDriveCopyOperation operation = new OneDriveCopyOperation();
            operation.rename(toFilenameString(target));
            operation.copy(OneDriveFolder.class.cast(targetParentEntry.getResource()));
            OneDriveLongRunningAction action = sourceEntry.getResource().copy(operation);
            action.await(statusObject -> {
Debug.printf("Copy Progress Operation %s progress %.0f %%, status %s",
 statusObject.getOperation(),
 statusObject.getPercentage(),
 statusObject.getStatus());
            });
            cache.addEntry(target, getEntry(target));
        } else if (sourceEntry.isFolder()) {
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /**
     * @param targetIsParent if the target is folder
     */
    private void moveEntry(final Path source, final Path target, boolean targetIsParent) throws IOException {
        OneDriveItem.Metadata sourceEntry = cache.getEntry(source);
        OneDriveItem.Metadata targetParentEntry = cache.getEntry(targetIsParent ? target : target.getParent());
        if (sourceEntry.isFile()) {
            OneDrivePatchOperation operation = new OneDrivePatchOperation();
            operation.rename(targetIsParent ? toFilenameString(source) : toFilenameString(target));
            operation.move(OneDriveFolder.class.cast(targetParentEntry.getResource()));
            final FileSystemInfoFacet info = new FileSystemInfoFacet();
            info.setLastModifiedDateTime(Instant.ofEpochMilli(sourceEntry.getLastModifiedDateTime().toEpochSecond()).atOffset(ZoneOffset.UTC));
            operation.facet("fileSystemInfo", info);
            sourceEntry.getResource().patch(operation);
            cache.removeEntry(source);
            if (targetIsParent) {
                cache.addEntry(target.resolve(source.getFileName()), getEntry(target.resolve(source.getFileName())));
            } else {
                cache.addEntry(target, getEntry(target));
            }
        } else if (sourceEntry.isFolder()) {
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException {
        OneDriveItem.Metadata sourceEntry = cache.getEntry(source);

        OneDrivePatchOperation operation = new OneDrivePatchOperation();
        operation.rename(toFilenameString(target));
        sourceEntry.getResource().patch(operation);
        cache.removeEntry(source);
        cache.addEntry(target, getEntry(target));
    }
}
