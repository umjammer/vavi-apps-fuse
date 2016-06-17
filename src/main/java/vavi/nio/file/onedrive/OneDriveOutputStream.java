/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.file.OneFile;
import de.tuberlin.onedrivesdk.uploadFile.OneUploadFile;


/**
 * Wrapper over {@link OneUploadFile} extending {@link OutputStream}
 *
 * <p>
 * This class wraps a OneDrive downloader class by extending {@code
 * InputStream} and delegating all of its methods to the downloader's
 * included stream. As such, this means this class is usable in a
 * try-with-resources statement (which the OneDrive class isn't).
 * </p>
 *
 * <p>
 * Note about exception handling: unfortunately, the OneDrive API class used
 * to wrap an output stream defines a close method which is not declared to
 * throw an exception; which means it may throw none, or it may throw an
 * <em>unchecked</em> exception. As such, the {@link #close()} method of this
 * class captures all {@link RuntimeException}s which {@link
 * OneUploadFile#close()} may throw and wrap it into a {@link
 * OneDriveIOException}. If the underlying output stream <em>did</em> throw an
 * exception, however, then such an exception is {@link
 * Throwable#addSuppressed(Throwable) suppressed}.
 * </p>
 */
// TODO: more complex than the input stuff; check again (.abort(), etc)
public final class OneDriveOutputStream extends OutputStream {

    private final AtomicBoolean closeCalled = new AtomicBoolean(false);

    private final OneUploadFile uploader;
    
    private OutputStream out;

    private Consumer<OneFile> consumer;
    
    public OneDriveOutputStream(@Nonnull final OneUploadFile uploader, Consumer<OneFile> consumer) throws IOException, OneDriveException {
        this.uploader = uploader;
        this.consumer = consumer;
        out = new FileOutputStream(uploader.getUploadFile());
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
         * OneDriveIOException if the stream was OK. */

        try {
//System.out.println("close: " + uploader.getUploadFile().length());
            OneFile file = uploader.startUpload();
            consumer.accept(file);
        } catch (OneDriveException e) {
            if (exception == null)
                exception = new OneDriveIOException(e);
            else
                exception.addSuppressed(e);
        }

        if (exception != null)
            throw exception;
    }
}
