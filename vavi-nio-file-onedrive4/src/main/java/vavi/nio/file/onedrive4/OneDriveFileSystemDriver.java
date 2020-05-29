/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.UnixLikeFileSystemDriverBase;
import com.github.fge.filesystem.exceptions.IsDirectoryException;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.google.common.io.ByteStreams;
import com.microsoft.graph.concurrency.ChunkedUploadProvider;
import com.microsoft.graph.concurrency.IProgressCallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.http.HttpMethod;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.http.IStatefulResponseHandler;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.DriveItemCopyBody;
import com.microsoft.graph.models.extensions.DriveItemUploadableProperties;
import com.microsoft.graph.models.extensions.Folder;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.ItemReference;
import com.microsoft.graph.models.extensions.UploadSession;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemCopyRequest;

import vavi.nio.file.Cache;
import vavi.nio.file.Util;
import vavi.nio.file.Util.OutputStreamForUploading;
import vavi.nio.file.onedrive4.graph.CopyMonitorProvider;
import vavi.nio.file.onedrive4.graph.CopyMonitorResponseHandler;
import vavi.nio.file.onedrive4.graph.CopyMonitorResult;
import vavi.nio.file.onedrive4.graph.CopySession;
import vavi.util.Debug;

import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;


/**
 * OneDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class OneDriveFileSystemDriver extends UnixLikeFileSystemDriverBase {

    private final IGraphServiceClient client;
    private boolean ignoreAppleDouble = false;

    @SuppressWarnings("unchecked")
    public OneDriveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final IGraphServiceClient client,
            final Map<String, ?> env) {
        super(fileStore, provider);
        this.client = client;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
    }

    /** ugly */
    private boolean isFile(DriveItem entry) {
        return entry.file != null;
    }

    /** ugly */
    private boolean isFolder(DriveItem entry) {
        return entry.folder != null;
    }

    /** */
    private Cache<DriveItem> cache = new Cache<DriveItem>() {
        /**
         * TODO when the parent is not cached
         * @see #ignoreAppleDouble
         * @throws NoSuchFileException must be thrown when the path is not found in this cache
         */
        public DriveItem getEntry(Path path) throws IOException {
            if (cache.containsFile(path)) {
                return cache.getFile(path);
            } else {
                if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                    throw new NoSuchFileException("ignore apple double file: " + path);
                }

                String pathString = toPathString(path);
//Debug.println("path: " + pathString);
                try {
                    DriveItem entry;
                    if (pathString.equals("/")) {
                        entry = client.drive().root().buildRequest().get();
                    } else {
                        entry = client.drive().root().itemWithPath(URLEncoder.encode(pathString.substring(1), "utf-8")).buildRequest().get();
                    }
                    cache.putFile(path, entry);
                    return entry;
                } catch (GraphServiceException e) {
                    if (e.getMessage().startsWith("Error code: itemNotFound")) {
                        if (cache.containsFile(path)) {
                            cache.removeEntry(path);
                        }
                        throw new NoSuchFileException(pathString);
                    } else {
                        throw e;
                    }
                }
            }
        }
    };

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final DriveItem entry = cache.getEntry(path);

        // TODO: metadata driver
        if (isFolder(entry)) {
            throw new IsDirectoryException(path.toString());
        }

        return client.drive().items(entry.id).content().buildRequest().get();
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
        try {
            DriveItem entry = cache.getEntry(path);

            if (isFolder(entry)) {
                throw new IsDirectoryException(path.toString());
            } else {
                throw new FileAlreadyExistsException(path.toString());
            }
        } catch (NoSuchFileException e) {
Debug.println("newOutputStream: " + e.getMessage());
//new Exception("*** DUMMY ***").printStackTrace();
        }

        return new OutputStreamForUploading() {
            @Override
            protected void onClosed() throws IOException {
                InputStream is = getInputStream();
                if (is.available() > 4 * 1024 * 1024) {
                    UploadSession uploadSession = client.drive().root().itemWithPath(URLEncoder.encode(toPathString(path), "utf-8")).createUploadSession(new DriveItemUploadableProperties()).buildRequest().post();
                    ChunkedUploadProvider<DriveItem> chunkedUploadProvider = new ChunkedUploadProvider<>(uploadSession,
                            client, is, is.available(), DriveItem.class);
                    chunkedUploadProvider.upload(new IProgressCallback<DriveItem>() {
                            @Override
                            public void progress(final long current, final long max) {
Debug.println(current + "/" + max);
                            }
                            @Override
                            public void success(final DriveItem result) {
                                try {
                                    cache.addEntry(path, result);
                                } catch (IOException e) {
                                    throw new IllegalStateException(e);
                                }
Debug.println("done");
                            }
                            @Override
                            public void failure(final ClientException ex) {
                                throw new IllegalStateException(ex);
                            }
                        });
                } else {
                    DriveItem newEntry = client.drive().root().itemWithPath(URLEncoder.encode(toPathString(path), "utf-8")).content().buildRequest().put(ByteStreams.toByteArray(is)); // TODO depends on guava
                    cache.addEntry(path, newEntry);
                }
            }
        };
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir), filter);
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
                        DriveItem entry = cache.getEntry(path);
                        if (entry != null && entry.size >= 0) {
                            leftover = entry.size;
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
                        java.io.File file = new java.io.File(toPathString(path));
                        FileInputStream fis = new FileInputStream(file);
                        FileChannel fc = fis.getChannel();
                        fc.transferTo(0, file.length(), this);
                        fis.close();
                    }
                    super.close();
                }
            };
        } else {
            DriveItem entry = cache.getEntry(path);
            if (isFolder(entry)) {
                throw new IsDirectoryException(path.toString());
            }
            return new Util.SeekableByteChannelForReading(newInputStream(path, null)) {
                @Override
                protected long getSize() throws IOException {
                    return entry.size;
                }
            };
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        DriveItem parentEntry = cache.getEntry(dir.getParent());

        // TODO: how to diagnose?
        DriveItem preEntry = new DriveItem();
        preEntry.name = toFilenameString(dir);
        preEntry.folder = new Folder();
        DriveItem newEntry = client.drive().items(parentEntry.id).children().buildRequest().post(preEntry);
System.out.println(newEntry.id + ", " + newEntry.name + ", folder: " + isFolder(newEntry) + ", " + newEntry.hashCode());
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
        copyEntry(source, target);
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
        final DriveItem entry = cache.getEntry(path);

        if (!isFile(entry)) {
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
     * @throws IOException you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    public Object getPathMetadata(final Path path) throws IOException {
        return cache.getEntry(path);
    }

    /** */
    private List<Path> getDirectoryEntries(Path dir) throws IOException {
        final DriveItem entry = cache.getEntry(dir);

        if (!isFolder(entry)) {
//System.err.println(entry.name + ", " + entry.id + ", " + entry.hashCode());
            throw new NotDirectoryException(dir.toString());
        }

        List<Path> list = null;
        if (cache.containsFolder(dir)) {
            list = cache.getFolder(dir);
        } else {
            list = new ArrayList<>(entry.folder.childCount);

            IDriveItemCollectionPage pages = client.drive().items(entry.id).children().buildRequest().get();
            while (pages != null) {
                for (final DriveItem child : pages.getCurrentPage()) {
                    Path childPath = dir.resolve(child.name);
                    list.add(childPath);
//System.err.println("child: " + childPath.toRealPath().toString());

                    cache.putFile(childPath, child);
                }
                pages = pages.getNextPage() != null ? pages.getNextPage().buildRequest().get() : null;
            }
            cache.putFolder(dir, list);
        }

        return list;
    }

    /** */
    private void removeEntry(Path path) throws IOException {
        DriveItem entry = cache.getEntry(path);
        if (isFolder(entry)) {
            if (cache.getChildCount(path) > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }

        // TODO: unknown what happens when a move operation is performed
        // and the target already exists
        client.drive().items(entry.id).buildRequest().delete();

        cache.removeEntry(path);
    }

    /** */
    private void copyEntry(final Path source, final Path target) throws IOException {
        DriveItem sourceEntry = cache.getEntry(source);
        DriveItem targetParentEntry = cache.getEntry(target.getParent());
        if (isFile(sourceEntry)) {
            ItemReference ir = new ItemReference();
            ir.id = targetParentEntry.id;
            IDriveItemCopyRequest request = client.drive().items(sourceEntry.id).copy(toFilenameString(target), ir).buildRequest();
            BaseRequest.class.cast(request).setHttpMethod(HttpMethod.POST); // #send() hides IDriveItemCopyRequest fields already set above.
            DriveItemCopyBody body = new DriveItemCopyBody(); // ditto
            body.name = toFilenameString(target); // ditto
            body.parentReference = ir; // ditto
            CopyMonitorResponseHandler<DriveItem> handler = new CopyMonitorResponseHandler<>();
            @SuppressWarnings({ "unchecked", "rawtypes" }) // TODO
            CopySession copySession = client.getHttpProvider().<CopyMonitorResult, DriveItemCopyBody, CopyMonitorResult>send((IHttpRequest) request, CopyMonitorResult.class, body, (IStatefulResponseHandler) handler).getSession();
            CopyMonitorProvider<DriveItem> copyMonitorProvider = new CopyMonitorProvider<>(copySession, client, DriveItem.class);
            copyMonitorProvider.monitor(new IProgressCallback<DriveItem>() {
                    @Override
                    public void progress(final long current, final long max) {
Debug.println("copy progress: " + current + "/" + max);
                    }
                    @Override
                    public void success(final DriveItem result) {
Debug.println("copy done: " + result.id);
                        try {
                            cache.addEntry(target, result);
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    @Override
                    public void failure(final ClientException ex) {
                        throw new IllegalStateException(ex);
                    }
                });
        } else if (isFolder(sourceEntry)) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /**
     * @param targetIsParent if the target is folder
     */
    private void moveEntry(final Path source, final Path target, boolean targetIsParent) throws IOException {
        DriveItem sourceEntry = cache.getEntry(source);
        DriveItem targetParentEntry = cache.getEntry(targetIsParent ? target : target.getParent());
        if (isFile(sourceEntry)) {
            DriveItem preEntry = new DriveItem();
            preEntry.name = targetIsParent ? toFilenameString(source) : toFilenameString(target);
            preEntry.parentReference = new ItemReference();
            preEntry.parentReference.id = targetParentEntry.id;
            DriveItem patchedEntry = client.drive().items(sourceEntry.id).buildRequest().patch(preEntry);
            cache.removeEntry(source);
            if (targetIsParent) {
                cache.addEntry(target.resolve(source.getFileName()), patchedEntry);
            } else {
                cache.addEntry(target, patchedEntry);
            }
        } else if (isFolder(sourceEntry)) {
            // TODO java spec. allows empty folder
            throw new IsDirectoryException("source can not be a folder: " + source);
        }
    }

    /** */
    private void renameEntry(final Path source, final Path target) throws IOException {
        DriveItem sourceEntry = cache.getEntry(source);
//Debug.println(sourceEntry.id + ", " + sourceEntry.name);

        DriveItem preEntry = new DriveItem();
        preEntry.name = toFilenameString(target);
        DriveItem patchedEntry = client.drive().items(sourceEntry.id).buildRequest().patch(preEntry);
        cache.removeEntry(source);
        cache.addEntry(target, patchedEntry);
    }
}
