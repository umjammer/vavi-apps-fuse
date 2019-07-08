/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static vavi.nio.file.Util.toPathString;


/**
 * Cache.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/16 umjammer initial version <br>
 */
public abstract class Cache<T> {
    /** <NFC normalized path {@link String}, {@link T}> */
    protected Map<String, T> entryCache = new HashMap<>(); // TODO refresh

    /** <NFC normalized path {@link String}, {@link List<Path>}> */
    protected Map<String, List<Path>> folderCache = new HashMap<>(); // TODO refresh

    /** */
    public boolean containsFile(Path path) throws IOException {
        return entryCache.containsKey(toPathString(path));
    }

    /** */
    public T getFile(Path path) throws IOException {
//System.err.println("CACHE: path: " + path + ", id: " + entryCache.get(path).getId());
        return entryCache.get(toPathString(path));
    }

    /** */
    public T putFile(Path path, T entry) throws IOException {
//System.err.println("GOT: path: " + path + ", id: " + entry.getId());
        return entryCache.put(toPathString(path), entry);
    }

    public boolean containsFolder(Path path) throws IOException {
        return folderCache.containsKey(toPathString(path));
    }

    /** */
    public List<Path> getFolder(Path path) throws IOException {
        return folderCache.get(toPathString(path));
    }

    /** */
    public int getChildCount(Path path) throws IOException {
        return containsFolder(path) ? getFolder(path).size() : -1;
    }

    /** */
    public List<Path> putFolder(Path path, List<Path> children) throws IOException {
        return folderCache.put(toPathString(path), children);
    }

    /** */
    public void addEntry(Path path, T entry) throws IOException {
//new Exception("*** ADD ***").printStackTrace();
        entryCache.put(toPathString(path), entry);
//System.out.println("CACHE A1: " + path);
        Path parentPath = path.getParent();
        List<Path> paths = folderCache.get(toPathString(parentPath));
        if (paths == null) {
            paths = new ArrayList<>();
            folderCache.put(toPathString(parentPath), paths);
        }
        paths.add(path);
//paths.forEach(p -> System.out.println("CACHE A2: " + p));
    }

    /** */
    public void removeEntry(Path path) throws IOException {
//new Exception("*** DELETE ***").printStackTrace();
        entryCache.remove(toPathString(path));
//System.out.println("CACHE D1: " + path);
        List<Path> paths = folderCache.get(toPathString(path.getParent()));
        if (paths != null) {
            paths.remove(path);
//paths.forEach(p -> System.out.println("CACHE D2: " + p));
        }
    }

    /** TODO for onedrive only check!!!!! */
    public void removeCache(Path path) throws IOException {
        entryCache.remove(toPathString(path));
        folderCache.remove(toPathString(path));
    }

    /**
     * @throws NoSuchFileException must be thrown when the path is not found.
     */
    public abstract T getEntry(Path path) throws IOException;

    /**
     * @throws NoSuchFileException is thrown when the path is not found.
     */
    public boolean existsEntry(Path path) throws IOException {
        try {
            getEntry(path);
            return true;
        } catch (NoSuchFileException e) {
            return false;
        }
    }
}

/* */
