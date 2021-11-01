/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.CachedFileSystemDriverBase;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;
import com.google.common.io.ByteStreams;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCopyParameterSet;
import com.microsoft.graph.models.DriveItemCreateUploadSessionParameterSet;
import com.microsoft.graph.models.DriveItemUploadableProperties;
import com.microsoft.graph.models.Folder;
import com.microsoft.graph.models.ItemReference;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.tasks.LargeFileUploadTask;

import vavi.nio.file.Util;
import vavi.nio.file.onedrive4.OneDriveFileAttributesFactory.Metadata;
import vavi.util.Debug;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;
import static vavi.nio.file.onedrive4.OneDriveFileSystemProvider.ENV_IGNORE_APPLE_DOUBLE;
import static vavi.nio.file.onedrive4.OneDriveFileSystemProvider.ENV_USE_SYSTEM_WATCHER;


/**
 * OneDriveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class OneDriveFileSystemDriver extends CachedFileSystemDriverBase<DriveItem> {

    private final GraphServiceClient<?> client;

    private Runnable closer;
    private OneDriveWatchService systemWatcher;

    @SuppressWarnings("unchecked")
    public OneDriveFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final GraphServiceClient<?> client,
            Runnable closer,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.client = client;
        this.closer = closer;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault(ENV_IGNORE_APPLE_DOUBLE, false);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
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
    protected String getFilenameString(DriveItem entry) throws IOException {
    	return entry.name;
    }

    @Override
    protected DriveItem getRootEntry() throws IOException {
    	return client.drive().root().buildRequest().get();
    }

    @Override
    protected DriveItem getEntry(DriveItem dirEntry, Path path)throws IOException {
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
	        // so reluctantly we provide {@link OneDriveUploadOpenOption} for {@link java.nio.file.Files#copy} options.
	        Path source = uploadOption.getSource();
Debug.println("upload w/ option: " + source);
	
	        return uploadEntryWithOption(path, Files.newInputStream(source), (int) Files.size(source));
	    } else {
Debug.println("upload w/o option");
	        return new Util.OutputStreamForUploading() {
	        	// TODO this output stream is used for getting file length
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

    /**
     * @see Files#copy(Path, Path, CopyOption...)
     * @see OneDriveUploadOption
     */
    private OutputStream uploadEntryWithOption(Path path, InputStream is, int size) throws IOException {
        if (size > threshold) {
            UploadSession uploadSession = client.drive().root()
            		.itemWithPath(toItemPathString(toPathString(path)))
            		.createUploadSession(DriveItemCreateUploadSessionParameterSet.newBuilder().withItem(new DriveItemUploadableProperties()).build())
            		.buildRequest()
            		.post();
            LargeFileUploadTask<DriveItem> chunkedUploadProvider = new LargeFileUploadTask<DriveItem>(
    				uploadSession,
    				client,
    				is,
    				size,
    				DriveItem.class);
            chunkedUploadProvider.uploadAsync()
            	.thenAccept(result -> {
                    cache.addEntry(path, result.responseBody);
Debug.println("upload done: " + result.responseBody.name);
            	});
            return new OutputStream() {
            	// TODO sync with upload provider
            	// currently this class is wasted. 
            	int l = 0;
            	public void write(int b) throws IOException {
            		throw new UnsupportedOperationException("don't use");
            	}
            	public void write(byte[] b, int off, int len) throws IOException {
            		l += len;
Debug.println(l + "/" + size);
            	}
            	public void close() throws IOException {
Debug.println("stream closed");
            	}
            };
        } else {
            return new Util.OutputStreamForUploading() {
                @Override
                protected void onClosed() throws IOException {
                    InputStream is = getInputStream();
                    DriveItem newEntry = client.drive().root()
                    		.itemWithPath(toItemPathString(toPathString(path)))
                    		.content()
                    		.buildRequest()
                    		.put(ByteStreams.toByteArray(is)); // TODO depends on guava
                    cache.addEntry(path, newEntry);
                }
            };
        }
    }

    /** w/o option */
    private void uploadEntry(Path path, InputStream is, int size) throws IOException {
        if (size > 4 * 1024 * 1024) {
            UploadSession uploadSession = client.drive().root()
            		.itemWithPath(toItemPathString(toPathString(path)))
            		.createUploadSession(DriveItemCreateUploadSessionParameterSet.newBuilder().withItem(new DriveItemUploadableProperties()).build())
            		.buildRequest()
            		.post();
            LargeFileUploadTask<DriveItem> chunkedUploadProvider = new LargeFileUploadTask<DriveItem>(
    				uploadSession,
    				client,
    				is,
    				size,
    				DriveItem.class);
            chunkedUploadProvider.uploadAsync()
            	.thenAccept(result -> {
                    cache.addEntry(path, result.responseBody);
Debug.println("upload done: " + result.responseBody.name);
            	});
// System.err.println(current + "/" + max);
        } else {
            DriveItem newEntry = client.drive().root().itemWithPath(toItemPathString(toPathString(path))).content().buildRequest().put(ByteStreams.toByteArray(is)); // TODO depends on guava
            cache.addEntry(path, newEntry);
        }
    }

    /** ms-graph doesn't accept '+' in a path string */
    private String toItemPathString(String pathString) throws IOException {
        return pathString.replaceFirst("^\\/", "").replace("+", "%20");
    }

    @Override
    protected DriveItem createDirectoryEntry(Path dir) throws IOException {
        DriveItem parentEntry = cache.getEntry(dir.toAbsolutePath().getParent());

        // TODO: how to diagnose?
        DriveItem preEntry = new DriveItem();
        preEntry.name = toFilenameString(dir);
        preEntry.folder = new Folder();
        DriveItem newEntry = client.drive().items(parentEntry.id).children().buildRequest().post(preEntry);
Debug.println(newEntry.id + ", " + newEntry.name + ", folder: " + isFolder(newEntry) + ", " + newEntry.hashCode());
		return newEntry;
    }

    @Override
    public void close() throws IOException {
        closer.run();
    }

    @Override
    protected Object getMetadata(DriveItem entry) throws IOException {
        return new Metadata(this, entry);
    }

    @Nonnull
    @Override
    public WatchService newWatchService() {
        try {
            return new OneDriveWatchService(client);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected List<DriveItem> getDirectoryEntries(DriveItem dirEntry, Path dir) throws IOException {
    	List<DriveItem> list = new ArrayList<>(dirEntry.folder.childCount);

        DriveItemCollectionPage pages = client.drive().items(dirEntry.id).children().buildRequest().get();
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
    protected boolean hasChildren(DriveItem dirEntry, Path path) throws IOException {
        DriveItemCollectionPage pages = client.drive().items(dirEntry.id).children().buildRequest().get();
    	return pages.getCurrentPage().size() > 0;
    }

    @Override
    protected void removeEntry(DriveItem entry, Path path) throws IOException {
    	// TODO: unknown what happens when a move operation is performed
        // and the target already exists
        client.drive().items(entry.id).buildRequest().delete();
	}

    @Override
    protected DriveItem copyEntry(DriveItem sourceEntry, DriveItem targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
        ItemReference ir = new ItemReference();
        ir.id = targetParentEntry.id;
        DriveItem newEntry = client.drive()
	    		.items(sourceEntry.id)
	    		.copy(DriveItemCopyParameterSet.newBuilder()
	    				.withName(toFilenameString(target))
	    				.withParentReference(ir)
	    				.build())
	    		.buildRequest()
	    		.post();
Debug.println("copy done: " + newEntry.id);
        return newEntry;
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
