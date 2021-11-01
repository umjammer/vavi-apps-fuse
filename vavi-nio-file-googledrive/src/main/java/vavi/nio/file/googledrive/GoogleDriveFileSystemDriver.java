/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.CachedFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import vavi.nio.file.Util;
import vavi.nio.file.googledrive.GoogleDriveFileAttributesFactory.Metadata;
import vavi.util.Debug;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.googledrive.GoogleDriveFileSystemProvider.ENV_IGNORE_APPLE_DOUBLE;
import static vavi.nio.file.googledrive.GoogleDriveFileSystemProvider.ENV_USE_SYSTEM_WATCHER;


/**
 * GoogleDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class GoogleDriveFileSystemDriver extends CachedFileSystemDriverBase<File> {

    private final Drive drive;

    private GoogleDriveWatchService systemWatcher;

    @SuppressWarnings("unchecked")
    public GoogleDriveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final Drive drive,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.drive = drive;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault(ENV_IGNORE_APPLE_DOUBLE, false);
        boolean useSystemWatcher = (Boolean) ((Map<String, Object>) env).getOrDefault(ENV_USE_SYSTEM_WATCHER, false);

        if (useSystemWatcher) {
            systemWatcher = new GoogleDriveWatchService(drive);
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
                    File entry = drive.files().get(id).execute();
                    Path parent = cache.getEntry(f -> entry.getParents().get(0).equals(f.getId()));
                    Path path = parent.resolve(entry.getName());
Debug.println("NOTIFICATION: maybe created: " + path);
                    cache.addEntry(path, entry);
                }
            } catch (NoSuchElementException e) {
Debug.println("NOTIFICATION: parent not found: " + e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** */
    private static final String ENTRY_FIELDS = "id, parents, name, size, mimeType, createdTime, modifiedTime, description";

    /** */
    public static final String MIME_TYPE_DIR = "application/vnd.google-apps.folder";

    @Override
    protected String getFilenameString(File entry) throws IOException {
    	return Util.toNormalizedString(entry.getName());
    }

    @Override
    protected boolean isFolder(File entry) {
        return MIME_TYPE_DIR.equals(entry.getMimeType());
    }

    @Override
    protected File getRootEntry() throws IOException {
    	return drive.files().get("root").setFields(ENTRY_FIELDS).execute().set("name", "/");
    }

    @Override
    protected File getEntry(File dirEntry, Path path) throws IOException {
        try {
    		String q = "'" + dirEntry.getId() + "' in parents and name = '" + path.getFileName() + "' and trashed=false";
//System.out.println("q: " + q);
	        FileList files = drive.files().list()
	                .setQ(q)
	                .setSpaces("drive")
	                .setFields("nextPageToken, files(" + ENTRY_FIELDS + ")")
	                .execute();
	        if (files.getFiles().size() > 0) {
	        	return files.getFiles().get(0);
	        } else {
	        	return null;
	        }
	    } catch (GoogleJsonResponseException e) {
	        if (e.getMessage().startsWith("404")) {
	        	return null;
	        } else {
	            throw e;
	        }
	    }
    }

    @Override
    protected InputStream downloadEntry(File entry, Path path, Set<? extends OpenOption> options) throws IOException {
        // TODO detect automatically?
        GoogleDriveOpenOption option = Util.getOneOfOptions(GoogleDriveOpenOption.class, options);
        if (option != null) {
            return drive.files().export(entry.getId(), option.getValue()).executeMediaAsInputStream();
        } else {
            return drive.files().get(entry.getId()).executeMediaAsInputStream();
        }
    }

    @Override
    protected OutputStream uploadEntry(File parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        // TODO detect automatically?
        @SuppressWarnings("unused")
        GoogleDriveOpenOption option = Util.getOneOfOptions(GoogleDriveOpenOption.class, options);

        //
        return new BufferedOutputStream(new Util.StealingOutputStreamForUploading<File>() {
            @Override
            protected File upload() throws IOException {
                AbstractInputStreamContent mediaContent = new AbstractInputStreamContent(null) { // implements HttpContent
                    @Override
                    public InputStream getInputStream() throws IOException {
                        return null; // never called
                    }
                    @Override
                    public long getLength() throws IOException {
                        return -1;
                    }
                    @Override
                    public boolean retrySupported() {
                        return false;
                    }
                    @Override
                    public void writeTo(OutputStream os) throws IOException {
                    	setOutputStream(os); // socket
                    }
                };

                File fileMetadata = new File();
                fileMetadata.setName(toFilenameString(path));
                fileMetadata.setParents(Arrays.asList(parentEntry.getId()));

                Drive.Files.Create creator = drive.files().create(fileMetadata, mediaContent); // why not HttpContent ???
                MediaHttpUploader uploader = creator.getMediaHttpUploader();
                uploader.setDirectUploadEnabled(true);
                // MediaHttpUploader#getProgress() cannot use because w/o content length, using #getNumBytesUploaded() instead
                uploader.setProgressListener(u -> { Debug.println("upload progress: " + u.getNumBytesUploaded() + ", " + u.getUploadState()); });
                return creator.setFields(ENTRY_FIELDS).execute();
            }

            @Override
            protected void onClosed(File newEntry) {
Debug.printf("file: %1$s, %2$tF %2$tT.%2$tL, %3$d\n", newEntry.getName(), newEntry.getCreatedTime().getValue(), newEntry.getSize());
                cache.addEntry(path, newEntry);
            }
        }, Util.BUFFER_SIZE);
    }

    @Override
    protected File createDirectoryEntry(Path dir) throws IOException {
        File dirEntry = new File();
        dirEntry.setName(toFilenameString(dir));
        dirEntry.setMimeType(MIME_TYPE_DIR);
        if (dir.toAbsolutePath().getParent().getNameCount() != 0) {
            dirEntry.setParents(Arrays.asList(cache.getEntry(dir.toAbsolutePath().getParent()).getId()));
        }
        return drive.files().create(dirEntry).setFields(ENTRY_FIELDS).execute();
    }

    @Override
    protected Object getMetadata(File entry) throws IOException {
        return new Metadata(this, entry);
    }

    @Override
    public WatchService newWatchService() {
        try {
            return new GoogleDriveWatchService(drive);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected List<File> getDirectoryEntries(File dirEntry, Path dir) throws IOException {
    	List<File> list = new ArrayList<>();
        String pageToken = null;
        do {
            FileList files = drive.files().list()
                    .setQ("'" + dirEntry.getId() + "' in parents and trashed=false")
                    .setSpaces("drive")
                    .setPageSize(1000)
                    .setFields("nextPageToken, files(" + ENTRY_FIELDS + ")")
                    .setPageToken(pageToken)
                    .setOrderBy("name_natural")
                    .execute();

            for (File child : files.getFiles()) {
            	list.add(child);
            }

            pageToken = files.getNextPageToken();
//System.out.println("t: " + (System.currentTimeMillis() - t) + ", " + children.size() + ", " + (pageToken != null));
        } while (pageToken != null);

        return list;
    }

    @Override
    protected boolean hasChildren(File dirEntry, Path dir) throws IOException {
        // TODO use cache ???
    	List<File> files = drive.files().list()
                .setQ("'" + dirEntry.getId() + "' in parents and trashed=false")
                .execute().getFiles();
    	return files != null && files.size() > 0;
    }

    @Override
    protected void removeEntry(File entry, Path path) throws IOException {
        drive.files().delete(entry.getId()).execute();
    }

    @Override
    protected File copyEntry(File sourceEntry, File targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
        File entry = new File();
        entry.setName(toFilenameString(target));
        entry.setParents(Arrays.asList(targetParentEntry.getId()));
        if (options != null && options.stream().anyMatch(o -> o.equals(GoogleDriveCopyOption.EXPORT_AS_GDOCS))) {
            entry.setMimeType(GoogleDriveCopyOption.EXPORT_AS_GDOCS.getValue());
        }
        return drive.files().copy(sourceEntry.getId(), entry).setFields(ENTRY_FIELDS).execute();
    }

    @Override
    protected File moveEntry(File sourceEntry, File targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        File entry = new File();
        entry.setName(toFilenameString(target));
        String previousParents = null;
        if (sourceEntry.getParents() != null) {
            previousParents = String.join(",", sourceEntry.getParents());
        }
        return drive.files().update(sourceEntry.getId(), entry)
                .setAddParents(targetParentEntry.getId())
                .setRemoveParents(previousParents)
                .setFields(ENTRY_FIELDS).execute();
    }

    @Override
    protected File moveFolderEntry(File sourceEntry, File targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        File dirEntry = new File();
        dirEntry.setName(toFilenameString(target));
        dirEntry.setMimeType(MIME_TYPE_DIR);
        String previousParents = null;
        if (sourceEntry.getParents() != null) {
            previousParents = String.join(",", sourceEntry.getParents());
        }
        return drive.files().update(sourceEntry.getId(), dirEntry)
                .setAddParents(targetParentEntry.getId())
                .setRemoveParents(previousParents)
                .setFields(ENTRY_FIELDS).execute();
    }

    @Override
    protected File renameEntry(File sourceEntry, File targetParentEntry, Path source, Path target) throws IOException {
        File entry = new File();
        entry.setName(toFilenameString(target));
        return drive.files().update(sourceEntry.getId(), entry).setFields(ENTRY_FIELDS).execute();
    }

    /** attributes user:description */
    void patchEntryDescription(File sourceEntry, String description) throws IOException {
        File entry = new File();
        entry.setDescription(description);
        File newEntry = drive.files().update(sourceEntry.getId(), entry).setFields(ENTRY_FIELDS).execute();
        Path path = cache.getEntry(sourceEntry);
        cache.removeEntry(path);
        cache.addEntry(path, newEntry);
    }
}
