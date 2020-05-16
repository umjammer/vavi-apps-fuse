/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

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
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.json.simple.parser.ParseException;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Cache;
import vavi.nio.file.UploadMonitor;
import vavi.nio.file.Util;

import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneItem;
import de.tuberlin.onedrivesdk.downloadFile.OneDownloadFile;
import de.tuberlin.onedrivesdk.file.OneFile;
import de.tuberlin.onedrivesdk.folder.OneFolder;
import de.tuberlin.onedrivesdk.uploadFile.OneUploadFile;


/**
 * OneDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class OneDriveFileSystemDriver extends UnixLikeFileSystemDriverBase {

    private final OneDriveSDK client;

    private boolean ignoreAppleDouble = false;

    @SuppressWarnings("unchecked")
    public OneDriveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final OneDriveSDK client,
            final Map<String, ?> env) {
        super(fileStore, provider);
        this.client = client;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
    }

    /** */
    private Cache<OneItem> cache = new Cache<OneItem>() {
        /**
         * TODO when the parent is not cached
         * @see #ignoreAppleDouble
         * @throws NoSuchFileException must be thrown when the path is not found in this cache
         */
        public OneItem getEntry(Path path) throws IOException {
            try {
                if (cache.containsFile(path)) {
                    return cache.getFile(path);
                } else {
                    if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                        throw new NoSuchFileException("ignore apple double file: " + path);
                    }

                    OneItem entry = client.getItemByPath(toPathString(path));
                    cache.putFile(path, entry);
                    return entry;
                }
            } catch (OneDriveException e) {
                // TODO focus only file not found
                // cache
                if (cache.containsFile(path)) {
                    cache.removeEntry(path);
                }

                throw (IOException) new NoSuchFileException("path: " + path).initCause(e);
            }
        }
    };

    /** */
    private UploadMonitor uploadMonitor = new UploadMonitor();

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        try {
            final OneItem entry = cache.getEntry(path);
            // TODO: metadata driver
            if (entry.isFolder()) {
                throw new IsDirectoryException("path: " + path);
            }

            final OneDownloadFile downloader = OneFile.class.cast(entry).download(File.createTempFile("vavi-apps-fuse-", ".download"));
            downloader.startDownload();
            return new FileInputStream(downloader.getDownloadedFile());
        } catch (OneDriveException e) {
            throw new IOException("path: " + path, e);
        }
    }

    /**
     * fuse からだと
     * <ol>
     *  <li>create -> newByteChannel
     *  <li>flush -> n/a
     *  <li>lock -> n/a
     *  <li>release -> byteChannel.close
     * </ol>
     * と呼ばれる <br/>
     * 元のファイルが取れない... <br/>
     * 書き込みの指示もない...
     * <p>
     * nio.file からだと
     * <pre>
     *  newOutputStream -> write(2)
     * </pre>
     */
    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final OneItem entry;
        try {
            entry = cache.getEntry(path);

            if (entry.isFolder()) {
                throw new IsDirectoryException("path: " + path);
            } else {
                throw new FileAlreadyExistsException("path: " + path);
            }
        } catch (NoSuchFileException e) {
System.out.println("newOutputStream: " + e.getMessage());
//new Exception("*** DUMMY ***").printStackTrace();
        }

        OneFolder dirEntry = (OneFolder) cache.getEntry(path.getParent());
        uploadMonitor.start(path);
        return new Util.OutputStreamForUploading() {
            @Override
            protected void upload(InputStream is) throws IOException {
                try {
                    Path tmp = Files.createTempFile("vavi-apps-fuse-", ".upload");
                    Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                    final OneUploadFile uploader = dirEntry.uploadFile(tmp.toFile(), toFilenameString(path));
                    OneFile newEntry = uploader.startUpload();
                    cache.addEntry(path, OneItem.class.cast(newEntry));
                    uploadMonitor.finish(path);
                } catch (OneDriveException e) {
e.printStackTrace();
                    throw new IOException(e);
                }
            }
        };
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        try {
            return Util.newDirectoryStream(getDirectoryEntries(dir), filter);
        } catch (OneDriveException e) {
            throw new IOException("dir: " + dir, e);
        }
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
                        OneItem entry = cache.getEntry(path);
                        if (entry != null && OneFile.class.cast(entry).getSize() >= 0) {
                            leftover = OneFile.class.cast(entry).getSize();
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
            OneItem entry = cache.getEntry(path);
            if (entry.isFolder()) {
                throw new IsDirectoryException(path.toString());
            }
            return new Util.SeekableByteChannelForReading(newInputStream(path, null)) {
                @Override
                protected long getSize() throws IOException {
                    return OneFile.class.cast(entry).getSize();
                }
            };
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        try {
            OneItem parentEntry = cache.getEntry(dir.getParent());

            // TODO: how to diagnose?
            OneFolder dirEntry = OneFolder.class.cast(parentEntry).createFolder(toFilenameString(dir));

            cache.addEntry(dir, OneItem.class.cast(dirEntry));
        } catch (OneDriveException e) {
            throw new IOException("dir: "+ dir, e);
        }
    }

    @Override
    public void delete(final Path path) throws IOException {
        try {
            removeEntry(path);
        } catch (OneDriveException e) {
            throw new IOException("path: " + path, e);
        }
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        try {
            if (cache.existsEntry(target)) {
                if (options != null && options.stream().anyMatch(o -> o.equals(StandardCopyOption.REPLACE_EXISTING))) {
                    removeEntry(target);
                } else {
                    throw new FileAlreadyExistsException(target.toString());
                }
            }
            copyEntry(source, target);
        } catch (OneDriveException e) {
e.printStackTrace();
            throw new IOException("source: "+  source + ", target: " + target, e);
        }
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        try {
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
        } catch (OneDriveException e) {
e.printStackTrace();
            throw new IOException("source: " + source + ", target: " + target, e);
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
        final OneItem entry = cache.getEntry(path);

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
System.out.println("uploading... (meta): " + toPathString(path));
    return new OneItem() {
        public String getId() {
            return "-1";
        }
        public String getName() {
            return path.getFileName().toString();
        }
        public long getSize() {
            return 0;
        }
        public long getLastModifiedDateTime() {
            return 0;
        }
        public boolean isFile() {
            return true;
        }
        public boolean isFolder() {
            return false;
        }
    };
}
        return cache.getEntry(path);
    }

    /** */
    private List<Path> getDirectoryEntries(Path dir) throws IOException, OneDriveException {
        final OneItem entry = cache.getEntry(dir);

        if (!entry.isFolder()) {
            throw new NotDirectoryException(dir.toString());
        }

        List<Path> list = null;
        if (cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
            final List<OneItem> children = OneFolder.class.cast(entry).getChildren();
            list = new ArrayList<>(children.size());

            for (final OneItem child : children) {
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
    private void removeEntry(Path path) throws IOException, OneDriveException {
        OneItem entry = cache.getEntry(path);
        if (entry.isFolder()) {
            // TODO use cache
            final List<OneItem> children = client.getFolderByPath(toPathString(path)).getChildren();

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
    private void copyEntry(final Path source, final Path target) throws IOException, OneDriveException {
        try {
            OneItem sourceEntry = cache.getEntry(source);
            OneItem targetParentEntry = cache.getEntry(target.getParent());
            if (sourceEntry.isFile()) {
                OneFile newEntry = OneFile.class.cast(sourceEntry).copy(OneFolder.class.cast(targetParentEntry));

                cache.addEntry(target, OneItem.class.cast(newEntry));
            } else if (sourceEntry.isFolder()) {
                throw new IsDirectoryException("source can not be a folder: " + source);
            }
        } catch (ParseException | InterruptedException e) {
            throw new IOException(e);
        }
    }

    /**
     * @param targetIsParent if the target is folder
     */
    private void moveEntry(final Path source, final Path target, boolean targetIsParent) throws IOException, OneDriveException {
        try {
            OneItem sourceEntry = cache.getEntry(source);
            OneItem targetParentEntry = cache.getEntry(targetIsParent ? target : target.getParent());
            if (sourceEntry.isFile()) {
                OneFile newEntry = OneFile.class.cast(sourceEntry).move(OneFolder.class.cast(targetParentEntry));
                cache.removeEntry(source);
                if (targetIsParent) {
                    cache.addEntry(target.resolve(source.getFileName()), OneItem.class.cast(newEntry));
                } else {
                    cache.addEntry(target, OneItem.class.cast(newEntry));
                }
            } else if (sourceEntry.isFolder()) {
                throw new IsDirectoryException("source can not be a folder: " + source);
            }
        } catch (ParseException | InterruptedException e) {
            throw new IOException(e);
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException, OneDriveException {
        try {
            OneItem sourceEntry = cache.getEntry(source);
            OneItem targetEntry = cache.getEntry(target.getParent());

            OneItem newEntry = sourceEntry.rename(OneFolder.class.cast(targetEntry), toFilenameString(target));

            cache.removeEntry(source);
            cache.addEntry(target, newEntry);
        } catch (ParseException | InterruptedException e) {
            throw new IOException(e);
        }
    }
}
