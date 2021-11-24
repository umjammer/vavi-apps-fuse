/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;

import static vavi.nio.file.Util.isAppleDouble;
import static vavi.nio.file.Util.toFilenameString;
import static vavi.nio.file.Util.toPathString;


/**
 * VfsFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class VfsFileSystemDriver extends ExtendedFileSystemDriver<FileObject> {

    private final FileSystemManager manager;

    private final FileSystemOptions opts;

    private final String baseUrl;

    /**
     * @param env { "baseUrl": "smb://10.3.1.1/Temporary Share/", "ignoreAppleDouble": boolean }
     */
    public VfsFileSystemDriver(FileStore fileStore,
            FileSystemFactoryProvider provider,
            FileSystemManager manager,
            FileSystemOptions options,
            String baseUrl,
            Map<String, ?> env) throws IOException {

    	super(fileStore, provider);

        setEnv(env);

        this.manager = manager;
        this.opts = options;

//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
        this.baseUrl = baseUrl;
    }

    @Override
    protected String getFilenameString(FileObject entry) {
    	return entry.getName().getBaseName();
    }

    @Override
    protected boolean isFolder(FileObject entry) throws IOException {
    	return entry.isFolder();
    }

    @Override
    protected boolean exists(FileObject entry) throws IOException {
    	return entry.exists();
    }

    // VFS might have cache?
    @Override
    protected FileObject getEntry(Path path) throws IOException {
        return getEntry(path, true);
    }

    /**
     * @param check check existence of the path
     */
    private FileObject getEntry(Path path, boolean check) throws IOException {
//Debug.println(Level.FINE, "path: " + path);
        if (ignoreAppleDouble && path.getFileName() != null && isAppleDouble(path)) {
            throw new NoSuchFileException("ignore apple double file: " + path);
        }

        FileObject entry = manager.resolveFile(baseUrl + toPathString(path), opts);
//Debug.println(Level.FINE, "entry: " + entry + ", " + entry.exists());
        if (check) {
            if (entry.exists()) {
                return entry;
            } else {
                throw new NoSuchFileException(path.toString());
            }
        } else {
            return entry;
        }
    }

    @Override
    protected InputStream downloadEntry(FileObject entry, Path path, Set<? extends OpenOption> options) throws IOException {
        return entry.getContent().getInputStream(Util.BUFFER_SIZE);
    }

    @Override
    protected OutputStream uploadEntry(FileObject parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        FileObject targetEntry = getEntry(path, false);
        targetEntry.createFile();
        return targetEntry.getContent().getOutputStream(Util.BUFFER_SIZE);
    }

    /** */
    protected List<FileObject> getDirectoryEntries(FileObject dirEntry, Path dir) throws IOException {
//System.err.println("path: " + dir);
//Arrays.stream(dirEntry.getChildren()).forEach(System.err::println);
    	return Arrays.stream(dirEntry.getChildren()).collect(Collectors.toList());
    }

    @Override
    protected FileObject createDirectoryEntry(FileObject parentEntry, Path dir) throws IOException {
        FileObject dirEntry = getEntry(dir, false);
        dirEntry.createFolder();
        return dirEntry;
    }

    @Override
    protected boolean hasChildren(FileObject dirEntry, Path dir) throws IOException {
    	return dirEntry.getChildren().length > 0;
    }

    @Override
    protected void removeEntry(FileObject entry, Path path) throws IOException {
        if (!entry.delete()) {
            throw new IOException("delete failed: " + path);
        }
    }

    @Override
    protected FileObject copyEntry(FileObject sourceEntry, FileObject targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
        FileObject targetEntry = getEntry(target, false);
        targetEntry.copyFrom(sourceEntry, Selectors.SELECT_ALL);
        return targetEntry;
    }

    @Override
    protected FileObject moveEntry(FileObject sourceEntry, FileObject targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        FileObject targetEntry = getEntry(targetIsParent ? target.resolve(toFilenameString(source)) : target, false);
        sourceEntry.moveTo(targetEntry);
        return targetEntry;
    }

    @Override
    protected FileObject moveFolderEntry(FileObject sourceEntry, FileObject targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        return moveEntry(sourceEntry, targetParentEntry, source, target, targetIsParent);
    }

    @Override
    protected FileObject renameEntry(FileObject sourceEntry, FileObject targetParentEntry, Path source, Path target) throws IOException {
        return moveEntry(sourceEntry, targetParentEntry, source, target, false);
    }

    // don't close the manager here, it will shutdown whole resources.
    // https://issues.apache.org/jira/browse/VFS-454
}
