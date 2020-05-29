/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.IOException;
import java.nio.file.FileStore;

import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.filestore.FileStoreBase;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.Drive;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.Quota;


/**
 * A simple OneDrive {@link FileStore}
 *
 * <p>
 * This makes use of information available in {@link Drive}.
 * Information is computed in "real time".
 * </p>
 */
public final class OneDriveFileStore extends FileStoreBase {

    private final IGraphServiceClient client;

    /**
     * Constructor
     *
     * @param client the (valid) OneDrive client to use
     */
    public OneDriveFileStore(final IGraphServiceClient client, final FileAttributesFactory factory) {
        super("onedrive", factory, false);
        this.client = client;
    }

    /**
     * Returns the size, in bytes, of the file store.
     *
     * @return the size of the file store, in bytes
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long getTotalSpace() throws IOException {
        final Quota quota = getQuota();
        return quota == null ? 0 : quota.total;
    }

    /**
     * Returns the number of bytes available to this Java virtual machine on the
     * file store.
     * <p>
     * The returned number of available bytes is a hint, but not a
     * guarantee, that it is possible to use most or any of these bytes. The
     * number of usable bytes is most likely to be accurate immediately
     * after the space attributes are obtained. It is likely to be made
     * inaccurate
     * by any external I/O operations including those made on the system outside
     * of this Java virtual machine.
     *
     * @return the number of bytes available
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long getUsableSpace() throws IOException {
        final Quota quota = getQuota();
        if (quota == null) {
            return 0;
        } else {
            return quota.remaining;
        }
    }

    /**
     * Returns the number of unallocated bytes in the file store.
     * <p>
     * The returned number of unallocated bytes is a hint, but not a
     * guarantee, that it is possible to use most or any of these bytes. The
     * number of unallocated bytes is most likely to be accurate immediately
     * after the space attributes are obtained. It is likely to be
     * made inaccurate by any external I/O operations including those made on
     * the system outside of this virtual machine.
     *
     * @return the number of unallocated bytes
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long getUnallocatedSpace() throws IOException {
        final Quota quota = getQuota();
        if (quota == null) {
            return 0;
        } else {
            return quota.remaining + quota.deleted;
        }
    }

    /** */
    private Quota cache; // TODO refresh

    /** */
    private Quota getQuota() throws IOException {
        if (cache != null) {
            return cache;
        } else {
            client.drive().buildRequest().get(new ICallback<Drive>() {
                @Override
                public void success(final Drive result) {
                    cache = result.quota;
                }
                @Override
                public void failure(ClientException ex) {
                }
            });
            return cache;
        }
    }
}
