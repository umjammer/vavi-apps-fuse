/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;


/**
 * OneDriveFileAttributeView. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/05 umjammer initial version <br>
 */
public class OneDriveFileAttributeView implements BasicFileAttributeView {

    private final OneDrivePath path;

    private OneDriveFileAttributeView(OneDrivePath path) {
        this.path = path;
    }
    
    /* @see java.nio.file.attribute.BasicFileAttributeView#name() */
    @Override
    public String name() {
        return "onedrive";
    }

    /* @see java.nio.file.attribute.BasicFileAttributeView#readAttributes() */
    @Override
    public BasicFileAttributes readAttributes() throws IOException {
        return null;
    }

    /* @see java.nio.file.attribute.BasicFileAttributeView#setTimes(java.nio.file.attribute.FileTime, java.nio.file.attribute.FileTime, java.nio.file.attribute.FileTime) */
    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        path.setTimes(lastModifiedTime, lastAccessTime, createTime);
    }
}

/* */
