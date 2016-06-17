/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import java.io.IOException;
import java.nio.file.FileStore;

import org.yetiz.lib.acd.ACD;
import org.yetiz.lib.acd.Entity.AccountQuota;

import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.filestore.FileStoreBase;
import com.google.api.services.drive.model.About.StorageQuota;


/**
 * A simple Amazon Cloud Drive {@link FileStore}
 *
 * <p>
 * This makes use of information available in {@link StorageQuota}.
 * Information is computed in "real time".
 * </p>
 */
public final class AcdFileStore extends FileStoreBase {

    private final ACD drive;

    /**
     * Constructor
     *
     * @param drive the (valid) Amazon Cloud Drive drive to use
     */
    public AcdFileStore(final ACD drive, final FileAttributesFactory factory) {
        super("acd", factory, false);
        this.drive = drive;
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
        return getQuota().getQuota();
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
        final AccountQuota quota = getQuota();
        return quota.getAvailable();
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
        final AccountQuota quota = getQuota();
        return quota.getQuota() - quota.getAvailable();
    }

    /** */
    private AccountQuota cache; // TODO refresh
    
    /** */
    private AccountQuota getQuota() throws IOException {
        if (cache != null) {
            return cache;
        } else {
            cache = drive.getUserQuota();
            return cache; 
        }
    }
}
