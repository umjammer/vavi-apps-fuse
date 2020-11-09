/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
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
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase;
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
import vavi.nio.file.onedrive4.OneDriveFileAttributesFactory.Metadata;
import vavi.nio.file.onedrive4.graph.LraMonitorProvider;
import vavi.nio.file.onedrive4.graph.LraMonitorResponseHandler;
import vavi.nio.file.onedrive4.graph.LraMonitorResult;
import vavi.nio.file.onedrive4.graph.LraSession;
import vavi.util.Debug;

import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;
import static vavi.nio.file.onedrive4.OneDriveFileSystemProvider.ENV_IGNORE_APPLE_DOUBLE;


/**
 * OneDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class OneDriveFileSystemDriver extends ExtendedFileSystemDriverBase {

    private final IGraphServiceClient client;
    private boolean ignoreAppleDouble = false;

    private Runnable closer;
    @SuppressWarnings("unchecked")
    public OneDriveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final IGraphServiceClient client,
            final Map<String, ?> env) {
            Runnable closer,
        super(fileStore, provider);
        this.client = client;
        this.closer = closer;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault(ENV_IGNORE_APPLE_DOUBLE, false);
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
                        entry = client.drive().root().itemWithPath(toItemPathString(pathString.substring(1))).buildRequest().get();
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

        if (isFolder(entry)) {
            throw new IsDirectoryException(path.toString());
        }

        try {
            return client.drive().items(entry.id).content().buildRequest().get();
        } catch (ClientException e) {
            throw new IOException(e);
        }
    }

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
        }

        OneDriveUploadOption uploadOption = Util.getOneOfOptions(OneDriveUploadOption.class, options);
        if (uploadOption != null) {
            // java.nio.file is highly abstracted, so here source information is lost.
            // but onedrive graph api requires content length for upload.
            // so reluctantly we provide {@link OneDriveUploadOpenOption} for {@link java.nio.file.Files#copy} options.
            Path source = uploadOption.getSource();
Debug.println("upload w/ option: " + source);

            return uploadEntry(path, (int) Files.size(source));
        } else {
Debug.println("upload w/o option");
            return new Util.OutputStreamForUploading() { // TODO used for getting file length
                @Override
                protected void onClosed() throws IOException {
                    InputStream is = getInputStream();
                    uploadEntry(path, is, is.available());
                }
            };
        }
    }

    /** */
    private static final int threshold = 4 * 1024 * 1024;

    /** OneDriveUploadOption */
    private OutputStream uploadEntry(Path path, int size) throws IOException {
        if (size > threshold) {
            UploadSession uploadSession = client.drive().root().itemWithPath(toItemPathString(toPathString(path))).createUploadSession(new DriveItemUploadableProperties()).buildRequest().post();
            vavi.nio.file.onedrive4.graph.ChunkedUploadProvider<DriveItem> chunkedUploadProvider =
                    new vavi.nio.file.onedrive4.graph.ChunkedUploadProvider<>(uploadSession, client, size, DriveItem.class);
            return new BufferedOutputStream(chunkedUploadProvider.upload(new IProgressCallback<DriveItem>() {
                    @Override
                    public void progress(final long current, final long max) {
Debug.println(current + "/" + max);
                    }
                    @Override
                    public void success(final DriveItem result) {
                        cache.addEntry(path, result);
Debug.println("upload done: " + result.name);
                    }
                    @Override
                    public void failure(final ClientException ex) {
                        // never called
                    }
                }), threshold);
        } else {
            return new Util.OutputStreamForUploading() {
                @Override
                protected void onClosed() throws IOException {
                    InputStream is = getInputStream();
                    DriveItem newEntry = client.drive().root().itemWithPath(toItemPathString(toPathString(path))).content().buildRequest().put(ByteStreams.toByteArray(is)); // TODO depends on guava
                    cache.addEntry(path, newEntry);
                }
            };
        }
    }

    /** {@link Files#copy(Path, Path, CopyOption...)} */
    private void uploadEntry(Path path, InputStream is, int size) throws IOException {
        if (size > 4 * 1024 * 1024) {
            UploadSession uploadSession = client.drive().root().itemWithPath(toItemPathString(toPathString(path))).createUploadSession(new DriveItemUploadableProperties()).buildRequest().post();
            ChunkedUploadProvider<DriveItem> chunkedUploadProvider = new ChunkedUploadProvider<>(uploadSession,
                    client, is, size, DriveItem.class);
            chunkedUploadProvider.upload(new IProgressCallback<DriveItem>() {
                    @Override
                    public void progress(final long current, final long max) {
Debug.println(current + "/" + max);
                    }
                    @Override
                    public void success(final DriveItem result) {
                        cache.addEntry(path, result);
Debug.println("upload done: " + result.name);
                    }
                    @Override
                    public void failure(final ClientException ex) {
                        throw ex;
                    }
                });
        } else {
            DriveItem newEntry = client.drive().root().itemWithPath(toItemPathString(toPathString(path))).content().buildRequest().put(ByteStreams.toByteArray(is)); // TODO depends on guava
            cache.addEntry(path, newEntry);
        }
    }

    /** */
    private String toItemPathString(String pathString) throws IOException {
        return URLEncoder.encode(pathString, "utf-8").replace("+", "%20");
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir, true), filter);
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        DriveItem parentEntry = cache.getEntry(dir.toAbsolutePath().getParent());

        // TODO: how to diagnose?
        DriveItem preEntry = new DriveItem();
        preEntry.name = toFilenameString(dir);
        preEntry.folder = new Folder();
        DriveItem newEntry = client.drive().items(parentEntry.id).children().buildRequest().post(preEntry);
