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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import vavi.nio.file.Cache;
import vavi.nio.file.Util;
import vavi.nio.file.googledrive.GoogleDriveFileAttributesFactory.Metadata;
import vavi.util.Debug;

import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.googledrive.GoogleDriveFileSystemProvider.ENV_IGNORE_APPLE_DOUBLE;


/**
 * GoogleDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class GoogleDriveFileSystemDriver extends ExtendedFileSystemDriverBase {

    private final Drive drive;

    private boolean ignoreAppleDouble = false;

    @SuppressWarnings("unchecked")
    public GoogleDriveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final Drive drive,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.drive = drive;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault(ENV_IGNORE_APPLE_DOUBLE, false);
    }

    /** */
    private static final String ENTRY_FIELDS = "id, parents, name, size, mimeType, createdTime, modifiedTime, description";

    /** */
    private static final String MIME_TYPE_DIR = "application/vnd.google-apps.folder";

    /** ugly */
    static boolean isFolder(File file) {
        return MIME_TYPE_DIR.equals(file.getMimeType());
    }

    /** */
    private Cache<File> cache = new Cache<File>() {
        /**
         * @see #ignoreAppleDouble
         */
        public File getEntry(Path path) throws IOException {
            try {
                if (cache.containsFile(path)) {
                    return cache.getFile(path);
                } else {
                    if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                        throw new NoSuchFileException("ignore apple double file: " + path);
                    }

                    File entry;
                    if (path.getNameCount() == 0) {
                        entry = drive.files().get("root").setFields(ENTRY_FIELDS).execute().set("name", "/");
                        cache.putFile(path, entry);
                        return entry;
                    } else {
                        List<Path> siblings = getDirectoryEntries(path.toAbsolutePath().getParent(), false);
                        for (int i = 0; i < siblings.size(); i++) { // avoid ConcurrentModificationException
                            Path p = siblings.get(i);
                            if (p.getFileName().equals(path.getFileName())) {
                                return cache.getEntry(p);
                            }
                        }
                        throw new NoSuchFileException(path.toString());
                    }
                }
            } catch (GoogleJsonResponseException e) {
                if (e.getMessage().startsWith("404")) {
                    // cache
                    if (cache.containsFile(path)) {
                        cache.removeEntry(path);
                    }

                    throw (IOException) new NoSuchFileException("path: " + path).initCause(e);
                } else {
                    throw e;
                }
            }
        }
    };

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final File entry = cache.getEntry(path);

        if (isFolder(entry)) {
            throw new IsDirectoryException("path: " + path);
        }

        // TODO detect automatically?
        GoogleDriveOpenOption option = Util.getOneOfOptions(GoogleDriveOpenOption.class, options);
        if (option != null) {
            return drive.files().export(entry.getId(), option.getValue()).executeMediaAsInputStream();
        } else {
            return drive.files().get(entry.getId()).executeMediaAsInputStream();
        }
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final File entry;
        try {
            entry = cache.getEntry(path);

            if (isFolder(entry)) {
                throw new IsDirectoryException("path: " + path);
            } else {
                throw new FileAlreadyExistsException("path: " + path);
            }
        } catch (NoSuchFileException e) {
Debug.println("newOutputStream: " + e.getMessage());
        }

        // TODO detect automatically?
        @SuppressWarnings("unused")
        GoogleDriveOpenOption option = Util.getOneOfOptions(GoogleDriveOpenOption.class, options);

        return uploadEntry(path);
    }

    /** */
    private OutputStream uploadEntry(Path path) throws IOException {
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
                fileMetadata.setParents(Arrays.asList(cache.getEntry(path.toAbsolutePath().getParent()).getId()));

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

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir, true), filter);
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        File dirEntry = new File();
        dirEntry.setName(toFilenameString(dir));
        dirEntry.setMimeType(MIME_TYPE_DIR);
        if (dir.toAbsolutePath().getParent().getNameCount() != 0) {
            dirEntry.setParents(Arrays.asList(cache.getEntry(dir.toAbsolutePath().getParent()).getId()));
        }
        File newEntry = drive.files().create(dirEntry).setFields(ENTRY_FIELDS).execute();
        cache.addEntry(dir, newEntry);
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

        copyEntry(source, target, options);
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        if (cache.existsEntry(target)) {
            if (isFolder(cache.getEntry(target))) {
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
     *                     if you use this with fuse, you should throw {@link NoSuchFileException} when the file not found.
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    @Override
    protected void checkAccessImpl(final Path path, final AccessMode... modes) throws IOException {
        final File entry = cache.getEntry(path);

        if (isFolder(entry)) {
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
        // GoogleDrive does not implement Closeable :(
    }

    /**
     * @throws IOException if you use this with fuse, you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    protected Object getPathMetadataImpl(final Path path) throws IOException {
        return new Metadata(this, cache.getEntry(path));
    }

    /** */
    private List<Path> getDirectoryEntries(Path dir, boolean useCache) throws IOException {
        final File entry = cache.getEntry(dir);

        if (!isFolder(entry)) {
            throw new NotDirectoryException("dir: " + dir);
        }

        List<Path> list = new ArrayList<>();
        if (useCache && cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
            Files.List request = drive.files().list();
            do {
                FileList files = request
                        .setQ("'" + entry.getId() + "' in parents and trashed=false")
                        .setFields("nextPageToken, files(" + ENTRY_FIELDS + ")").execute();
                final List<File> children = files.getFiles();
                request.setPageToken(files.getNextPageToken());

                for (final File child : children) {
                    Path childPath = dir.resolve(child.getName());
                    list.add(childPath);

                    cache.putFile(childPath, child);
                }
            } while (request.getPageToken() != null && request.getPageToken().length() > 0);

            cache.putFolder(dir, list);
        }

        return list;
    }

    /** */
    private void removeEntry(Path path) throws IOException {
        final File entry = cache.getEntry(path);

        if (isFolder(entry)) {
            // TODO use cache ???
            List<File> list = drive.files().list()
                    .setQ("'" + entry.getId() + "' in parents and trashed=false")
                    .execute().getFiles();

            if (list != null && list.size() > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        drive.files().delete(entry.getId()).execute();

        cache.removeEntry(path);
    }

    /** */
    private void copyEntry(final Path source, final Path target, Set<CopyOption> options) throws IOException {
        final File sourceEntry = cache.getEntry(source);
        File targetParentEntry = cache.getEntry(target.toAbsolutePath().getParent());
        if (!isFolder(sourceEntry)) {
            File entry = new File();
            entry.setName(toFilenameString(target));
            entry.setParents(Arrays.asList(targetParentEntry.getId()));
            if (options != null && options.stream().anyMatch(o -> o.equals(GoogleDriveCopyOption.EXPORT_AS_GDOCS))) {
                entry.setMimeType(GoogleDriveCopyOption.EXPORT_AS_GDOCS.getValue());
            }
            File newEntry = drive.files().copy(sourceEntry.getId(), entry).setFields(ENTRY_FIELDS).execute();

            cache.addEntry(target, newEntry);
        } else {
            // TODO java spec. allows empty folder
            throw new UnsupportedOperationException("source can not be a folder");
        }
    }

    /**
     * @param targetIsParent if the target is folder
     */
    private void moveEntry(final Path source, final Path target, boolean targetIsParent) throws IOException {
        File sourceEntry = cache.getEntry(source);
        File targetParentEntry = cache.getEntry(targetIsParent ? target : target.toAbsolutePath().getParent());
        if (!isFolder(sourceEntry)) {
            File entry = new File();
            entry.setName(targetIsParent ? toFilenameString(source) : toFilenameString(target));
            String previousParents = null;
            if (sourceEntry.getParents() != null) {
                previousParents = String.join(",", sourceEntry.getParents());
            }
            File newEntry = drive.files().update(sourceEntry.getId(), entry)
                    .setAddParents(targetParentEntry.getId())
                    .setRemoveParents(previousParents)
                    .setFields(ENTRY_FIELDS).execute();
            cache.removeEntry(source);
            if (targetIsParent) {
                cache.addEntry(target.resolve(source.getFileName()), newEntry);
            } else {
                cache.addEntry(target, newEntry);
            }
        } else if (isFolder(sourceEntry)) {
            File dirEntry = new File();
            dirEntry.setName(toFilenameString(target));
            dirEntry.setMimeType(MIME_TYPE_DIR);
            String previousParents = null;
            if (sourceEntry.getParents() != null) {
                previousParents = String.join(",", sourceEntry.getParents());
            }
            File newEntry = drive.files().update(sourceEntry.getId(), dirEntry)
                    .setAddParents(targetParentEntry.getId())
                    .setRemoveParents(previousParents)
                    .setFields(ENTRY_FIELDS).execute();
            cache.moveEntry(source, target, newEntry);
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException {
        File sourceEntry = cache.getEntry(source);
        File entry = new File();
        entry.setName(toFilenameString(target));
        File newEntry = drive.files().update(sourceEntry.getId(), entry).setFields(ENTRY_FIELDS).execute();
        cache.removeEntry(source);
        cache.addEntry(target, newEntry);
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
