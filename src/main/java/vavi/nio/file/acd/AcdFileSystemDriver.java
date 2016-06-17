/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

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

import org.yetiz.lib.acd.ACD;
import org.yetiz.lib.acd.ACDSession;
import org.yetiz.lib.acd.Entity.FileInfo;
import org.yetiz.lib.acd.Entity.FolderInfo;
import org.yetiz.lib.acd.Entity.NodeInfo;
import org.yetiz.lib.acd.api.v1.Nodes;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;


/**
 * AcdFileSystemDriver. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class AcdFileSystemDriver extends UnixLikeFileSystemDriverBase {

    private final ACD drive;
    private ACDSession session;

    private boolean ignoreAppleDouble = false;

    @SuppressWarnings("unchecked")
    public AcdFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final ACD drive,
            final ACDSession session,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.drive = drive;
        this.session = session;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
        NodeInfo root = Nodes.getRootFolder(session);
        cache.put("/", root);
    }

    /** <NFC normalized path {@link String}, {@link NodeInfo}> */
    private Map<String, NodeInfo> cache = new HashMap<>(); // TODO refresh
    
    /**
     * TODO when the parent is not cached
     * @see #ignoreAppleDouble
     */
    private NodeInfo getFile(Path path) throws IOException {
        String pathString = toString(path);
        if (cache.containsKey(pathString)) {
//System.err.println("CACHE: path: " + path + ", id: " + cache.get(pathString).getId());
            return cache.get(pathString);
        } else {
            if (ignoreAppleDouble && path.getFileName() != null && isAppleDouble(path)) {
                throw new NoSuchFileException("ignore apple double file: " + path);
            }

            NodeInfo entry = Nodes.getFileMetadata(session, pathString); // TODO
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
        final NodeInfo entry = getFile(path);

        if (entry.isFolder())
            throw new IsDirectoryException("path: " + path);

        return drive.getFile(entry.getId());
    }

    /** NFC normalized {@link String} */
    private Set<String> uploadFlags = new HashSet<>();
    
    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        String pathString = toString(path);
        final NodeInfo entry;
        try {
            entry = getFile(path);

            if (entry.isFolder())
                throw new IsDirectoryException("path: " + path);
            else
                throw new FileAlreadyExistsException("path: " + path);
        } catch (IOException e) {
            System.err.println("newOutputStream: " + e.getMessage());
        }

        java.io.File temp = java.io.File.createTempFile("vavi-apps-fuse-", ".upload");
        
        uploadFlags.add(pathString);
        return new AcdOutputStream(drive, temp, toFilenameString(path), FolderInfo.class.cast(getFile(path.getParent())), file -> {
            try {
                uploadFlags.remove(pathString);
System.out.println("file: " + file.getName() + ", " + file.getCreationDate() + ", " + file.getContentProperties().getSize());
                cache.put(pathString, file);
                folderCache.get(toString(path.getParent())).add(path);
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
        String pathString = toString(dir);
        final NodeInfo entry = getFile(dir);

        if (!entry.isFolder())
            throw new NotDirectoryException("dir: " + dir);

        List<Path> list = null;
        if (folderCache.containsKey(pathString)) {
            list = folderCache.get(pathString);
        } else {
            final List<NodeInfo> children = drive.getList(entry.getId());
            list = new ArrayList<>(children.size());
            
            // TODO nextPageToken
            for (final NodeInfo child : children) {
                Path childPath = dir.resolve(child.getName());
                list.add(childPath);
//System.err.println("child: " + childPath.toRealPath().toString());
                cache.put(toString(childPath), child);
            }

//System.out.println("put folderCache: " + pathString);
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
                    NodeInfo entry = getFile(path);
                    if (entry != null && FileInfo.class.cast(entry).getContentProperties().getSize() >= 0)
                        leftover = FileInfo.class.cast(entry).getContentProperties().getSize();
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
                        written = pos;
                        return this;
                    }
    
                    public int read(ByteBuffer dst) throws IOException {
                        throw new UnsupportedOperationException();
                    }
    
                    public SeekableByteChannel truncate(long size) throws IOException {
                        throw new UnsupportedOperationException();
                    }
    
                    public int write(ByteBuffer src) throws IOException {
System.out.println("here: X0");
                    int n = wbc.write(src);
                    written += n;
                    return n;
                }

                public long size() throws IOException {
                    return written;
                }

                public void close() throws IOException {
System.out.println("here: X1");
                    wbc.close();
                }
            };
        } else {
            NodeInfo entry = getFile(path);
            if (entry.isFolder())
                throw new NoSuchFileException(path.toString());
            final ReadableByteChannel rbc = Channels.newChannel(newInputStream(path, null));
            final long size = FileInfo.class.cast(entry).getContentProperties().getSize();
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
        NodeInfo newEntry = drive.createFolder(getFile(dir.getParent()).getId(), toFilenameString(dir));
        // cache
        cache.put(toString(dir), newEntry);
        folderCache.get(toString(dir.getParent())).add(dir);
    }

    @Override
    public void delete(final Path path) throws IOException {
        final String pathString = toString(path);
        final NodeInfo entry = getFile(path);

        if (entry.isFolder()) {
            // TODO use cache
            List<NodeInfo> list = drive.getList(entry.getId());

            if (list.size() > 0)
                throw new DirectoryNotEmptyException(pathString);

            drive.removeFolder(entry.getId());
        } else {
            drive.removeFile(entry.getId());
        }

        // cache
        cache.remove(pathString);
        folderCache.get(toString(path.getParent())).remove(path);
    }

    // TODO there is no api???
    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        NodeInfo targetEntry;
        String targetFilename;
        try {
            targetEntry = getFile(target);
            if (!targetEntry.isFolder()) {
                drive.removeFile(targetEntry.getId());
                // cache
                cache.remove(toString(target));
                folderCache.get(toString(target.getParent())).remove(toString(target));

                targetEntry = getFile(target.getParent());
                targetFilename = toFilenameString(target);
            } else {
                targetFilename = toFilenameString(source);
            }
        } catch (IOException e) { // TODO
            if (e.getMessage().startsWith("404")) {
System.err.println(e);
                targetEntry = getFile(target.getParent());
                targetFilename = toFilenameString(source);
            } else {
                throw e;
            }
        }

        final NodeInfo sourceEntry = getFile(source);
        if (!sourceEntry.isFolder()) {
            NodeInfo newEntry = null;

            // cache
            cache.put(toString(target), newEntry);
            String pathString = toString(target.getParent());
            List<Path> paths = folderCache.get(pathString);
            if (paths == null) {
                paths = new ArrayList<>();
                folderCache.put(pathString, paths);
            }
            paths.add(target);
        } else {
            throw new UnsupportedOperationException("source can not be a folder");
        }
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        NodeInfo targetEntry;
        String targetFilename;
        try {
            targetEntry = getFile(target);
            if (!targetEntry.isFolder()) {
                drive.removeFile(targetEntry.getId());
                // cache
                cache.remove(toString(target));
                folderCache.get(toString(target.getParent())).remove(toString(target));

                targetEntry = getFile(target.getParent());
                targetFilename = toFilenameString(target);
            } else {
                targetFilename = toFilenameString(source);
            }
        } catch (IOException e) {
            if (e.getMessage().startsWith("404")) { // TODO
System.err.println(e);
                targetEntry = getFile(target.getParent());
                targetFilename = toFilenameString(source);
            } else {
                throw e;
            }
        }

        NodeInfo sourceEntry = getFile(source);
        StringBuilder previousParents = new StringBuilder();
        for (String parent: sourceEntry.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }
        NodeInfo newEntry = drive.renameFile(sourceEntry.getId(), targetFilename); // TODO

        // cache
        cache.remove(toString(source));
        folderCache.get(toString(source.getParent())).remove(source);

        cache.put(toString(target), newEntry);
//System.out.println("target.parent: " + target.getParent() + ", " + folderCache.get(toString(target.getParent())));
        String pathString = toString(target.getParent());
        List<Path> paths = folderCache.get(pathString);
        if (paths == null) {
            paths = new ArrayList<>();
            folderCache.put(pathString, paths);
        }
        paths.add(target);
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
            final NodeInfo entry = getFile(path);
    
            if (entry.isFolder())
                return;
    
            // TODO: assumed; not a file == directory
            for (final AccessMode mode : modes)
                if (mode == AccessMode.EXECUTE)
                    throw new AccessDeniedException(pathString);

        } catch (IOException e) {
            if (e.getMessage().startsWith("404")) { // TODO
                throw (IOException) new NoSuchFileException("path: " + path).initCause(e);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void close() throws IOException {
        drive.destroy();
    }

    /**
     * @throws IOException if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    public Object getPathMetadata(final Path path) throws IOException {
if (uploadFlags.contains(toString(path))) {
System.out.println("uploading...");
    FileInfo entry = new FileInfo();
    entry.setName(toFilenameString(path));
    return entry;
}
        try {
            return getFile(path);
        } catch (IOException e) {
            if (e.getMessage().startsWith("404")) { // TODO
                throw (IOException) new NoSuchFileException("path: " + path).initCause(e);
            } else {
                throw e;
            }
        }
    }
}
