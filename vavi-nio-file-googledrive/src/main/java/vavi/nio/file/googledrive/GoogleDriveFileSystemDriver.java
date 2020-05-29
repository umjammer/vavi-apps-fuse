/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.AsynchronousFileChannel;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploader.UploadState;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import vavi.nio.file.Cache;
import vavi.nio.file.UploadMonitor;
import vavi.nio.file.Util;

import static com.rainerhahnekamp.sneakythrow.Sneaky.sneaked;
import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;


/**
 * GoogleDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class GoogleDriveFileSystemDriver extends UnixLikeFileSystemDriverBase {

    private final Drive drive;

    private boolean ignoreAppleDouble = false;

    @SuppressWarnings("unchecked")
    public GoogleDriveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final Drive drive,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.drive = drive;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
    }

    /** */
    private Cache<File> cache = new Cache<File>() {
        /**
         * @see #ignoreAppleDouble
         */
        public File getEntry(Path path) throws IOException {
            try {
                if (cache.containsFile(path)) {
//System.err.println("CACHE: path: " + path + ", id: " + cache.get(pathString).getId());
                    return cache.getFile(path);
                } else {
                    if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                        throw new NoSuchFileException("ignore apple double file: " + path);
                    }

                    File entry;
                    if (path.getNameCount() == 0) {
                        entry = drive.files().get("root").setFields("id, parents, size, mimeType, createdTime, modifiedTime").execute().set("name", "/");
//System.err.println(path + ", " + entry);
                        cache.putFile(path, entry);
                        return entry;
                    } else {
                        List<Path> siblings;
                        if (!cache.containsFolder(path.getParent())) {
                            siblings = getDirectoryEntries(path.getParent());
                        } else {
                            siblings = cache.getFolder(path.getParent());
                        }
                        for (int i = 0; i < siblings.size(); i++) { // avoid ConcurrentModificationException
                            Path p = siblings.get(i);
                            if (p.getFileName().equals(path.getFileName())) {
                                return cache.getEntry(p);
                            }
                        }
                        throw new NoSuchFileException(path.toString());
                    }
//System.err.println("GOT: path: " + path + ", id: " + entry.getId());
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

    /** */
    private UploadMonitor uploadMonitor = new UploadMonitor();

    /** */
    private static final String MIME_TYPE_DIR = "application/vnd.google-apps.folder";

    /** ugly */
    static boolean isFolder(File file) {
        return file.getMimeType().equals(MIME_TYPE_DIR);
    }

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final File entry = cache.getEntry(path);

        if (isFolder(entry)) {
            throw new IsDirectoryException("path: " + path);
        }

        // TODO detect automatically?
        if (options != null && options.stream().anyMatch(o -> GoogleDriveOpenOption.class.isInstance(o))) {
            GoogleDriveOpenOption option = GoogleDriveOpenOption.class
                    .cast(options.stream().filter(o -> GoogleDriveOpenOption.class.isInstance(o)).findFirst().get());
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
System.out.println("newOutputStream: " + e.getMessage());
if (options != null) {
 options.forEach(o -> { System.err.println("newOutputStream: " + o); });
}
        }

        // TODO detect automatically?
        if (options != null && options.stream().anyMatch(o -> GoogleDriveOpenOption.class.isInstance(o))) {
            @SuppressWarnings("unused")
            GoogleDriveOpenOption option = GoogleDriveOpenOption.class
                    .cast(options.stream().filter(o -> GoogleDriveOpenOption.class.isInstance(o)).findFirst().get());
        }

        return new Util.OutputStreamForUploading() {

            @Override
            protected void upload(InputStream is) throws IOException {
                File fileMetadata = new File();
                fileMetadata.setName(toFilenameString(path));
                fileMetadata.setParents(Arrays.asList(cache.getEntry(path.getParent()).getId()));

                InputStreamContent mediaContent = new InputStreamContent(null, is);
                mediaContent.setLength(is.available());

                Drive.Files.Create creator = drive.files().create(fileMetadata, mediaContent);
                MediaHttpUploader uploader = creator.getMediaHttpUploader();
                uploader.setDirectUploadEnabled(true);
                uploader.setProgressListener(u -> { System.err.println("upload progress: " + u.getProgress()); });
                uploadMonitor.start(path);
                final File newEntry = creator.setFields("id, name, size, parents, mimeType, createdTime").execute(); // TODO file is not finished status!

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.submit(sneaked(() -> {
                    long timeout = 0;
                    long delay = 100;
                    try {
System.err.println("executorService: " + uploader.getProgress() + ", " + uploader.getUploadState());
                        while ((uploader.getUploadState() != UploadState.MEDIA_COMPLETE || uploader.getProgress() < 1) && timeout < 10 * 1000) {
System.err.println("executorService: " + uploader.getProgress() + ", " + uploader.getUploadState() + ", " + timeout);
                            Thread.sleep(delay);
                            timeout += delay;
                            delay *= 2;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    uploadMonitor.finish(path);
                    lock = null;
System.out.printf("file: %1$s, %2$tF %2$tT.%2$tL, %3$d\n", newEntry.getName(), newEntry.getCreatedTime().getValue(), newEntry.size());

                    cache.addEntry(path, newEntry);
                }));
            }
        };
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir), filter);
    }

    /** */
    private Object lock;

    @Override
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path,
                                                              Set<? extends OpenOption> options,
                                                              ExecutorService executor,
                                                              FileAttribute<?>... attrs)
        throws IOException
    {
        if (uploadMonitor.isUploading(path)) {
            lock = new Object();
System.err.println("newAsynchronousFileChannel: " + path);
        }
        return null;
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs) throws IOException {
new Exception("*** DUMMY ***").printStackTrace();
if (options != null) {
 options.forEach(o -> { System.err.println("newByteChannel: " + o); });
}
if (lock != null) {
    System.err.println("newByteChannel: locked: " + path);
    return null;
}
        if (options != null && (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND))) {
            return new Util.SeekableByteChannelForWriting(newOutputStream(path, options)) {
                @Override
                protected long getLeftOver() throws IOException {
                    long leftover = 0;
                    if (options.contains(StandardOpenOption.APPEND)) {
                        File entry = cache.getEntry(path);
                        if (entry != null && entry.getSize() >= 0) {
                            leftover = entry.getSize();
                        }
                    }
                    return leftover;
                }

                @Override
                public void close() throws IOException {
                    if (lock != null) {
System.out.println("SeekableByteChannelForWriting::close: scpecial: " + path);
                        return;
                    }
                    super.close();
                }
            };
        } else {
            File entry = cache.getEntry(path);
            if (isFolder(entry)) {
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
        File dirEntry = new File();
        dirEntry.setName(toFilenameString(dir));
        dirEntry.setMimeType(MIME_TYPE_DIR);
        if (dir.getParent().getFileName() != null) {
            dirEntry.setParents(Arrays.asList(cache.getEntry(dir.getParent()).getId()));
        }
        File newEntry = drive.files().create(dirEntry)
                .setFields("id, parents, name, size, mimeType, createdTime").execute();
//Debug.println("createDirectory: " + dir + ", " + isFolder(newEntry) + ", " + newEntry.hashCode());
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
        // TODO: what to do here? GoogleDrive does not implement Closeable :(
    }

    /**
     * @throws IOException if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    public Object getPathMetadata(final Path path) throws IOException {
//if (uploadMonitor.isUploading(path)) {
//System.out.println("getPathMetadata: uploading...");
//    return new File().setName(toFilenameString(path)).setMimeType("");
//}

        return cache.getEntry(path);
    }

    /** */
    private List<Path> getDirectoryEntries(final Path dir) throws IOException {
        final File entry = cache.getEntry(dir);

//Debug.println("getDirectoryEntries: " + dir + ", " + isFolder(entry) + ", " + entry.hashCode());
        if (!isFolder(entry)) {
            throw new NotDirectoryException("dir: " + dir);
        }

        List<Path> list = new ArrayList<>();
        if (cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
            Files.List request = drive.files().list();
            do {
                FileList files = request
                        .setQ("'" + entry.getId() + "' in parents and trashed=false")
                        .setFields("nextPageToken, files(id, parents, name, size, mimeType, createdTime, modifiedTime)").execute();
                final List<File> children = files.getFiles();
                request.setPageToken(files.getNextPageToken());

                for (final File child : children) {
                    Path childPath = dir.resolve(child.getName());
                    list.add(childPath);
//System.err.println("child: " + childPath + ", " + child.getId() + ", folder: " + isFolder(child));

                    cache.putFile(childPath, child);
                }
            } while (request.getPageToken() != null && request.getPageToken().length() > 0);

//System.out.println("put folderCache: " + pathString);
            cache.putFolder(dir, list);
        }

        return list;
    }

    /** */
    private void removeEntry(Path path) throws IOException {
        final File entry = cache.getEntry(path);

        if (isFolder(entry)) {
            // TODO use cache
            List<File> list = drive.files().list()
                    .setQ("'" + entry.getId() + "' in parents and trashed=false")
                    .setFields("nextPageToken").execute().getFiles();

            if (list != null && list.size() > 0) {
                throw new DirectoryNotEmptyException(toPathString(path));
            }
        }

        drive.files().delete(entry.getId()).execute();

        cache.removeEntry(path);
    }

    /** */
    private void copyEntry(final Path source, final Path target, Set<CopyOption> options) throws IOException {
        final File sourceEntry = cache.getEntry(source);
        File targetParentEntry = cache.getEntry(target.getParent());
        if (!isFolder(sourceEntry)) {
            File entry = new File();
            entry.setName(toFilenameString(target));
            entry.setParents(Arrays.asList(targetParentEntry.getId()));
            if (options != null && options.stream().anyMatch(o -> o.equals(GoogleDriveCopyOption.EXPORT_AS_GDOCS))) {
                entry.setMimeType(GoogleDriveCopyOption.EXPORT_AS_GDOCS.getValue());
            }
            File newEntry = drive.files().copy(sourceEntry.getId(), entry)
                        .setFields("id, parents, name, size, mimeType, createdTime").execute();

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
        File targetParentEntry = cache.getEntry(targetIsParent ? target : target.getParent());
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
                    .setFields("id, parents, name, size, mimeType, createdTime").execute();
            cache.removeEntry(source);
            if (targetIsParent) {
                cache.addEntry(target.resolve(source.getFileName()), newEntry);
            } else {
                cache.addEntry(target, newEntry);
            }
        } else if (isFolder(sourceEntry)) {
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException {
        File sourceEntry = cache.getEntry(source);
        File entry = new File();
        entry.setName(toFilenameString(target));
        File newEntry = drive.files().update(sourceEntry.getId(), entry)
                .setFields("id, name, size, mimeType, createdTime").execute();
        cache.removeEntry(source);
        cache.addEntry(target, newEntry);
    }
}