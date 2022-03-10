/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.CachedFileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;
import vavi.util.Debug;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;
import static vavi.nio.file.onedrive.OneDriveFileSystemProvider.ENV_USE_SYSTEM_WATCHER;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneItem;
import de.tuberlin.onedrivesdk.file.OneFile;
import de.tuberlin.onedrivesdk.folder.OneFolder;
import de.tuberlin.onedrivesdk.uploadFile.OneUpload;


/**
 * OneDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class OneDriveFileSystemDriver extends CachedFileSystemDriver<OneItem> {

    private final OneDriveSDK client;

    private OneDriveWatchService systemWatcher;

    @SuppressWarnings("unchecked")
    public OneDriveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final OneDriveSDK client,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.client = client;
        setEnv(env);
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
                    OneFile entry = client.getFileById(id);
                    Path parent = cache.getEntry(f -> { try { return entry.getParentFolder().getId().equals(f.getId()); } catch (IOException g) { g.printStackTrace(); return false; }});
                    Path path = parent.resolve(entry.getName());
Debug.println("NOTIFICATION: maybe created: " + path);
                    cache.addEntry(path, OneItem.class.cast(entry));
                }
            } catch (NoSuchElementException e) {
Debug.println("NOTIFICATION: parent not found: " + e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** */
    private static OneFile asFile(OneItem entry) {
        return OneFile.class.cast(entry);
    }

    /** */
    private static OneFolder asFolder(OneItem entry) {
        return OneFolder.class.cast(entry);
    }

    @Override
    protected String getFilenameString(OneItem entry) {
        return entry.getName();
    }

    @Override
    protected boolean isFolder(OneItem entry) {
        return entry.isFolder();
    }

    @Override
    protected OneItem getRootEntry(Path root) throws IOException {
        return client.getItemByPath(toPathString(root));
    }

    @Override
    protected OneItem getEntry(OneItem parentEntry, Path path)throws IOException {
        try {
            return client.getItemByPath(toPathString(path));
        } catch (OneDriveException e) {
            return null;
        }
    }

    @Override
    protected InputStream downloadEntry(OneItem entry, Path path, Set<? extends OpenOption> options) throws IOException {
        return asFile(entry).download().getDownloadedInputStream();
    }

    @Override
    protected OutputStream uploadEntry(OneItem parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        OneDriveUploadOption uploadOption = Util.getOneOfOptions(OneDriveUploadOption.class, options);
        if (uploadOption != null) {
            // java.nio.file is highly abstracted, so here source information is lost.
            // but onedrive graph api requires content length for upload.
            // so reluctantly we provide {@link OneDriveUploadOption} for {@link java.nio.file.Files#copy} options.
            Path source = uploadOption.getSource();
Debug.println("upload w/ option: " + source);

            return uploadEntry(parentEntry, path, (int) Files.size(source));
        } else {
Debug.println("upload w/o option");
            return new Util.OutputStreamForUploading() { // TODO used for getting file length
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

    /** OneDriveUploadOption */
    private OutputStream uploadEntry(OneItem parentEntry, Path path, int size) throws IOException {
        OneUpload uploader = asFolder(parentEntry).upload(toFilenameString(path), size, newEntry -> {
            updateEntry(path, newEntry);
        });
        return new BufferedOutputStream(uploader.upload(), Util.BUFFER_SIZE);
    }

    @Override
    protected List<OneItem> getDirectoryEntries(OneItem dirEntry, Path dir) throws IOException {
        return asFolder(dirEntry).getChildren();
    }

    @Override
    protected OneItem createDirectoryEntry(OneItem parentEntry, Path dir) throws IOException {
        // TODO: how to diagnose?
        return OneItem.class.cast(asFolder(parentEntry).createFolder(toFilenameString(dir)));
    }

    @Override
    protected boolean hasChildren(OneItem dirEntry, Path dir) throws IOException {
        return client.getFolderByPath(toPathString(dir)).getChildren().size() > 0;
    }

    @Override
    protected void removeEntry(OneItem entry, Path path) throws IOException {
        // TODO: unknown what happens when a move operation is performed
        // and the target already exists
        entry.delete();
    }

    @Override
    protected OneItem copyEntry(OneItem sourceEntry, OneItem targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
        OneFile newEntry = asFile(sourceEntry).copy(asFolder(targetParentEntry), toFilenameString(target));
Debug.println(newEntry.getParentFolder().getName() + "/" + newEntry.getName());
        return OneItem.class.cast(newEntry);
    }

    @Override
    protected OneItem moveEntry(OneItem sourceEntry, OneItem targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        return asFile(sourceEntry).move(asFolder(targetParentEntry));
    }

    @Override
    protected OneItem moveFolderEntry(OneItem sourceEntry, OneItem targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        OneItem newEntry = asFolder(sourceEntry).move(asFolder(targetParentEntry));
Debug.println(newEntry.getParentFolder().getName() + "/" + newEntry.getName());
        return newEntry;
    }

    @Override
    protected OneItem renameEntry(OneItem sourceEntry, OneItem targetParentEntry, Path source, Path target) throws IOException {
        return sourceEntry.rename(asFolder(targetParentEntry), toFilenameString(target));
    }

    @Override
    public void close() throws IOException {
        client.disconnect();
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
