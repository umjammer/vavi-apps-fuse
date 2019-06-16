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
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import vavi.nio.file.Cache;
import vavi.nio.file.UploadMonitor;
import vavi.nio.file.Util;

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
        {
            // TODO datetime
//          File root = drive.files().get("fileId=root").execute();
            entryCache.put("/", new File().setName("/").setId("root").setMimeType(MIME_TYPE_DIR).setModifiedTime(new DateTime(0)).setSize(0L));
        }

        /**
         * TODO when the parent is not cached
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

                    File entry = drive.files().get(toPathString(path)).execute(); // TODO
//System.err.println("GOT: path: " + path + ", id: " + entry.getId());
                    cache.putFile(path, entry);
                    return entry;
                }
            } catch (GoogleJsonResponseException e) {
                if (e.getMessage().startsWith("404")) {
                    // TODO when a deep directory is specified at first, like '/Books/Novels'
                    // cache
                    removeEntry(path);

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

        final java.io.File downloadFile = java.io.File.createTempFile("vavi-apps-fuse-", ".download");

        return new GoogleDriveInputStream(drive, entry, downloadFile);
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
options.forEach(o -> { System.err.println("newOutputStream: " + o); });
        }

        // TODO mime type
        if (options.stream().anyMatch(o -> GoogleDriveCopyOption.class.isInstance(o))) {
            String mimeType = options.stream()
                    .filter(o -> GoogleDriveCopyOption.class.isInstance(o))
                    .map(o -> GoogleDriveCopyOption.class.cast(o).getMimeType()).findFirst().get();
        }

        java.io.File temp = java.io.File.createTempFile("vavi-apps-fuse-", ".upload");

        uploadMonitor.start(path);
        return new GoogleDriveOutputStream(drive, temp, toFilenameString(path), newEntry -> {
            try {
                uploadMonitor.finish(path);
                lock = null;
System.out.println("file: " + newEntry.getName() + ", " + newEntry.getCreatedTime() + ", " + newEntry.size());

                cache.addEntry(path, newEntry);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir));
    }

    Object lock;

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
options.forEach(o -> { System.err.println("newByteChannel: " + o); });
if (lock != null) {
    System.err.println("newByteChannel: locked: " + path);
    return null;
}
        if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
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
        File newEntry = drive.files().create(dirEntry)
                .setFields("id, parents, name, size, mimeType, createdTime").execute();

        cache.addEntry(dir, newEntry);
    }

    @Override
    public void delete(final Path path) throws IOException {
        final File entry = cache.getEntry(path);

        if (isFolder(entry)) {
            // TODO use cache
            List<File> list = drive.files().list()
                    .setQ("'" + entry.getId() + "' in parents and trashed=false")
                    .setFields("nextPageToken").execute().getFiles();

            if (list.size() > 0) {
                throw new DirectoryNotEmptyException(toPathString(path));
            }
        }

        drive.files().delete(entry.getId()).execute();

        cache.removeEntry(path);
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        File targetEntry;
        String targetFilename;
        try {
            targetEntry = cache.getEntry(target);
            if (!isFolder(targetEntry)) {
                drive.files().delete(targetEntry.getId()).execute();

                cache.removeEntry(target);

                targetEntry = cache.getEntry(target.getParent());
                targetFilename = toFilenameString(target);
            } else {
                targetFilename = toFilenameString(source);
            }
        } catch (NoSuchFileException e) {
            targetEntry = cache.getEntry(target.getParent());
            targetFilename = toFilenameString(source);
        }

        // 2.
        final File sourceEntry = cache.getEntry(source);
        if (!isFolder(sourceEntry)) {
            File entry = new File();
            entry.setName(targetFilename);
            entry.setParents(Arrays.asList(new String[] { targetEntry.getId() }));
            File newEntry = drive.files().copy(sourceEntry.getId(), entry)
                        .setFields("id, parents, name, size, mimeType, createdTime").execute();

            cache.addEntry(target, newEntry);
        } else {
            throw new UnsupportedOperationException("source can not be a folder");
        }
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        File targetEntry;
        String targetFilename;
        try {
            targetEntry = cache.getEntry(target);
            if (!isFolder(targetEntry)) {
                drive.files().delete(targetEntry.getId()).execute();

                cache.removeEntry(target);

                targetEntry = cache.getEntry(target.getParent());
                targetFilename = toFilenameString(target);
            } else {
                targetFilename = toFilenameString(source);
            }
        } catch (NoSuchFileException e) {
            targetEntry = cache.getEntry(target.getParent());
            targetFilename = toFilenameString(target);
        }

        // 2.
        File sourceEntry = cache.getEntry(source);
        StringBuilder previousParents = new StringBuilder();
        for (String parent: sourceEntry.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }
        File entry = new File();
        entry.setName(targetFilename);
        File newEntry = drive.files().update(sourceEntry.getId(), entry)
                .setAddParents(targetEntry.getId())
                .setRemoveParents(previousParents.toString())
                .setFields("id, parents, name, size, mimeType, createdTime").execute();

        cache.removeEntry(source);
        cache.addEntry(target, newEntry);
//System.out.println("target.parent: " + target.getParent() + ", " + folderCache.get(toString(target.getParent())));
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
        final String pathString = toPathString(path);
        final File entry = cache.getEntry(path);

        if (isFolder(entry)) {
            return;
        }

        // TODO: assumed; not a file == directory
        for (final AccessMode mode : modes) {
            if (mode == AccessMode.EXECUTE) {
                throw new AccessDeniedException(pathString);
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
//System.err.println("child: " + childPath.toRealPath().toString());

                    cache.putFile(childPath, child);
                }
            } while (request.getPageToken() != null && request.getPageToken().length() > 0);

//System.out.println("put folderCache: " + pathString);
            cache.putFolder(dir, list);
        }

        return list;
    }
}
