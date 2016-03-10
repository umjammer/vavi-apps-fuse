/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;


/**
 * GoogleDrivePath. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/05 umjammer initial version <br>
 */
public class GoogleDrivePath implements Path {

    /* @see java.nio.file.Path#getFileSystem() */
    @Override
    public FileSystem getFileSystem() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#isAbsolute() */
    @Override
    public boolean isAbsolute() {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.Path#getRoot() */
    @Override
    public Path getRoot() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#getFileName() */
    @Override
    public Path getFileName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#getParent() */
    @Override
    public Path getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#getNameCount() */
    @Override
    public int getNameCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see java.nio.file.Path#getName(int) */
    @Override
    public Path getName(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#subpath(int, int) */
    @Override
    public Path subpath(int beginIndex, int endIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#startsWith(java.nio.file.Path) */
    @Override
    public boolean startsWith(Path other) {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.Path#startsWith(java.lang.String) */
    @Override
    public boolean startsWith(String other) {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.Path#endsWith(java.nio.file.Path) */
    @Override
    public boolean endsWith(Path other) {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.Path#endsWith(java.lang.String) */
    @Override
    public boolean endsWith(String other) {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see java.nio.file.Path#normalize() */
    @Override
    public Path normalize() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#resolve(java.nio.file.Path) */
    @Override
    public Path resolve(Path other) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#resolve(java.lang.String) */
    @Override
    public Path resolve(String other) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#resolveSibling(java.nio.file.Path) */
    @Override
    public Path resolveSibling(Path other) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#resolveSibling(java.lang.String) */
    @Override
    public Path resolveSibling(String other) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#relativize(java.nio.file.Path) */
    @Override
    public Path relativize(Path other) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#toUri() */
    @Override
    public URI toUri() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#toAbsolutePath() */
    @Override
    public Path toAbsolutePath() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#toRealPath(java.nio.file.LinkOption[]) */
    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#toFile() */
    @Override
    public File toFile() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#register(java.nio.file.WatchService, java.nio.file.WatchEvent.Kind[], java.nio.file.WatchEvent.Modifier[]) */
    @Override
    public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#register(java.nio.file.WatchService, java.nio.file.WatchEvent.Kind[]) */
    @Override
    public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#iterator() */
    @Override
    public Iterator<Path> iterator() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see java.nio.file.Path#compareTo(java.nio.file.Path) */
    @Override
    public int compareTo(Path other) {
        // TODO Auto-generated method stub
        return 0;
    }

}

/* */
