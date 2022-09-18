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
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchService;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.DoubleCachedFileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import org.nuxeo.onedrive.client.CopyOperation;
import org.nuxeo.onedrive.client.Files;
import org.nuxeo.onedrive.client.OneDriveAPI;
import org.nuxeo.onedrive.client.OneDriveLongRunningAction;
import org.nuxeo.onedrive.client.PatchOperation;
import org.nuxeo.onedrive.client.UploadSession;
import org.nuxeo.onedrive.client.types.Drive;
import org.nuxeo.onedrive.client.types.DriveItem;
import org.nuxeo.onedrive.client.types.FileSystemInfo;
import vavi.nio.file.Util;
import vavi.util.Debug;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.onedrive3.OneDriveFileSystemProvider.ENV_USE_SYSTEM_WATCHER;


/**
 * OneDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class OneDriveFileSystemDriver extends DoubleCachedFileSystemDriver<DriveItem.Metadata> {

    private final OneDriveAPI client;
    private final Drive.Metadata drive;

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
        setEnv(env);
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
    private static DriveItem asDriveItem(DriveItem.Metadata entry) {
        return DriveItem.class.cast(entry.getItem());
    }

    @Override
    protected String getFilenameString(DriveItem.Metadata entry) {
        return entry.getName();
    }

    @Override
    protected boolean isFolder(DriveItem.Metadata entry) {
        return entry.isFolder();
    }

    @Override
    protected DriveItem.Metadata getRootEntry(Path root) throws IOException {
        return new Drive(client, drive.getId()).getRoot().getMetadata();
    }

    @Override
    protected InputStream downloadEntryImpl(DriveItem.Metadata entry, Path path, Set<? extends OpenOption> options) throws IOException {
        return new BufferedInputStream(Files.download(asDriveItem(entry)), Util.BUFFER_SIZE);
    }

    @Override
    protected OutputStream uploadEntry(DriveItem.Metadata parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        OneDriveUploadOption uploadOption = Util.getOneOfOptions(OneDriveUploadOption.class, options);
        if (uploadOption != null) {
            // java.nio.file is highly abstracted, so here source information is lost.
            // but onedrive graph api requires content length for upload.
            // so reluctantly we provide {@link OneDriveUploadOption} for {@link java.nio.file.Files#copy} options.
            Path source = uploadOption.getSource();
Debug.println("upload w/ option: " + source);
            return uploadEntry(parentEntry, path, (int) java.nio.file.Files.size(source));
        } else {
Debug.println("upload w/o option");
            return new Util.OutputStreamForUploading() { // TODO used for only getting file length
                @Override
                protected void onClosed() throws IOException {
                    InputStream is = getInputStream();
Debug.println("upload w/o option: " + is.available());
                    OutputStream os = uploadEntry(parentEntry, path, is.available());
                    Util.transfer(is, os);
                    is.close();
                    os.close();
                }
            };
        }
    }

    /** */
    private OutputStream uploadEntry(DriveItem.Metadata parentEntry, Path path, int size) throws IOException {
        DriveItem file = new DriveItem(asDriveItem(parentEntry), toItemPathString(toFilenameString(path)));
        final UploadSession uploadSession = Files.createUploadSession(file);
        return new BufferedOutputStream(new OneDriveOutputStream(uploadSession, path, size, newEntry -> {
            updateEntry(path, newEntry);
        }), Util.BUFFER_SIZE);
    }

    /** ms-graph doesn't accept '+' in a path string */
    private String toItemPathString(String pathString) throws IOException {
        return URLEncoder.encode(pathString, "utf-8").replace("+", "%20");
    }

    @Override
    protected List<DriveItem.Metadata> getDirectoryEntries(DriveItem.Metadata dirEntry, Path dir) throws IOException {
        Iterator<DriveItem.Metadata> iterator = Files.getFiles(asDriveItem(dirEntry));
        Spliterator<DriveItem.Metadata> spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
        return StreamSupport.stream(spliterator, false).collect(Collectors.toList());
    }

    @Override
    protected DriveItem.Metadata createDirectoryEntry(DriveItem.Metadata parentEntry, Path dir) throws IOException {
        return Files.createFolder(asDriveItem(parentEntry), toFilenameString(dir));
    }

    @Override
    protected boolean hasChildren(DriveItem.Metadata dirEntry, Path dir) throws IOException {
        return getDirectoryEntries(dir, false).size() > 0;
    }

    @Override
    protected void removeEntry(DriveItem.Metadata entry, Path path) throws IOException {
        Files.delete(asDriveItem(entry));
    }

    @Override
    protected DriveItem.Metadata copyEntry(DriveItem.Metadata sourceEntry, DriveItem.Metadata targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
        CopyOperation operation = new CopyOperation();
        operation.rename(toFilenameString(target));
Debug.println("target: " + targetParentEntry.getName());
        operation.copy(asDriveItem(targetParentEntry));
        OneDriveLongRunningAction action = Files.copy(asDriveItem(sourceEntry), operation);
        action.await(statusObject -> {
Debug.printf("Copy Progress Operation %s progress %.0f %%, status %s",
statusObject.getOperation(),
statusObject.getPercentage(),
statusObject.getStatus());
        });
        return getEntry(null, target);
    }

    @Override
    protected DriveItem.Metadata moveEntry(DriveItem.Metadata sourceEntry, DriveItem.Metadata targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        PatchOperation operation = new PatchOperation();
        operation.rename(targetIsParent ? toFilenameString(source) : toFilenameString(target));
        operation.move(asDriveItem(targetParentEntry));
        final FileSystemInfo info = new FileSystemInfo();
        info.setLastModifiedDateTime(Instant.ofEpochMilli(sourceEntry.getLastModifiedDateTime().toEpochSecond()).atOffset(ZoneOffset.UTC));
        operation.facet("fileSystemInfo", info);
        Files.patch(asDriveItem(sourceEntry), operation);
        if (targetIsParent) {
            return getEntry(null, target.resolve(source.getFileName()));
        } else {
            return getEntry(null, target);
        }
    }

    @Override
    protected DriveItem.Metadata moveFolderEntry(DriveItem.Metadata sourceEntry, DriveItem.Metadata targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        PatchOperation operation = new PatchOperation();
        operation.rename(toFilenameString(target));
        operation.move(asDriveItem(targetParentEntry));
        final FileSystemInfo info = new FileSystemInfo();
        info.setLastModifiedDateTime(Instant.ofEpochMilli(sourceEntry.getLastModifiedDateTime().toEpochSecond()).atOffset(ZoneOffset.UTC));
        operation.facet("fileSystemInfo", info);
        Files.patch(asDriveItem(sourceEntry), operation);
        return getEntry(null, target);
    }

    @Override
    protected DriveItem.Metadata renameEntry(DriveItem.Metadata sourceEntry, DriveItem.Metadata targetParentEntry, Path source, Path target) throws IOException {
        PatchOperation operation = new PatchOperation();
        operation.rename(toFilenameString(target));
        Files.patch(asDriveItem(sourceEntry), operation);
        return getEntry(null, target);
    }

    @Override
    public void close() throws IOException {
        closer.run();
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
}
