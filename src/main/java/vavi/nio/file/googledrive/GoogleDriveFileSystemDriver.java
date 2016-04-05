/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import static java.text.Normalizer.normalize;
import static java.text.Normalizer.Form.NFC;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;


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
            final Map<String, ?> env) {
        super(fileStore, provider);
        this.drive = drive;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
        cache.put("/", new File().setName("/").setId("ROOT").setMimeType(MIME_TYPE_DIR).setModifiedByMeTime(new DateTime(0)).setSize(0L));
    }

    /** <{@link String}, {@link File}> */
    private Map<String, File> cache = new HashMap<>(); // TODO refresh
    
    /** @see #ignoreAppleDouble */
    private File getFile(Path path) throws IOException {
        String pathString = normalize(path.toRealPath().toString(), NFC);
        if (cache.containsKey(pathString)) {
//System.err.println("CACHE: path: " + path + ", id: " + cache.get(pathString).getId());
            return cache.get(pathString);
        } else {
            if (ignoreAppleDouble && path.getFileName() != null && isAppleDouble(path)) {
                throw new NoSuchFileException("ignore apple double file: " + path);
            }

            File item = drive.files().get(pathString).execute();
//System.err.println("GOT: path: " + path + ", id: " + item.getId());
            cache.put(pathString, item);
            return item;
        }
    }

    /** @see #ignoreAppleDouble */
    private boolean isAppleDouble(Path path) throws IOException {
//System.err.println("path.toRealPath(): " + path.toRealPath());
//System.err.println("path.getFileName(): " + path.getFileName());
        String filename = path.getFileName().toString();
        return filename.startsWith("._") ||
               filename.equals(".DS_Store") ||
               filename.equals(".localized") ||
               filename.equals(".hidden");
    }
    
    private static final String MIME_TYPE_DIR = "application/vnd.google-apps.folder";

    /** ugly */
    static boolean isFolder(File file) {
        return file.getMimeType().equals(MIME_TYPE_DIR);
    }
    
    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final File entry = getFile(path);

        // TODO: metadata driver
        if (isFolder(entry))
            throw new IsDirectoryException("path: " + path);

        final java.io.File uploadedFile = java.io.File.createTempFile("vavi-apps-fuse-", ".download");

        //
        return new GoogleDriveInputStream(drive, uploadedFile);
    }

    private Set<String> uploadFlags = new HashSet<>();
    
    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        String pathString = normalize(path.toRealPath().toString(), NFC);
        final File entry;
        try {
            entry = getFile(path);

            if (isFolder(entry))
                throw new IsDirectoryException("path: " + path);
            else
                throw new FileAlreadyExistsException("path: " + path);
        } catch (IOException e) {
            System.err.println("newOutputStream: " + e.getMessage());
        }

        File dirEntry = getFile(path.getParent());

        //
        java.io.File temp = java.io.File.createTempFile("vavi-apps-fuse-", ".upload");
        
        uploadFlags.add(pathString);
        return new GoogleDriveOutputStream(drive, temp, file -> {
            try {
                uploadFlags.remove(pathString);
                cache.put(pathString, file);
                folderCache.get(normalize(path.getParent().toRealPath().toString(), NFC)).add(path);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    /** <NFC normalized path {@link String}, {@link Path}> */
    private Map<String, List<Path>> folderCache = new HashMap<>(); // TODO refresh
    
    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        String pathString = normalize(dir.toRealPath().toString(), NFC);
        final File entry = getFile(dir);

        if (!isFolder(entry))
            throw new NotDirectoryException("dir: " + dir);

        List<Path> list = null;
        if (folderCache.containsKey(pathString)) {
            list = folderCache.get(pathString);
        } else {
            final List<File> children = drive.files().list().setFields("files(id, name, size, mimeType, createdTime, modifiedByMeTime)").execute().getFiles();
            list = new ArrayList<>(children.size());
            
            for (final File child : children) {
                Path childPath = dir.resolve(child.getName());
                list.add(childPath);
//System.err.println("child: " + childPath.toRealPath().toString());
                cache.put(normalize(childPath.toRealPath().toString(), NFC), child);
            }
            
            folderCache.put(pathString, list);
        }
        
        final List<Path> _list = list;

        //noinspection AnonymousInnerClassWithTooManyMethods
        return new DirectoryStream<Path>() {
            private final AtomicBoolean alreadyOpen = new AtomicBoolean(false);

            @Override
            public Iterator<Path> iterator() {
                // required by the contract
                if (alreadyOpen.getAndSet(true))
                    throw new IllegalStateException();
                return _list.iterator();
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs) throws IOException {
            if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
                final WritableByteChannel wbc = Channels.newChannel(newOutputStream(path, options));
                long leftover = 0;
                if (options.contains(StandardOpenOption.APPEND)) {
                    File entry = getFile(path);
                    if (entry != null && entry.getSize() >= 0)
                        leftover = entry.getSize();
                }
                final long offset = leftover;
                return new SeekableByteChannel() {
                    long written = offset;
    
                    public boolean isOpen() {
                        return wbc.isOpen();
                    }
    
                    public long position() throws IOException {
                        return written;
                    }
    
                    public SeekableByteChannel position(long pos) throws IOException {
                        throw new UnsupportedOperationException();
                    }
    
                    public int read(ByteBuffer dst) throws IOException {
                        throw new UnsupportedOperationException();
                    }
    
                    public SeekableByteChannel truncate(long size) throws IOException {
                        throw new UnsupportedOperationException();
                    }
    
                    public int write(ByteBuffer src) throws IOException {
System.err.println("here: X0");
                        int n = wbc.write(src);
                        written += n;
                        return n;
                    }
    
                    public long size() throws IOException {
                        return written;
                    }
    
                    public void close() throws IOException {
System.err.println("here: X1");
                    wbc.close();
                }
            };
        } else {
            File entry = getFile(path);
            if (isFolder(entry))
                throw new NoSuchFileException(path.toString());
            final ReadableByteChannel rbc = Channels.newChannel(newInputStream(path, null));
            final long size = entry.getSize();
            return new SeekableByteChannel() {
                long read = 0;

                public boolean isOpen() {
                    return rbc.isOpen();
                }

                public long position() throws IOException {
                    return read;
                }

                public SeekableByteChannel position(long pos) throws IOException {
                    read = pos;
                    return this;
                }

                public int read(ByteBuffer dst) throws IOException {
                    int n = rbc.read(dst);
                    if (n > 0) {
                        read += n;
                    }
                    return n;
                }

                public SeekableByteChannel truncate(long size) throws IOException {
                    throw new NonWritableChannelException();
                }

                public int write(ByteBuffer src) throws IOException {
                    throw new NonWritableChannelException();
                }

                public long size() throws IOException {
                    return size;
                }

                public void close() throws IOException {
                    rbc.close();
                }
            };
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        final String filenameString = dir.toRealPath().getFileName().toString();
        File parentEntry = getFile(dir.getParent());

        // TODO: how to diagnose?
        File dirEntry = new File();
        dirEntry.setName(filenameString);
        dirEntry.setMimeType(MIME_TYPE_DIR);
        if (drive.files().create(dirEntry).execute() == null)
            throw new GoogleDriveIOException("cannot create directory??");
        // cache
        cache.put(normalize(dir.toRealPath().toString(), NFC), dirEntry);
        folderCache.get(normalize(dir.getParent().toRealPath().toString(), NFC)).add(dir);
    }

    @Override
    public void delete(final Path path) throws IOException {
        final String pathString = normalize(path.toRealPath().toString(), NFC);
        final File item = getFile(path);

        // TODO: metadata!
        if (isFolder(item)) {
            // TODO use cache
            List<File> list = drive.files().list().set("parents", item.getId()).execute().getFiles();

            if (list.size() > 0)
                throw new DirectoryNotEmptyException(pathString);
        }

        drive.files().delete(item.getId()).execute();
        //throw new GoogleDriveIOException("cannot delete ??");
        // cache
        cache.remove(pathString);
        folderCache.get(normalize(path.getParent().toRealPath().toString(), NFC)).remove(path);
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        final String targetString = target.toRealPath().toString();
        File targetEntry = getFile(target);

        if (isFolder(targetEntry)) {
            // TODO use cache
            final List<File> list = drive.files().list().set("parents", targetEntry.getId()).execute().getFiles();

            if (list.size() > 0)
                throw new DirectoryNotEmptyException("path: " + target);
        }

        // TODO: unknown what happens when a copy operation is performed
        drive.files().delete(targetEntry.getId()).execute();
//        targetEntry = targetEntry.getParents();

        final File sourceEntry = getFile(source);

        // TODO: how to diagnose?
        if (!isFolder(sourceEntry)) {
            if (drive.files().copy(sourceEntry.getId(), targetEntry).execute() == null)
                throw new GoogleDriveIOException("cannot copy??");
        } else if (isFolder(sourceEntry)) {
            throw new UnsupportedOperationException("source can not be a folder");
        }
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        final String targetString = source.toRealPath().toString();
        File targetEntry = getFile(target);

        if (isFolder(targetEntry)) {
            // TODO use cache
            final List<File> list = drive.files().list().setFields("files(id, name, size, mimeType, createdTime, modifiedByMeTime)").execute().getFiles();

            if (list.size() > 0)
                throw new DirectoryNotEmptyException(targetString);
        }
        // TODO: unknown what happens when a move operation is performed
        // and the target already exists
        drive.files().delete(targetEntry.getId()).execute();
//        targetEntry = targetEntry.getParents();

        final File sourceEntry = getFile(source);

        // TODO: how to diagnose?
        if (!isFolder(sourceEntry)) {
            if (drive.files().update(sourceEntry.getId(), targetEntry) == null)
                throw new GoogleDriveIOException("cannot copy??");
        } else if (isFolder(sourceEntry)) {
            throw new UnsupportedOperationException("source can not be a folder");
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
        final String pathString = path.toRealPath().toString();
        final File entry = getFile(path);

        if (isFolder(entry))
            return;

        // TODO: assumed; not a file == directory
        for (final AccessMode mode : modes)
            if (mode == AccessMode.EXECUTE)
                throw new AccessDeniedException(pathString);
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
if (uploadFlags.contains(path.toRealPath().toString())) {
System.out.println("uploading...");
    return null;
}
        return getFile(path);
    }
}
