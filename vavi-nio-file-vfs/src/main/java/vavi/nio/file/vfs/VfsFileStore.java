/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import java.io.IOException;
import java.nio.file.FileStore;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;

import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.filestore.FileStoreBase;


/**
 * A simple Commons VFS2 {@link FileStore}
 *
 * <p>
 * This makes use of information available in {@link FileSystemManager}.
 * Information is computed in "real time".
 * </p>
 */
public final class VfsFileStore extends FileStoreBase {

    private final FileObject root; // TODO

    /**
     * Constructor
     *
     * @param root the (valid) Commons VFS drive to use
     */
    public VfsFileStore(final FileObject root, final FileAttributesFactory factory) {
        super("vfs", factory, false);
        this.root = root;
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
        return 1024 * 1024 * 1024;
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
        return 1024 * 1024 * 1024;
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
        return 1024 * 1024 * 1024;
    }
}
