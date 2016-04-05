/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive.test;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.HashMap;
import java.util.Map;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About.StorageQuota;


/**
 * GoogleDriveFileStore. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/05 umjammer initial version <br>
 */
public class GoogleDriveFileStore extends FileStore {

    /* @see java.nio.file.FileStore#name() */
    @Override
    public String name() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.FileStore#type() */
    @Override
    public String type() {
        return "googledrive";
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
    private Drive drive;
    
    /** */
    private Map<String, Object> getAttributs() throws IOException {
        StorageQuota quota = drive.about().get().execute().getStorageQuota();
//Debug.println("total: " + quota.getTotal());
//Debug.println("used: " + quota.getUsed());
    
        long blockSize = 512;

        long total = quota.getLimit() / blockSize;
        long used = quota.getUsage() / blockSize;
        long free = total - used;

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("bavail", total - free);
        attributes.put("bfree", free);
        attributes.put("blocks", total);
        attributes.put("bsize", blockSize);
        
        return attributes;
    }
}

/* */
