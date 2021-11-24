/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.flickr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.SearchParameters;
import com.github.fge.filesystem.driver.CachedFileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemFactoryProvider;

import vavi.nio.file.Util;


/**
 * FlickrFileSystemDriver.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class FlickrFileSystemDriver extends CachedFileSystemDriver<Photo> {

    private final Flickr flickr;

    public FlickrFileSystemDriver(final FileStore fileStore,
            final FileSystemFactoryProvider provider,
            final Flickr drive,
            final Map<String, ?> env) throws IOException {
        super(fileStore, provider);
        this.flickr = drive;
        setEnv(env);
//System.err.println("ignoreAppleDouble: " + ignoreAppleDouble);
    }

    @Override
    protected String getFilenameString(Photo entry) {
    	return entry.getTitle();
    }

    @Override
    protected boolean isFolder(Photo entry) {
    	// flickr doesn't have folder capability
    	return false;
    }

    @Override
    protected Photo getRootEntry(Path root) throws IOException {
    	Photo photo = new Photo();
        photo.setTitle("/");
        photo.setId("root");
        photo.setLastUpdate(new Date(0)); // TODO
        return photo;
    }

    @Override
    protected Photo getEntry(Photo parentEntry, Path path)throws IOException {
        try {
            Photo entry = flickr.getPhotosInterface().getInfo(Util.toPathString(path), null); // TODO
//System.err.println("GOT: path: " + path + ", id: " + entry.getId());
            return entry;
        } catch (FlickrException e) {
            if (e.getMessage().startsWith("404")) { // TODO
                return null;
            } else {
                throw new IOException(e);
            }
        }
    }

    @Override
    protected InputStream downloadEntry(Photo entry, Path path, Set<? extends OpenOption> options) throws IOException {
        final java.io.File downloadFile = java.io.File.createTempFile("vavi-apps-fuse-", ".download");
        return new FlickrInputStream(flickr, entry, downloadFile);
    }

    @Override
    protected OutputStream uploadEntry(Photo parentEntry, Path path, Set<? extends OpenOption> options) throws IOException {
        java.io.File temp = java.io.File.createTempFile("vavi-apps-fuse-", ".upload");
        return new FlickrOutputStream(flickr, temp, Util.toFilenameString(path), newEntry -> {
            try {
System.out.println("file: " + newEntry.getTitle() + ", " + newEntry.getDateAdded());
                updateEntry(path, newEntry);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    protected List<Photo> getDirectoryEntries(Photo dirEntry, Path dir) throws IOException {
        try {
			SearchParameters params = new SearchParameters();
			Date now = new Date();
			params.setMaxTakenDate(now);
			params.setMinTakenDate(now);
			return flickr.getPhotosInterface().search(params, 10, 0);
		} catch (FlickrException e) {
			throw new IOException(e);
		}
    }

    @Override
    protected Photo createDirectoryEntry(Photo parentEntry, Path dir) throws IOException {
        throw new UnsupportedOperationException("flickr doesn't have folder capability");
    }

    @Override
    protected boolean hasChildren(Photo dirEntry, Path dir) throws IOException {
    	// flickr doesn't have folder capability
    	return false;
    }

    @Override
    protected void removeEntry(Photo entry, Path path) throws IOException {
        try {
            flickr.getPhotosInterface().delete(entry.getId());
        } catch (FlickrException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected Photo copyEntry(Photo sourceEntry, Photo targetParentEntry, Path source, Path target, Set<CopyOption> options) throws IOException {
//        try {
	        Photo entry = new Photo();
	        entry.setTitle(Util.toFilenameString(target));
	        // TODO
	        return null; //flickr.getPhotosInterface().copy(sourceEntry.getId(), entry);
//        } catch (FlickrException e) {
//            throw new IOException(e);
//        }
    }

    @Override
    protected Photo moveEntry(Photo sourceEntry, Photo targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
//        try {
	        sourceEntry.setTitle(Util.toFilenameString(target));
	        // TODO
	        return null; //flickr.getPeopleInterface().update(sourceEntry.getId(), sourceEntry);
//        } catch (FlickrException e) {
//            throw new IOException(e);
//        }
    }

    @Override
    protected Photo moveFolderEntry(Photo sourceEntry, Photo targetParentEntry, Path source, Path target, boolean targetIsParent) throws IOException {
        throw new UnsupportedOperationException("flickr doesn't have folder capability");
    }

    @Override
    protected Photo renameEntry(Photo sourceEntry, Photo targetParentEntry, Path source, Path target) throws IOException {
        throw new UnsupportedOperationException("flickr doesn't have folder capability");
    }
}
