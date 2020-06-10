/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.flickr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.SearchParameters;
import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Cache;
import vavi.nio.file.Util;


/**
 * FlickrFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class FlickrFileSystemDriver extends ExtendedFileSystemDriverBase {

    private final Flickr flickr;

    private boolean ignoreAppleDouble = false;

    @SuppressWarnings("unchecked")
    public FlickrFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final Flickr drive,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.flickr = drive;
        ignoreAppleDouble = (Boolean) ((Map<String, Object>) env).getOrDefault("ignoreAppleDouble", Boolean.FALSE);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
    }

    /** */
    private Cache<Photo> cache = new Cache<Photo>() {
        {
            Photo photo = new Photo();
            photo.setTitle("/");
            photo.setId("root");
            photo.setLastUpdate(new Date(0)); // TODO
            entryCache.put("/", photo);
        }

        /**
         * TODO when the parent is not cached
         * @see #ignoreAppleDouble
         */
        public Photo getEntry(Path path) throws IOException {
            String pathString = Util.toPathString(path);
            if (cache.containsFile(path)) {
//System.err.println("CACHE: path: " + path + ", id: " + cache.get(pathString).getId());
                return cache.getFile(path);
            } else {
                if (ignoreAppleDouble && path.getFileName() != null && Util.isAppleDouble(path)) {
                    throw new NoSuchFileException("ignore apple double file: " + path);
                }

                try {
                    Photo entry = flickr.getPhotosInterface().getInfo(pathString, null); // TODO
//System.err.println("GOT: path: " + path + ", id: " + entry.getId());
                    cache.putFile(path, entry);
                    return entry;
                } catch (FlickrException e) {
                    if (e.getMessage().startsWith("404")) { // TODO
                        throw new NoSuchFileException(path.toString());
                    } else {
                        throw new IOException(e);
                    }
                }
            }
        }
    };

    @Nonnull
    @Override
    public InputStream newInputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        final Photo entry = cache.getEntry(path);

        final java.io.File downloadFile = java.io.File.createTempFile("vavi-apps-fuse-", ".download");

        return new FlickrInputStream(flickr, entry, downloadFile);
    }

    @Nonnull
    @Override
    public OutputStream newOutputStream(final Path path, final Set<? extends OpenOption> options) throws IOException {
        try {
            cache.getEntry(path);

            throw new FileAlreadyExistsException("path: " + path);
        } catch (IOException e) {
            System.err.println("newOutputStream: " + e.getMessage());
        }

        java.io.File temp = java.io.File.createTempFile("vavi-apps-fuse-", ".upload");

        return new FlickrOutputStream(flickr, temp, Util.toFilenameString(path), newEntry -> {
            try {
System.out.println("file: " + newEntry.getTitle() + ", " + newEntry.getDateAdded());
                cache.addEntry(path, newEntry);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Nonnull
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                    final DirectoryStream.Filter<? super Path> filter) throws IOException {
        try {
            return Util.newDirectoryStream(getDirectoryEntries(dir), filter);
        } catch (FlickrException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException("createDirectory");
    }

    @Override
    public void delete(final Path path) throws IOException {
        final Photo entry = cache.getEntry(path);

        try {
            flickr.getPhotosInterface().delete(entry.getId());

            cache.removeEntry(path);
        } catch (FlickrException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void copy(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        Photo targetEntry;
        String targetFilename;
        try {
            targetEntry = cache.getEntry(target);
            flickr.getPhotosInterface().delete(targetEntry.getId());

            cache.removeEntry(target);

            targetEntry = cache.getEntry(target.getParent());
            targetFilename = Util.toFilenameString(target);
        } catch (FlickrException e) {
System.err.println(e);
            throw new IOException(e);
        }

        final Photo sourceEntry = cache.getEntry(source);
        Photo entry = new Photo();
        entry.setTitle(targetFilename);
        // TODO
        Photo newEntry = null; //flickr.getPhotosInterface().copy(sourceEntry.getId(), entry);

        cache.addEntry(target, newEntry);
    }

    @Override
    public void move(final Path source, final Path target, final Set<CopyOption> options) throws IOException {
        Photo targetEntry;
        String targetFilename;
        try {
            targetEntry = cache.getEntry(target);
            flickr.getPhotosInterface().delete(targetEntry.getId());

            cache.removeEntry(target);

            targetEntry = cache.getEntry(target.getParent());
            targetFilename = Util.toFilenameString(target);
        } catch (FlickrException e) {
System.err.println(e);
            throw new IOException(e);
        }

        Photo sourceEntry = cache.getEntry(source);
        sourceEntry.setTitle(targetFilename);
        // TODO
        Photo newEntry = null;//flickr.getPeopleInterface().update(sourceEntry.getId(), sourceEntry);

        cache.removeEntry(source);
        cache.addEntry(target, newEntry);
    }

    /**
     * Check access modes for a path on this filesystem
     * <p>
     * If no modes are provided to check for, this simply checks for the
     * existence of the path.
     * </p>
     *
     * @param path the path to check
     * @param modes the modes to check for, if any
     * @throws IOException filesystem level error, or a plain I/O error
     *                     if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     * @see FileSystemProvider#checkAccess(Path, AccessMode...)
     */
    @Override
    protected void checkAccessImpl(final Path path, final AccessMode... modes) throws IOException {
        cache.getEntry(path);

        // TODO: assumed; not a file == directory
        for (final AccessMode mode : modes) {
            if (mode == AccessMode.EXECUTE) {
                throw new AccessDeniedException(path.toString());
            }
        }
    }

    @Override
    public void close() throws IOException {
        // TODO: what to do here? Flickr does not implement Closeable :(
    }

    /**
     * @throws IOException if you use this with javafs (jnr-fuse), you should throw {@link NoSuchFileException} when the file not found.
     */
    @Nonnull
    @Override
    protected Object getPathMetadataImpl(final Path path) throws IOException {
        return cache.getEntry(path);
    }

    /** */
    private List<Path> getDirectoryEntries(final Path dir) throws IOException, FlickrException {
        List<Path> list = null;
        SearchParameters params = new SearchParameters();
        params.setMaxTakenDate(new Date());
        params.setMinTakenDate(new Date());
        final PhotoList<Photo> children = flickr.getPhotosInterface().search(params, 10, 0);
        list = new ArrayList<>(children.size());

        // TODO nextPageToken
        for (final Photo child : children) {
            Path childPath = dir.resolve(child.getTitle());
            list.add(childPath);
//System.err.println("child: " + childPath.toRealPath().toString());
            cache.addEntry(childPath, child);
        }

        return list;
    }
}
