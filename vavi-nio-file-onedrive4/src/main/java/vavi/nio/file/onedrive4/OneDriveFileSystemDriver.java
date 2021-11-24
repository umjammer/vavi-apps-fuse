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
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.github.fge.filesystem.driver.CachedFileSystemDriver;
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

import vavi.nio.file.Util;
import vavi.nio.file.onedrive4.OneDriveFileAttributesFactory.Metadata;
import vavi.nio.file.onedrive4.graph.LraMonitorProvider;
import vavi.nio.file.onedrive4.graph.LraMonitorResponseHandler;
import vavi.nio.file.onedrive4.graph.LraMonitorResult;
import vavi.nio.file.onedrive4.graph.LraSession;
import vavi.util.Debug;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;
import static vavi.nio.file.onedrive4.OneDriveFileSystemProvider.ENV_USE_SYSTEM_WATCHER;


/**
 * OneDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
public final class OneDriveFileSystemDriver extends CachedFileSystemDriver<DriveItem> {

    private final IGraphServiceClient client;

    private Runnable closer;
    private OneDriveWatchService systemWatcher;

    @SuppressWarnings("unchecked")
    public OneDriveFileSystemDriver(FileStore fileStore,
            FileSystemFactoryProvider provider,
            IGraphServiceClient client,
            Runnable closer,
            Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.client = client;
        this.closer = closer;
        setEnv(env);
        boolean useSystemWatcher = (Boolean) ((Map<String, Object>) env).getOrDefault(ENV_USE_SYSTEM_WATCHER, false);

        if (useSystemWatcher) {
            systemWatcher = new OneDriveWatchService(client);
            systemWatcher.setNotificationListener(this::processNotification);
        }
    }

    /** for system watcher */
    private void processNotification(String id, Kind<?> kind) {
        if (ENTRY_DELETE == kind) {
            try {
                Path path = cache.getEntry(e -> id.equals(e.id));
                cache.removeEntry(path);
            } catch (NoSuchElementException e) {
Debug.println("NOTIFICATION: already deleted: " + id);
            }
        } else {
            try {
                try {
                    Path path = cache.getEntry(e -> id.equals(e.id));
Debug.println("NOTIFICATION: maybe updated: " + path);
                    cache.removeEntry(path);
                    cache.getEntry(path);
                } catch (NoSuchElementException e) {
                    DriveItem entry = client.drive().items(id).buildRequest().get();
                    Path parent = cache.getEntry(f -> entry.parentReference.id.equals(f.id));
                    Path path = parent.resolve(entry.name);
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

    @Override
    protected boolean isFolder(DriveItem entry) {
        return entry.folder != null;
    }

    @Override
    protected String getFilenameString(DriveItem entry) {
    	return entry.name;
    }

    @Override
    protected DriveItem getRootEntry(Path root) throws IOException {
    	return client.drive().root().buildRequest().get();
    }

    @Override
    protected DriveItem getEntry(DriveItem parentEntry, Path path)throws IOException {
        try {
        	return client.drive().root().itemWithPath(toItemPathString(toPathString(path))).buildRequest().get();
	    } catch (GraphServiceException e) {
	        if (e.getMessage().startsWith("Error code: itemNotFound")) {
	            return null;
	        } else {
	            throw new IOException(e);
	        }
	    }
    }

    @Override
    protected InputStream downloadEntry(DriveItem entry, Path path, Set<? extends OpenOption> options) throws IOException {
        try {
            return client.drive().items(entry.id).content().buildRequest().get();
        } catch (ClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected OutputStream uploadEntry(DriveItem parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
	    OneDriveUploadOption uploadOption = Util.getOneOfOptions(OneDriveUploadOption.class, options);
	    if (uploadOption != null) {
	        // java.nio.file is highly abstracted, so here source information is lost.
	        // but onedrive graph api requires content length for upload.
	        // so reluctantly we provide {@link OneDriveUploadOption} for {@link java.nio.file.Files#copy} options.
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
                    	updateEntry(path, result);
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
                    updateEntry(path, newEntry);
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
                    	updateEntry(path, result);
Debug.println("upload done: " + result.name);
                    }
                    @Override
                    public void failure(final ClientException ex) {
                        throw ex;
                    }
                });
        } else {
            DriveItem newEntry = client.drive().root().itemWithPath(toItemPathString(toPathString(path))).content().buildRequest().put(ByteStreams.toByteArray(is)); // TODO depends on guava
            updateEntry(path, newEntry);
        }
    }

    /** ms-graph doesn't accept '+' in a path string */
    private String toItemPathString(String pathString) throws IOException {
        return URLEncoder.encode(pathString.replaceFirst("^\\/", ""), "utf-8").replace("+", "%20");
    }

    @Override
    protected List<DriveItem> getDirectoryEntries(DriveItem dirEntry, Path dir) throws IOException {
    	List<DriveItem> list = new ArrayList<>(dirEntry.folder.childCount);

        IDriveItemCollectionPage pages = client.drive().items(dirEntry.id).children().buildRequest().get();
        while (pages != null) {
            for (final DriveItem child : pages.getCurrentPage()) {
                list.add(child);
//System.err.println("child: " + childPath.toRealPath().toString());
            }
            pages = pages.getNextPage() != null ? pages.getNextPage().buildRequest().get() : null;
        }

        return list;
    }

    @Override
    protected DriveItem createDirectoryEntry(DriveItem parentEntry, Path dir) throws IOException {
        DriveItem preEntry = new DriveItem();
        preEntry.name = toFilenameString(dir);
        preEntry.folder = new Folder();
        DriveItem newEntry = client.drive().items(parentEntry.id).children().buildRequest().post(preEntry);
Debug.println(newEntry.id + ", " + newEntry.name + ", folder: " + isFolder(newEntry) + ", " + newEntry.hashCode());
		return newEntry;
    }

    @Override
    protected boolean hasChildren(DriveItem dirEntry, Path path) throws IOException {
        IDriveItemCollectionPage pages = client.drive().items(dirEntry.id).children().buildRequest().get();
    	return pages.getCurrentPage().size() > 0;
    }

    @Override
    protected void removeEntry(DriveItem entry, Path path) throws IOException {
        client.drive().items(entry.id).buildRequest().delete();
	}

    @Override
    protected DriveItem copyEntry(DriveItem sourceEntry, DriveItem targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
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
				updateEntry(target, result);
            }
            @Override
            public void failure(final ClientException ex) {
                throw new IllegalStateException(ex);
            }
        });
        // null means that copy is async, cache by your self
        // @see IProgressCallback#success()
        return null;
    }

    @Override
    protected DriveItem moveEntry(DriveItem sourceEntry, DriveItem targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        DriveItem preEntry = new DriveItem();
        preEntry.name = toFilenameString(target);
        preEntry.parentReference = new ItemReference();
        preEntry.parentReference.id = targetParentEntry.id;
        return client.drive().items(sourceEntry.id).buildRequest().patch(preEntry);
    }

    @Override
    protected DriveItem moveFolderEntry(DriveItem sourceEntry, DriveItem targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        DriveItem preEntry = new DriveItem();
        preEntry.name = toFilenameString(target);
        preEntry.parentReference = new ItemReference();
        preEntry.parentReference.id = targetParentEntry.id;
        return client.drive().items(sourceEntry.id).buildRequest().patch(preEntry);
    }

    @Override
    protected DriveItem renameEntry(DriveItem sourceEntry, DriveItem targetParentEntry, Path source, Path target) throws IOException {
        DriveItem preEntry = new DriveItem();
        preEntry.name = toFilenameString(target);
        return client.drive().items(sourceEntry.id).buildRequest().patch(preEntry);
    }

    @Override
    protected Object getPathMetadata(DriveItem entry) throws IOException {
        return new Metadata(this, entry);
    }

    @Override
    public void close() throws IOException {
        closer.run();
    }

    @Override
    public WatchService newWatchService() {
        try {
            return new OneDriveWatchService(client);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    //
    // user:attributes
    //

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
