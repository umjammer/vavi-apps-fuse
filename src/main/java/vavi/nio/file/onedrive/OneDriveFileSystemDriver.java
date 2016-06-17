/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.File;
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
import java.text.Normalizer;
import java.text.Normalizer.Form;
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

import org.json.simple.parser.ParseException;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

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

    /** <NFC normalized path {@link String}, {@link OneItem}> */
    private Map<String, OneItem> cache = new HashMap<>(); // TODO refresh
    
    /**
     * TODO when the parent is not cached
     * @see #ignoreAppleDouble
     */
    private OneItem getOneItem(Path path) throws OneDriveException, IOException {
        String pathString = toString(path);
        if (cache.containsKey(pathString)) {
//System.err.println("CACHE: path: " + path + ", id: " + cache.get(pathString).getId());
            return cache.get(pathString);
        } else {
            if (ignoreAppleDouble && path.getFileName() != null && isAppleDouble(path)) {
                throw new NoSuchFileException("ignore apple double file: " + path);
            }

            OneItem entry = client.getItemByPath(pathString);
//System.err.println("GOT: path: " + path + ", id: " + entry.getId());
            cache.put(pathString, entry);
            return entry;
        }
    }

    /** */
    private String toString(Path path) throws IOException {
        return Normalizer.normalize(path.toRealPath().toString(), Form.NFC);
    }

    /** */
    private String toFilenameString(Path path) throws IOException {
        return Normalizer.normalize(path.toRealPath().getFileName().toString(), Form.NFC);
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
    
    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        try {
            final OneItem entry = getOneItem(path);

            // TODO: metadata driver
            if (OneFolder.class.isInstance(entry))
                throw new IsDirectoryException("path: " + path);

            final OneDownloadFile downloader = OneFile.class.cast(entry).download(File.createTempFile("vavi-apps-fuse-", ".download"));
            downloader.startDownload();

            return new OneDriveInputStream(downloader);
        } catch (OneDriveException e) {
            throw new OneDriveIOException("path: " + path, e);
        }
    }

    /** NFC normalized {@link String} */
    private Set<String> uploadFlags = new HashSet<>();
    
    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        try {
            String pathString = toString(path);
            final OneItem entry;
            try {
                entry = getOneItem(path);

                if (OneFolder.class.isInstance(entry))
                    throw new IsDirectoryException("path: " + path);
                else
                    throw new FileAlreadyExistsException("path: " + path);
            } catch (OneDriveException e) {
                System.err.println("newOutputStream: " + e.getMessage());
            }

            OneFolder dirEntry = (OneFolder) getOneItem(path.getParent());

            final OneUploadFile uploader = dirEntry.uploadFile(File.createTempFile("vavi-apps-fuse-", ".upload"), toFilenameString(path));

            uploadFlags.add(pathString);
            return new OneDriveOutputStream(uploader, file -> {
                try {
                    uploadFlags.remove(pathString);
                    // cache
                    cache.put(pathString, OneItem.class.cast(file));
                    folderCache.get(toString(path.getParent())).add(path);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (OneDriveException e) {
            throw new OneDriveIOException(e);
        }
    }

    /** <NFC normalized path {@link String}, {@link Path}> */
    private Map<String, List<Path>> folderCache = new HashMap<>(); // TODO refresh
    
    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        try {
            String pathString = toString(dir);
            final OneItem entry = getOneItem(dir);

            if (!OneFolder.class.isInstance(entry))
                throw new NotDirectoryException("dir: " + dir);
    
            List<Path> list = null;
            if (folderCache.containsKey(pathString)) {
                list = folderCache.get(pathString);
            } else {
                final List<OneItem> children = OneFolder.class.cast(entry).getChildren();
                list = new ArrayList<>(children.size());
                
                for (final OneItem child : children) {
                    Path childPath = dir.resolve(child.getName());
                    list.add(childPath);
//System.err.println("child: " + childPath.toRealPath().toString());
                    // cache
                    cache.put(toString(childPath), child);
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
        } catch (OneDriveException e) {
            throw new OneDriveIOException("dir: " + dir, e);
        }
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path,
                                              Set<? extends OpenOption> options,
                                              FileAttribute<?>... attrs) throws IOException {
        try {
            if (options.contains(StandardOpenOption.WRITE) || options.contains(StandardOpenOption.APPEND)) {
                final WritableByteChannel wbc = Channels.newChannel(newOutputStream(path, options));
                long leftover = 0;
                if (options.contains(StandardOpenOption.APPEND)) {
                    OneItem entry = getOneItem(path);
                    if (entry != null && OneFile.class.cast(entry).getSize() >= 0)
                        leftover = OneFile.class.cast(entry).getSize();
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
                OneItem entry = getOneItem(path);
                if (OneFolder.class.isInstance(entry))
                    throw new NoSuchFileException(path.toString());
                final ReadableByteChannel rbc = Channels.newChannel(newInputStream(path, null));
                final long size = OneFile.class.cast(entry).getSize();
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
        } catch (OneDriveException e) {
            throw new OneDriveIOException("path: " + path, e);
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        try {
            final String filenameString = toFilenameString(dir);
            OneItem parentEntry = getOneItem(dir.getParent());

            // TODO: how to diagnose?
            OneFolder dirEntry = OneFolder.class.cast(parentEntry).createFolder(filenameString);
            if (dirEntry == null)
                throw new OneDriveIOException("cannot create directory??");
            // cache
            cache.put(toString(dir), OneItem.class.cast(dirEntry));
            folderCache.get(toString(dir.getParent())).add(dir);
        } catch (OneDriveException e) {
            throw new OneDriveIOException("dir: "+ dir, e);
        }
    }

    @Override
    public void delete(final Path path) throws IOException {
        try {
            final String pathString = toString(path);
            final OneItem entry = getOneItem(path);

            // TODO: metadata!
            if (OneFolder.class.isInstance(entry)) {
                // TODO use cache
                List<OneItem> list = client.getFolderByPath(pathString).getChildren();

                if (list.size() > 0)
                    throw new DirectoryNotEmptyException(pathString);
            }

            if (!entry.delete()) {
                throw new OneDriveIOException("cannot delete ??");
            }
            // cache
            cache.remove(pathString);
            folderCache.get(toString(path.getParent())).remove(path);
        } catch (OneDriveException e) {
            throw new OneDriveIOException("path: " + path, e);
        }
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        try {
            final String targetString = target.toRealPath().toString();
            OneItem targetEntry = getOneItem(target);

            if (OneFolder.class.isInstance(targetEntry)) {
                // TODO use cache
                final List<OneItem> list = client.getFolderByPath(targetString).getChildren();
    
                if (list.size() > 0)
                    throw new DirectoryNotEmptyException("path: " + target);
            }

            // TODO: unknown what happens when a copy operation is performed
            targetEntry.delete();
            targetEntry = OneItem.class.cast(targetEntry.getParentFolder());

            // cache
            cache.remove(toString(target));
            folderCache.get(toString(target.getParent())).remove(toString(target));

            // 2.
            final OneItem sourceEntry = getOneItem(source);

            // TODO: how to diagnose?
            if (OneFile.class.isInstance(sourceEntry)) {
                OneFile newEntry = OneFile.class.cast(sourceEntry).copy(OneFolder.class.cast(targetEntry));
                if (newEntry == null)
                    throw new OneDriveIOException("cannot copy??");

                // cache
                cache.put(toString(target), OneItem.class.cast(newEntry));
                String pathString = toString(target.getParent());
                List<Path> paths = folderCache.get(pathString);
                if (paths == null) {
                    paths = new ArrayList<>();
                    folderCache.put(pathString, paths);
                }
                paths.add(target);
            } else if (OneFolder.class.isInstance(sourceEntry)) {
                throw new UnsupportedOperationException("source can not be a folder");
            }
        } catch (ParseException | InterruptedException e) {
            throw new OneDriveIOException(e);
        } catch (OneDriveException e) {
            throw new OneDriveIOException("source: "+  source + ", target: " + target, e);
        }
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        try {
            final String targetString = source.toRealPath().toString();
            OneItem targetEntry = getOneItem(target);

            if (OneFolder.class.isInstance(targetEntry)) {
                // TODO use cache
                final List<OneItem> list = client.getFolderByPath(targetString).getChildren();
    
                if (list.size() > 0)
                    throw new DirectoryNotEmptyException(targetString);
            }
            // TODO: unknown what happens when a move operation is performed
            // and the target already exists
            targetEntry.delete();
            targetEntry = OneItem.class.cast(targetEntry.getParentFolder());

            // cache
            cache.remove(toString(target));
            folderCache.get(toString(target.getParent())).remove(toString(target));

            // 2.
            final OneItem sourceEntry = getOneItem(source);

            // TODO: how to diagnose?
            if (OneFile.class.isInstance(sourceEntry)) {
                OneFile newEntry = OneFile.class.cast(sourceEntry).move(OneFolder.class.cast(targetEntry));
                if (newEntry == null)
                    throw new OneDriveIOException("cannot copy??");

                // cache
                cache.remove(toString(source));
                folderCache.get(toString(source.getParent())).remove(source);

                cache.put(toString(target), OneItem.class.cast(newEntry));
        //System.out.println("target.parent: " + target.getParent() + ", " + folderCache.get(toString(target.getParent())));
                String pathString = toString(target.getParent());
                List<Path> paths = folderCache.get(pathString);
                if (paths == null) {
                    paths = new ArrayList<>();
                    folderCache.put(pathString, paths);
                }
                paths.add(target);
            } else if (OneFolder.class.isInstance(sourceEntry)) {
                throw new UnsupportedOperationException("source can not be a folder");
            }
        } catch (ParseException | InterruptedException e) {
            throw new OneDriveIOException(e);
        } catch (OneDriveException e) {
            throw new OneDriveIOException("source: " + source + ", target: " + target, e);
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
        try {
            final String pathString = toString(path);
            final OneItem entry = getOneItem(path);

            if (!OneFile.class.isInstance(entry))
                return;
    
            // TODO: assumed; not a file == directory
            for (final AccessMode mode : modes)
                if (mode == AccessMode.EXECUTE)
                    throw new AccessDeniedException(pathString);
        } catch (OneDriveException e) {
            throw (IOException) new NoSuchFileException("path: " + path).initCause(e);
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
        try {
if (uploadFlags.contains(toString(path))) {
System.out.println("uploading...");
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
            return getOneItem(path);
        } catch (OneDriveException e) {
            throw (IOException) new NoSuchFileException("path: " + path).initCause(e);
        }
    }
}