Debug.println(newEntry.id + ", " + newEntry.name + ", folder: " + isFolder(newEntry) + ", " + newEntry.hashCode());
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
     *                     if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    @Override
    protected void checkAccessImpl(final Path path, final AccessMode... modes) throws IOException {
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
        closer.run();
    }

    /**
     * @throws IOException you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    protected Object getPathMetadataImpl(final Path path) throws IOException {
        return new Metadata(this, cache.getEntry(path));
    }

    /** */
    private List<Path> getDirectoryEntries(Path dir, boolean useCache) throws IOException {
        final DriveItem entry = cache.getEntry(dir);

        if (!isFolder(entry)) {
//System.err.println(entry.name + ", " + entry.id + ", " + entry.hashCode());
            throw new NotDirectoryException(dir.toString());
        }

        List<Path> list = null;
        if (useCache && cache.containsFolder(dir)) {
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
            if (getDirectoryEntries(path, false).size() > 0) {
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
        DriveItem targetParentEntry = cache.getEntry(target.toAbsolutePath().getParent());
        if (isFile(sourceEntry)) {
            ItemReference ir = new ItemReference();
            ir.id = targetParentEntry.id;
            IDriveItemCopyRequest request = client.drive().items(sourceEntry.id).copy(toFilenameString(target), ir).buildRequest();
            BaseRequest.class.cast(request).setHttpMethod(HttpMethod.POST); // #send() hides IDriveItemCopyRequest fields already set above.
            DriveItemCopyBody body = new DriveItemCopyBody(); // ditto
            body.name = toFilenameString(target); // ditto
            body.parentReference = ir; // ditto
            LraMonitorResponseHandler<DriveItem> handler = new LraMonitorResponseHandler<>();
            @SuppressWarnings({ "unchecked", "rawtypes" }) // TODO
            LraSession copySession = client.getHttpProvider().<LraMonitorResult, DriveItemCopyBody, LraMonitorResult>send((IHttpRequest) request, LraMonitorResult.class, body, (IStatefulResponseHandler) handler).getSession();
            LraMonitorProvider<DriveItem> copyMonitorProvider = new LraMonitorProvider<>(copySession, client, DriveItem.class);
            copyMonitorProvider.monitor(new IProgressCallback<DriveItem>() {
                @Override
                public void progress(final long current, final long max) {
Debug.println("copy progress: " + current + "/" + max);
                }
                @Override
                public void success(final DriveItem result) {
Debug.println("copy done: " + result.id);
                    cache.addEntry(target, result);
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
        DriveItem targetParentEntry = cache.getEntry(targetIsParent ? target : target.toAbsolutePath().getParent());
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
            DriveItem preEntry = new DriveItem();
            preEntry.name = toFilenameString(target);
            preEntry.folder = new Folder();
            preEntry.parentReference = new ItemReference();
            preEntry.parentReference.id = targetParentEntry.id;
            DriveItem patchedEntry = client.drive().items(sourceEntry.id).buildRequest().patch(preEntry);
            cache.moveEntry(source, target, patchedEntry);
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

    /** attributes user:description */
    void patchEntryDescription(DriveItem sourceEntry, String description) throws IOException {
        DriveItem preEntry = new DriveItem();
        preEntry.description = description;
        DriveItem patchedEntry = client.drive().items(sourceEntry.id).buildRequest().patch(preEntry);
        Path path = cache.getEntry(sourceEntry);
        cache.removeEntry(path);
        cache.addEntry(path, patchedEntry);
    }
}
