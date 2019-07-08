/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive2;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import de.tuberlin.onedrivesdk.common.OneItem;


/**
 * OneDriveFileAttributes.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/05 umjammer initial version <br>
 */
public class OneDriveFileAttributes implements BasicFileAttributes {

    /** */
    private final OneItem oneItem;

    /** */
    public OneDriveFileAttributes(OneItem oneItem) {
        this.oneItem = oneItem;
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#lastModifiedTime() */
    @Override
    public FileTime lastModifiedTime() {
        return FileTime.from(oneItem.getLastModifiedDateTime(), TimeUnit.SECONDS);
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#lastAccessTime() */
    @Override
    public FileTime lastAccessTime() {
        return FileTime.from(oneItem.getLastModifiedDateTime(), TimeUnit.SECONDS);
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#creationTime() */
    @Override
    public FileTime creationTime() {
        return FileTime.from(oneItem.getCreatedDateTime(), TimeUnit.SECONDS);
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#isRegularFile() */
    @Override
    public boolean isRegularFile() {
        return oneItem.isFile();
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#isDirectory() */
    @Override
    public boolean isDirectory() {
        return oneItem.isFolder();
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#isSymbolicLink() */
    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#isOther() */
    @Override
    public boolean isOther() {
        return false;
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#size() */
    @Override
    public long size() {
        return oneItem.getSize();
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#fileKey() */
    @Override
    public Object fileKey() {
        return null;
    }
}

/* */
