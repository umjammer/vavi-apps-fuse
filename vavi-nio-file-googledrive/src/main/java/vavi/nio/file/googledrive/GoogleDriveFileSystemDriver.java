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
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;

import com.github.fge.filesystem.driver.DoubleCachedFileSystemDriver;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Revision;
import com.google.api.services.drive.model.RevisionList;
import vavi.nio.file.Util;
import vavi.nio.file.googledrive.GoogleDriveFileAttributesFactory.Metadata;
import vavi.util.Debug;
import vavi.util.StringUtil;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.googledrive.GoogleDriveFileSystemProvider.ENV_USE_SYSTEM_WATCHER;


/**
 * GoogleDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public final class GoogleDriveFileSystemDriver extends DoubleCachedFileSystemDriver<File> {

    private Drive drive;

    private GoogleDriveWatchService systemWatcher;

    @SuppressWarnings("unchecked")
    public GoogleDriveFileSystemDriver(FileStore fileStore,
            FileSystemFactoryProvider provider,
            Drive drive,
            Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.drive = drive;
        setEnv(env);
        boolean useSystemWatcher = (Boolean) ((Map<String, Object>) env).getOrDefault(ENV_USE_SYSTEM_WATCHER, false);

        if (useSystemWatcher) {
            systemWatcher = new GoogleDriveWatchService(drive);
            systemWatcher.setNotificationListener(this::processNotification);
        }

        if (fileSearcher == null) {
            fileSearcher = new FileSearcher(drive);
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
    protected String getFilenameString(File entry) {
        try {
            return Util.toNormalizedString(entry.getName());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected boolean isFolder(File entry) {
        return MIME_TYPE_DIR.equals(entry.getMimeType());
    }

    @Override
    protected File getRootEntry(Path root) throws IOException {
        return drive.files().get("root").setFields(ENTRY_FIELDS).execute().set("name", "/");
    }

    @Override
    protected File getEntry(File parentEntry, Path path) throws IOException {
        try {
            String q = "'" + parentEntry.getId() + "' in parents and name = '" + path.getFileName() + "' and trashed=false";
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
    protected InputStream downloadEntryImpl(File entry, Path path, Set<? extends OpenOption> options) throws IOException {
        // TODO detect automatically? (w/o options)
        if (options != null && options.stream().anyMatch(o -> o.equals(GoogleDriveOpenOption.EXPORT_WITH_GDOCS_DOCX) ||
                                                         o.equals(GoogleDriveOpenOption.EXPORT_WITH_GDOCS_XLSX))) {
            GoogleDriveOpenOption option = Util.getOneOfOptions(GoogleDriveOpenOption.class, options);
            // export google docs, sheet ... as specified type
            return drive.files().export(entry.getId(), option.getValue()).executeMediaAsInputStream();
        } else {
            // normal download
Debug.println(Level.FINE, "download: " + entry.getName() + ", " + entry.getSize());
            return drive.files().get(entry.getId()).executeMediaAsInputStream();
        }
    }

    @Override
    protected void whenUploadEntryExists(File destEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        if (options == null || options.stream().noneMatch(o -> o.equals(GoogleDriveOpenOption.IMPORT_AS_NEW_REVISION))) {
            super.whenUploadEntryExists(destEntry, path, options); // means throws FileAlreadyExistsException
        }
        // goto #uploadEntry()
    }

    @Override
    protected OutputStream uploadEntry(File parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {

        return new BufferedOutputStream(new Util.StealingOutputStreamForUploading<File>() {
            @Override
            protected File upload() throws IOException {
                AbstractInputStreamContent mediaContent = new AbstractInputStreamContent(null) { // implements HttpContent
                    @Override
                    public InputStream getInputStream() {
                        return null; // never called
                    }
                    @Override
                    public long getLength() {
                        return -1;
                    }
                    @Override
                    public boolean retrySupported() {
                        return false;
                    }
                    @Override
                    public void writeTo(OutputStream os) {
                        setOutputStream(os); // socket
                    }
                };

                if (options == null || options.stream().noneMatch(o -> o.equals(GoogleDriveOpenOption.IMPORT_AS_NEW_REVISION))) {
Debug.printf(Level.FINE, "new file: " + path);
                File entry = new File();
                entry.setName(toFilenameString(path));
                entry.setParents(Collections.singletonList(parentEntry.getId()));

                Drive.Files.Create creator = drive.files().create(entry, mediaContent); // why not HttpContent ???
                MediaHttpUploader uploader = creator.getMediaHttpUploader();
                uploader.setDirectUploadEnabled(true);
                // MediaHttpUploader#getProgress() cannot use because w/o content length, using #getNumBytesUploaded() instead
                    uploader.setProgressListener(u -> {
                        Debug.println(Level.FINE, "upload progress: " + u.getNumBytesUploaded() + ", " + u.getUploadState());
                    });
                return creator.setFields(ENTRY_FIELDS).execute();
                } else {
Debug.printf(Level.FINE, "new revision: " + path);
                    File entry = new File();

                    File destEntry = getEntry(path);
                    Drive.Files.Update updater = drive.files().update(destEntry.getId(), entry, mediaContent);
                    MediaHttpUploader uploader = updater.getMediaHttpUploader();
                    uploader.setDirectUploadEnabled(true);
                    // MediaHttpUploader#getProgress() cannot use because w/o content length, using #getNumBytesUploaded() instead
                    uploader.setProgressListener(u -> { Debug.println(Level.FINE, "new revision progress: " + u.getNumBytesUploaded() + ", " + u.getUploadState()); });
                    return updater.setFields(ENTRY_FIELDS).execute();
                }
            }

            @Override
            protected void onClosed(File newEntry) {
Debug.printf(Level.FINE, "file: %1$s, %2$tF %2$tT.%2$tL, %3$d\n", newEntry.getName(), newEntry.getCreatedTime().getValue(), newEntry.getSize());
                updateEntry(path, newEntry);
            }
        }, Util.BUFFER_SIZE);
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

            list.addAll(files.getFiles());

            pageToken = files.getNextPageToken();
//Debug.println("t: " + (System.currentTimeMillis() - t) + ", " + children.size() + ", " + (pageToken != null));
        } while (pageToken != null);

        return list;
    }

    @Override
    protected File createDirectoryEntry(File parentEntry, Path dir) throws IOException {
        File dirEntry = new File();
        dirEntry.setName(toFilenameString(dir));
        dirEntry.setMimeType(MIME_TYPE_DIR);
        if (dir.toAbsolutePath().getParent().getNameCount() != 0) {
            dirEntry.setParents(Collections.singletonList(parentEntry.getId()));
        }
        return drive.files().create(dirEntry).setFields(ENTRY_FIELDS).execute();
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
        entry.setParents(Collections.singletonList(targetParentEntry.getId()));
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

    @Override
    protected Object getPathMetadata(File entry) {
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
    public void close() {
        fileSearcher = null;
    }

    //
    // user:attributes
    //

    /** attributes user:description */
    void patchEntryDescription(File sourceEntry, String description) throws IOException {
        File entry = new File();
        entry.setDescription(description);
        File newEntry = drive.files().update(sourceEntry.getId(), entry).setFields(ENTRY_FIELDS).execute();
        Path path = cache.getEntry(sourceEntry);
        cache.removeEntry(path);
        cache.addEntry(path, newEntry);
    }

    /** attributes user:revisions */
    List<Revision> getRevisions(File entry) throws IOException {

        if (isFolder(entry)) {
            throw new IsDirectoryException("dir: " + entry.getName());
        }

        List<Revision> list = new ArrayList<>();
        String pageToken = null;
        do {
            RevisionList revisions = drive.revisions().list(entry.getId())
                    .setPageSize(1000)
                    .setFields("nextPageToken, revisions(id, mimeType, modifiedTime, size)")
                    .setPageToken(pageToken)
                    .execute();

            if (revisions.getRevisions() != null) {
Debug.println(Level.FINE, "revisions: " + revisions.getRevisions().size() + ", " + revisions.getNextPageToken());
                list.addAll(revisions.getRevisions());
            }

            pageToken = revisions.getNextPageToken();
        } while (pageToken != null);

        return list;
    }

    /** attributes user:revisions */
    void removeRevision(File entry, String revisionId) throws IOException {
Debug.println(Level.INFO, "delete revision: " + entry.getName() + ", revision: " + revisionId);
        drive.revisions().delete(entry.getId(), revisionId).execute();
    }

    /**
     * attributes user:thumbnail
     * @param image currently only jpeg is available.
     */
    void setThumbnail(File sourceEntry, byte[] image) throws IOException {
        File.ContentHints.Thumbnail thumbnail = new File.ContentHints.Thumbnail();
        thumbnail.setMimeType("image/jpg");
        thumbnail.encodeImage(image);

        File.ContentHints contentHints = new File.ContentHints();
        contentHints.setThumbnail(thumbnail);

        File entry = new File();
        entry.setContentHints(contentHints);

        File newEntry = drive.files().update(sourceEntry.getId(), entry).setFields("thumbnailLink").execute();
Debug.println(Level.INFO, "thumbnail updated: " + sourceEntry.getName() + ", size: " + image.length + ", " + StringUtil.paramString(newEntry));
    }

    /**
     * attributes user:thumbnail
     * @return url
     * @see "https://stackoverflow.com/a/45027853"
     */
    String getThumbnail(File sourceEntry) throws IOException {
        File entry = new File();

        File newEntry = drive.files().update(sourceEntry.getId(), entry).setFields("thumbnailLink").execute();
Debug.println(Level.INFO, "thumbnail url: " + sourceEntry.getName() + ", url: " + newEntry.getThumbnailLink());
        return newEntry.getThumbnailLink();
    }

    //
    // google drive specific
    //

    public static FileSearcher fileSearcher;

    /** */
    public static class FileSearcher {
        static final String SIMPLE_ENTRY_FIELDS = "id, name, parents";
        private final Drive drive;
        /** folder cache <id, File> */
        static Map<String, File> cache = new HashMap<>();
        private FileSearcher(Drive drive) {
            this.drive = drive;
        }
        /** @param root should be a google-drive file system */
        public List<Path> search(Path root, String queryTerm) throws IOException {
            List<Path> list = new ArrayList<>();
            String pageToken = null;
            do {
                FileList files = drive.files().list()
                        .setQ(queryTerm + " and trashed=false")
                        .setSpaces("drive")
                        .setPageSize(1000)
                        .setFields("nextPageToken, files(" + SIMPLE_ENTRY_FIELDS + ")")
                        .setPageToken(pageToken)
                        .setOrderBy("name_natural")
                        .execute();

                files.getFiles().forEach(f -> {
                    try {
                        List<String> pathElements = new ArrayList<>();
                        String pid = f.getParents().get(0);
                        while (true) {
                            File folder = getParent(pid);
                            if (!hasParent(folder)) {
                                break;
                            }
                            pathElements.add(0, folder.getName());
                            pid = folder.getParents().get(0);
                        }
                        pathElements.add(f.getName());
                        list.add(root.resolve(String.join(java.io.File.separator, pathElements)));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

                pageToken = files.getNextPageToken();
            } while (pageToken != null);

            return list;
        }
        private boolean hasParent(File file) {
            return file.getParents() != null && file.getParents().size() != 0;
        }
        private File getParent(String pid) throws IOException {
            File parent = cache.get(pid);
            if (parent == null) {
                parent = drive.files().get(pid).setFields(SIMPLE_ENTRY_FIELDS).execute();
                cache.put(pid, parent);
            }
            return parent;
        }
    }
}
