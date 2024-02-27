/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.flickr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.imageio.ImageIO;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Photo;


/**
 * Wrapper over {@link File} extending {@link InputStream}
 *
 * <p>
 * This class wraps a Flickr downloader class by extending {@code
 * InputStream} and delegating all of its methods to the downloader's
 * included stream. As such, this means this class is usable in a
 * try-with-resources statement (which the DropBox class isn't).
 * </p>
 *
 * <p>
 * Note about exception handling: unfortunately, the Flickr API class used
 * to wrap an input stream defines a close method which is not declared to
 * throw an exception; which means it may throw none, or it may throw an
 * <em>unchecked</em> exception. As such, the {@link #close()} method of this
 * class captures all {@link RuntimeException}s which {@link
 * #close()} may throw and wrap it into a {@link
 * IOException}. If the underlying input stream <em>did</em> throw an
 * exception, however, then such an exception is {@link
 * Throwable#addSuppressed(Throwable) suppressed}.
 * </p>
 *
 * @see java.nio.file.Files#newInputStream(Path, OpenOption...)
 */
@ParametersAreNonnullByDefault
public final class FlickrInputStream extends InputStream {

    private final InputStream delegate;

    public FlickrInputStream(Flickr flickr, Photo file, final File downloadFile) throws IOException {
        try {
            Photo nfo = flickr.getPhotosInterface().getInfo(file.getId(), null);
            if (nfo.getOriginalSecret().isEmpty()) {
                ImageIO.write(file.getLargeImage(), file.getOriginalFormat(), downloadFile);
                System.out.println(file.getTitle() + "\t" + file.getLargeUrl() + " was written to " + downloadFile.getName());
            } else {
                file.setOriginalSecret(nfo.getOriginalSecret());
                ImageIO.write(file.getOriginalImage(), file.getOriginalFormat(), downloadFile);
                System.out.println(file.getTitle() + "\t" + file.getOriginalUrl() + " was written to " + downloadFile.getName());
            }
        } catch (FlickrException e) {
            throw new IOException(e);
        }
        delegate = new FileInputStream(downloadFile);
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return delegate.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public synchronized void mark(final int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
