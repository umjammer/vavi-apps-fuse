/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive2;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.HashMap;
import java.util.Map;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.drive.DriveQuota;


/**
 * OneDriveFileStore.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/05 umjammer initial version <br>
 */
public class OneDriveFileStore extends FileStore {

    private final OneDrivePath oneDrivePath;

    /**
     * @param oneDrivePath
     */
    public OneDriveFileStore(OneDrivePath oneDrivePath) {
        this.oneDrivePath = oneDrivePath;
    }

    /* @see java.nio.file.FileStore#name() */
    @Override
    public String name() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.FileStore#type() */
    @Override
    public String type() {
        return "onedrive";
    }

    /* @see java.nio.file.FileStore#isReadOnly() */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /* @see java.nio.file.FileStore#getTotalSpace() */
    @Override
    public long getTotalSpace() throws IOException {
        return Long.class.cast(getAttributs().get("blocks"));
    }

    /* @see java.nio.file.FileStore#getUsableSpace() */
    @Override
    public long getUsableSpace() throws IOException {
        return Long.class.cast(getAttributs().get("bavail"));
    }

    /* @see java.nio.file.FileStore#getUnallocatedSpace() */
    @Override
    public long getUnallocatedSpace() throws IOException {
        return 0;
    }

    /* @see java.nio.file.FileStore#supportsFileAttributeView(java.lang.Class) */
    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return false;
    }

    /* @see java.nio.file.FileStore#supportsFileAttributeView(java.lang.String) */
    @Override
    public boolean supportsFileAttributeView(String name) {
        return false;
    }

    /* @see java.nio.file.FileStore#getFileStoreAttributeView(java.lang.Class) */
    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        return null;
    }

    /* @see java.nio.file.FileStore#getAttribute(java.lang.String) */
    @Override
    public Object getAttribute(String attribute) throws IOException {
        return getAttributs().get(attribute);
    }

    /** */
    private OneDriveSDK api;

    /** */
    private Map<String, Object> getAttributs() throws IOException {
        try {
            DriveQuota quota = api.getDefaultDrive().getQuota();
//Debug.println("total: " + quota.getTotal());
//Debug.println("used: " + quota.getUsed());

            long blockSize = 512;

            long total = quota.getTotal() / blockSize;
            long used = quota.getUsed() / blockSize;
            long free = total - used;

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("bavail", total - free);
            attributes.put("bfree", free);
            attributes.put("blocks", total);
            attributes.put("bsize", blockSize);

            return attributes;
        } catch (OneDriveException e) {
            throw new IOException(e);
        }
    }
}

/* */
