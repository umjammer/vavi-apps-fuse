/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive2;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;


/**
 * GoogleDriveFileAttributeView.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/05 umjammer initial version <br>
 */
public class GoogleDriveFileAttributeView implements BasicFileAttributeView {

    /* @see java.nio.file.attribute.BasicFileAttributeView#name() */
    @Override
    public String name() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.attribute.BasicFileAttributeView#readAttributes() */
    @Override
    public BasicFileAttributes readAttributes() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.attribute.BasicFileAttributeView#setTimes(java.nio.file.attribute.FileTime, java.nio.file.attribute.FileTime, java.nio.file.attribute.FileTime) */
    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        // TODO Auto-generated method stub

    }

}

/* */
