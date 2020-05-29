/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.flickr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;


/**
 * Wrapper over {@link java.io.File} extending {@link OutputStream}
 *
 * <p>
 * This class wraps a Flickr downloader class by extending {@code
 * InputStream} and delegating all of its methods to the downloader's
 * included stream. As such, this means this class is usable in a
 * try-with-resources statement (which the Flickr class isn't).
 * </p>
 *
 * <p>
 * Note about exception handling: unfortunately, the Flickr API class used
 * to wrap an output stream defines a close method which is not declared to
 * throw an exception; which means it may throw none, or it may throw an
 * <em>unchecked</em> exception. As such, the {@link #close()} method of this
 * class captures all {@link RuntimeException}s which {@link
 * java.io.File#close()} may throw and wrap it into a {@link
 * FlickrIOException}. If the underlying output stream <em>did</em> throw an
 * exception, however, then such an exception is {@link
 * Throwable#addSuppressed(Throwable) suppressed}.
 * </p>
 */
// TODO: more complex than the input stuff; check again (.abort(), etc)
public final class FlickrOutputStream extends OutputStream {

    private final AtomicBoolean closeCalled = new AtomicBoolean(false);

    private Flickr flickr;
    private String filename;
    private File file;
    private OutputStream out;

    private Consumer<Photo> consumer;

    public FlickrOutputStream(Flickr flcikr, @Nonnull final File file, String filename, Consumer<Photo> consumer) throws IOException {
        this.flickr = flcikr;
        this.file = file;
        this.filename = filename;
        this.consumer = consumer;
        out = new FileOutputStream(file);
    }

    @Override
    public void write(final int b) throws IOException {
System.out.println("here: 0");
        out.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
System.out.println("here: 1");
        out.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
System.out.println("here: 2");
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        /* Reentrancy: check if .close() has been called already... */
        if (closeCalled.getAndSet(true))
            return;

        /* TODO: UGLY! Tied to the API in a big, big way */
        IOException exception = null;

        /* First try and close the stream */
        try {
            out.close();
        } catch (IOException e) {
            exception = e;
        }

        /* First, .finish() the transaction; if this throws an exception, wrap
         * it in either the exception thrown by the OutputStream, or a new
         * FlickrIOException if the stream was OK. */

        try {
//System.out.println("close: " + uploader.getUploadFile().length());
            UploadMetaData metadata = new UploadMetaData();
            metadata.setTitle(filename);

            Uploader uploader = flickr.getUploader();

            String id = uploader.upload(file, metadata);

            consumer.accept(null);
        } catch (FlickrException e) {
            if (exception == null)
                exception = new IOException(e);
            else
                exception.addSuppressed(e);
        }

        if (exception != null)
            throw exception;
    }
}
