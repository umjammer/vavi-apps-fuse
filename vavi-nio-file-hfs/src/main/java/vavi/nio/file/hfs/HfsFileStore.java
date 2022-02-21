/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.hfs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.util.logging.Level;

import org.catacombae.storage.fs.FSFolder;
import org.catacombae.storage.fs.FSForkType;
import org.catacombae.storage.fs.hfscommon.HFSCommonFileSystemHandler;

import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.github.fge.filesystem.filestore.FileStoreBase;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * A simple HFS {@link FileStore}
 *
 * <p>
 * This makes use of information available in {@link FSFolder}.
 * Information is computed in "real time".
 * </p>
 */
public final class HfsFileStore extends FileStoreBase {

    private final FSFolder root;

    /**
     * Constructor
     */
    public HfsFileStore(HFSCommonFileSystemHandler handler, final FileAttributesFactory factory) {
        super("hfs", factory, false);
        this.root = handler.getRoot();
Debug.println(Level.FINE, StringUtil.paramString(root));
Debug.println(Level.FINE, StringUtil.paramString(root.getAllForks()));
Debug.println(Level.FINE, StringUtil.paramString(root.getAttributes()));
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
        return 100000000;
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
        return 100000000;
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
        return 100000000;
    }
}
