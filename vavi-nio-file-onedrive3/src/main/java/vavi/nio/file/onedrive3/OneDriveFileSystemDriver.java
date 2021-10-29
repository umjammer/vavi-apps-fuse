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
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.nuxeo.onedrive.client.OneDriveAPI;
import org.nuxeo.onedrive.client.CopyOperation;
import org.nuxeo.onedrive.client.Files;
import org.nuxeo.onedrive.client.OneDriveLongRunningAction;
import org.nuxeo.onedrive.client.PatchOperation;
import org.nuxeo.onedrive.client.UploadSession;
import org.nuxeo.onedrive.client.types.Drive;
import org.nuxeo.onedrive.client.types.DriveItem;
import org.nuxeo.onedrive.client.types.FileSystemInfo;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Cache;
import vavi.nio.file.Util;
import vavi.util.Debug;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.onedrive3.OneDriveFileSystemProvider.ENV_IGNORE_APPLE_DOUBLE;
import static vavi.nio.file.onedrive3.OneDriveFileSystemProvider.ENV_USE_SYSTEM_WATCHER;


/**
 * OneDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class OneDriveFileSystemDriver extends ExtendedFileSystemDriverBase {

    private final OneDriveAPI client;
    private final Drive.Metadata drive;
    private boolean ignoreAppleDouble = false;

    private Runnable closer;
    private OneDriveWatchService systemWatcher;

    @SuppressWarnings("unchecked")
    public OneDriveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final OneDriveAPI client,
            Runnable closer,
            final Drive.Metadata drive,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.client = client;
        this.closer = closer;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault(ENV_IGNORE_APPLE_DOUBLE, false);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
        this.drive = drive;
        boolean useSystemWatcher = (Boolean) ((Map<String, Object>) env).getOrDefault(ENV_USE_SYSTEM_WATCHER, false);

        if (useSystemWatcher) {
            systemWatcher = new OneDriveWatchService(client);
            systemWatcher.setNotificationListener(this::processNotification);
        }
    }

    /** for system watcher */
    private void processNotification(String id, Kind<?> kind) {
        if (ENTRY_DELETE == kind) {
            try {
                Path path = cache.getEntry(e -> id.equals(e.getId()));
                cache.removeEntry(path);
            } catch (NoSuchElementException e) {
Debug.println("NOTIFICATION: already deleted: " + id);
            }
        } else {
            try {
                try {
                    Path path = cache.getEntry(e -> id.equals(e.getId()));
Debug.println("NOTIFICATION: maybe updated: " + path);
                    cache.removeEntry(path);
                    cache.getEntry(path);
                } catch (NoSuchElementException e) {
// TODO impl
//                    OneDriveItem.Metadata entry = drive.getApi().getMetadata(id);
//                    Path parent = cache.getEntry(f -> entry.getParentReference().getId().equals(f.getId()));
//                    Path path = parent.resolve(entry.getName());
//Debug.println("NOTIFICATION: maybe created: " + path);
//                    cache.addEntry(path, entry);
                }
            } catch (NoSuchElementException e) {
Debug.println("NOTIFICATION: parent not found: " + e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** */
    private Cache<DriveItem.Metadata> cache = new Cache<DriveItem.Metadata>() {
        /**
         * @see #ignoreAppleDouble
         * @throws NoSuchFileException must be thrown when the path is not found in this cache
         */
        public DriveItem.Metadata getEntry(Path path) throws IOException {
            if (cache.containsFile(path)) {
                return cache.getFile(path);
            } else {
                if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                    throw new NoSuchFileException("ignore apple double file: " + path);
                }

//                String pathString = toPathString(path);
//Debug.println("path: " + pathString);
                DriveItem.Metadata entry;
                if (path.getNameCount() == 0) {
                    entry = new Drive(client, drive.getId()).getRoot().getMetadata();
                    cache.putFile(path, entry);
                    return entry;
                } else {
                    // TODO make this like google drive
                    List<Path> siblings = getDirectoryEntries(path.toAbsolutePath().getParent(), false);
                    Optional<Path> found = siblings.stream().filter(p -> path.getFileName().equals(p.getFileName())).findFirst();
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
        final DriveItem.Metadata entry = cache.getEntry(path);

        if (entry.isFolder()) {
            throw new IsDirectoryException("path: " + path);
        }

        return new BufferedInputStream(Files.download(DriveItem.class.cast(entry.getItem())), Util.BUFFER_SIZE);
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        DriveItem.Metadata entry = null;
        try {
            entry = cache.getEntry(path);

            if (entry.isFolder()) {
                throw new IsDirectoryException("path: " + path);
            } else {
                throw new FileAlreadyExistsException("path: " + path);
            }
        } catch (NoSuchFileException e) {
Debug.println("newOutputStream: " + e.getMessage());
        }

        OneDriveUploadOption uploadOption = Util.getOneOfOptions(OneDriveUploadOption.class, options);
        if (uploadOption != null) {
            // java.nio.file is highly abstracted, so here source information is lost.
            // but onedrive graph api requires content length for upload.
            // so reluctantly we provide {@link OneDriveUploadOpenOption} for {@link java.nio.file.Files#copy} options.
            Path source = uploadOption.getSource();
Debug.println("upload w/ option: " + source);
            return uploadEntry(path, (int) java.nio.file.Files.size(source));
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
        DriveItem.Metadata folder = cache.getEntry(path.toAbsolutePath().getParent());
        DriveItem file = new DriveItem(DriveItem.class.cast(folder.getItem()), toItemPathString(toFilenameString(path)));
        final UploadSession uploadSession = Files.createUploadSession(file);
        return new BufferedOutputStream(new OneDriveOutputStream(uploadSession, path, size, newEntry -> {
            cache.addEntry(path, newEntry);
        }), Util.BUFFER_SIZE);
    }

    /** ms-graph doesn't accept '+' in a path string */
    private String toItemPathString(String pathString) throws IOException {
        return URLEncoder.encode(pathString, "utf-8").replace("+", "%20");
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir, true), filter);
    }


    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        DriveItem.Metadata parentEntry = cache.getEntry(dir.toAbsolutePath().getParent());

        // TODO: how to diagnose?
        DriveItem.Metadata dirEntry = Files.createFolder(DriveItem.class.cast(parentEntry.getItem()), toFilenameString(dir));

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
            if (source.toAbsolutePath().getParent().equals(target.toAbsolutePath().getParent())) {
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
        final DriveItem.Metadata entry = cache.getEntry(path);

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
        closer.run();
    }

    /**
     * @throws IOException if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    protected Object getPathMetadataImpl(final Path path) throws IOException {
        return cache.getEntry(path);
    }

    @Nonnull
    @Override
    public WatchService newWatchService() {
        try {
            return new OneDriveWatchService(client);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    private List<Path> getDirectoryEntries(Path dir, boolean useCache) throws IOException {
        final DriveItem.Metadata entry = cache.getEntry(dir);

        if (!entry.isFolder()) {
            throw new NotDirectoryException(dir.toString());
        }

        final List<Path> list;
        if (useCache && cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
            list = new ArrayList<>();

            Iterator<DriveItem.Metadata> iterator = Files.getFiles(DriveItem.class.cast(entry));
            Spliterator<DriveItem.Metadata> spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
            StreamSupport.stream(spliterator, false).forEach(child -> {
                Path childPath = dir.resolve(child.getName());
                list.add(childPath);
//System.err.println("child: " + childPath.toRealPath().toString());

                cache.putFile(childPath, child);
            });

            cache.putFolder(dir, list);
        }

        return list;
    }

    /** for created entry */
    private DriveItem.Metadata getEntry(Path path) throws IOException {
        final DriveItem.Metadata parentEntry = cache.getEntry(path.toAbsolutePath().getParent());

        Iterator<DriveItem.Metadata> iterator = Files.getFiles(DriveItem.class.cast(parentEntry));
        Spliterator<DriveItem.Metadata> spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
        Optional<DriveItem.Metadata> found = StreamSupport.stream(spliterator, false)
            .filter(child -> {
                try {
System.err.println(child.getName() + ", " + toFilenameString(path));
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
        DriveItem.Metadata entry = cache.getEntry(path);
        if (entry.isFolder()) {
            if (getDirectoryEntries(path, false).size() > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        // TODO: unknown what happens when a move operation is performed
        // and the target already exists
        Files.delete(DriveItem.class.cast(entry));

        cache.removeEntry(path);
    }

    /** */
    private void copyEntry(final Path source, final Path target) throws IOException {
        DriveItem.Metadata sourceEntry = cache.getEntry(source);
        DriveItem.Metadata targetParentEntry = cache.getEntry(target.toAbsolutePath().getParent());
        if (sourceEntry.isFile()) {
            CopyOperation operation = new CopyOperation();
            operation.rename(toFilenameString(target));
            Files.copy(DriveItem.class.cast(targetParentEntry), operation);
            OneDriveLongRunningAction action = Files.copy(DriveItem.class.cast(sourceEntry), operation);
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
        DriveItem.Metadata sourceEntry = cache.getEntry(source);
        DriveItem.Metadata targetParentEntry = cache.getEntry(targetIsParent ? target : target.toAbsolutePath().getParent());
        if (sourceEntry.isFile()) {
            PatchOperation operation = new PatchOperation();
            operation.rename(targetIsParent ? toFilenameString(source) : toFilenameString(target));
            operation.move(DriveItem.class.cast(targetParentEntry));
            final FileSystemInfo info = new FileSystemInfo();
            info.setLastModifiedDateTime(Instant.ofEpochMilli(sourceEntry.getLastModifiedDateTime().toEpochSecond()).atOffset(ZoneOffset.UTC));
            operation.facet("fileSystemInfo", info);
            Files.patch(DriveItem.class.cast(sourceEntry), operation);
            cache.removeEntry(source);
            if (targetIsParent) {
                cache.addEntry(target.resolve(source.getFileName()), getEntry(target.resolve(source.getFileName())));
            } else {
                cache.addEntry(target, getEntry(target));
            }
        } else if (sourceEntry.isFolder()) {
            PatchOperation operation = new PatchOperation();
            operation.rename(toFilenameString(target));
            operation.move(DriveItem.class.cast(targetParentEntry));
            final FileSystemInfo info = new FileSystemInfo();
            info.setLastModifiedDateTime(Instant.ofEpochMilli(sourceEntry.getLastModifiedDateTime().toEpochSecond()).atOffset(ZoneOffset.UTC));
            operation.facet("fileSystemInfo", info);
            Files.patch(DriveItem.class.cast(sourceEntry), operation);
            cache.moveEntry(source, target, getEntry(target));
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException {
        DriveItem.Metadata sourceEntry = cache.getEntry(source);

        PatchOperation operation = new PatchOperation();
        operation.rename(toFilenameString(target));
        Files.patch(DriveItem.class.cast(sourceEntry), operation);
        cache.removeEntry(source);
        cache.addEntry(target, getEntry(target));
    }
}
