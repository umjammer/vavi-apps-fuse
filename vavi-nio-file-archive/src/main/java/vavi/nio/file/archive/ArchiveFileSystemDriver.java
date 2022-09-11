/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.archive;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;
import vavi.util.Debug;
import vavi.util.archive.Archive;
import vavi.util.archive.Entry;


/**
 * ArchiveFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public final class ArchiveFileSystemDriver extends ExtendedFileSystemDriverBase {

    private final Archive archive;

    /**
     * @param env
     */
    public ArchiveFileSystemDriver(FileStore fileStore,
            FileSystemFactoryProvider provider,
            Archive archive,
            Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        setEnv(env);
        this.archive = archive;
    }

    // TODO research all archive type
    static String toArchiveString(Path path) {
        return path.toAbsolutePath().toString().substring(1);
    }

    /** */
    private Entry getEntry(Path path) throws FileNotFoundException{
        if (path.getNameCount() == 0) {
Debug.println(Level.FINE, "root");
            return null;
        }
Debug.println(Level.FINE, "entry: \"" + toArchiveString(path) + "\"");
        Entry entry = archive.getEntry(toArchiveString(path));
        if (entry == null) {
            throw new FileNotFoundException(path.toString());
        }
        return entry;
    }

    @Override
    public InputStream newInputStream(Path path, Set<? extends OpenOption> options) throws IOException {
        return archive.getInputStream(getEntry(path));
    }

    @Override
    public OutputStream newOutputStream(Path path, Set<? extends OpenOption> options) throws IOException {
        throw new UnsupportedOperationException("newOutputStream is not supported by the file system");
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir,
                                                    DirectoryStream.Filter<? super Path> filter) throws IOException {
        return Util.newDirectoryStream(getDirectoryEntries(dir), filter);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("createDirectory is not supported by the file system");
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new UnsupportedOperationException("delete is not supported by the file system");
    }

    @Override
    public void copy(Path source, Path target, Set<CopyOption> options) throws IOException {
        throw new UnsupportedOperationException("copy is not supported by the file system");
    }

    @Override
    public void move(Path source, Path target, Set<CopyOption> options) throws IOException {
        throw new UnsupportedOperationException("move is not supported by the file system");
    }

    @Override
    protected void checkAccessImpl(Path path, AccessMode... modes) throws IOException {
    }

    /**
     * @return null when path is root
     * @throws IOException if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     */
    @Override
    protected Object getPathMetadataImpl(final Path path) throws IOException {
        return getEntry(path);
    }

    @Override
    public void close() throws IOException {
        archive.close();
    }

    /** */
    private List<Path> getDirectoryEntries(final Path dir) throws IOException {
//Debug.println("dir: " + dir + " ---------");
        List<Path> list = new ArrayList<>();

        for (Entry entry : archive.entries()) {
            if (dir.getNameCount() == 0) {
                String[] names = entry.getName().split("/");
                if (names.length == 1) {
                    Path childPath = dir.resolve(entry.getName());
//Debug.println("root: " + childPath);
                    list.add(childPath);
                }
            } else {
                String dirString = toArchiveString(dir) + "/";
                if (entry.getName().startsWith(dirString)) {
                    String entryName = entry.getName().substring(dirString.length());
                    String[] names = entryName.split("/");
                    if (names.length == 1 && names[0].length() > 0) {
                        Path childPath = dir.resolve(entryName);
//Debug.println("sub : " + childPath);
                        list.add(childPath);
                    }
                }
            }
        }

        return list;
    }
}
