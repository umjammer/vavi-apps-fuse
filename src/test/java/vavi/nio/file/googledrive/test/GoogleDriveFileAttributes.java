/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive.test;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;


/**
 * GoogleDriveFileAttributes. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/05 umjammer initial version <br>
 */
public class GoogleDriveFileAttributes implements BasicFileAttributes {

    /* @see java.nio.file.attribute.BasicFileAttributes#lastModifiedTime() */
    @Override
    public FileTime lastModifiedTime() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#lastAccessTime() */
    @Override
    public FileTime lastAccessTime() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#creationTime() */
    @Override
    public FileTime creationTime() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#isRegularFile() */
    @Override
    public boolean isRegularFile() {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#isDirectory() */
    @Override
    public boolean isDirectory() {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#isSymbolicLink() */
    @Override
    public boolean isSymbolicLink() {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#isOther() */
    @Override
    public boolean isOther() {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#size() */
    @Override
    public long size() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see java.nio.file.attribute.BasicFileAttributes#fileKey() */
    @Override
    public Object fileKey() {
        // TODO Auto-generated method stub
        return null;
    }

}

/* */
