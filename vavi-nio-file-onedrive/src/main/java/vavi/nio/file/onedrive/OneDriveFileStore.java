/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.IOException;
import java.nio.file.FileStore;

import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.filestore.FileStoreBase;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.drive.DriveQuota;


/**
 * A simple OneDrive {@link FileStore}
 *
 * <p>
 * This makes use of information available in {@link DriveQuota}.
 * Information is computed in "real time".
 * </p>
 */
public final class OneDriveFileStore extends FileStoreBase {

    private final OneDriveSDK client;

    /**
     * Constructor
     *
     * @param client the (valid) OneDrive client to use
     */
    public OneDriveFileStore(final OneDriveSDK client, final FileAttributesFactory factory) {
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
        final DriveQuota quota = getQuota();
        return quota == null ? 0 : quota.getTotal();
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
        final DriveQuota quota = getQuota();
        if (quota == null) {
            return 0;
        } else {
            return quota.getTotal() - quota.getUsed();
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
        final DriveQuota quota = getQuota();
        if (quota == null) {
            return 0;
        } else {
            return quota.getTotal() - quota.getUsed();
        }
    }

    /** */
    private DriveQuota cache; // TODO refresh

    /** */
    private DriveQuota getQuota() throws IOException {
        try {
            if (cache != null) {
                return cache;
            } else {
                cache = client.getDefaultDrive().getQuota();
                return cache;
            }
        } catch (OneDriveException e) {
            throw new IOException("cannot get quota info from account", e);
        }
    }
}
