/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;


/**
 * Wrapper over {@link java.io.File} extending {@link OutputStream}
 *
 * <p>
 * This class wraps a GoogleDrive downloader class by extending {@code
 * InputStream} and delegating all of its methods to the downloader's
 * included stream. As such, this means this class is usable in a
 * try-with-resources statement (which the GoogleDrive class isn't).
 * </p>
 *
 * <p>
 * Note about exception handling: unfortunately, the GoogleDrive API class used
 * to wrap an output stream defines a close method which is not declared to
 * throw an exception; which means it may throw none, or it may throw an
 * <em>unchecked</em> exception. As such, the {@link #close()} method of this
 * class captures all {@link RuntimeException}s which {@link
 * java.io.File#close()} may throw and wrap it into a {@link
 * GoogleDriveIOException}. If the underlying output stream <em>did</em> throw an
 * exception, however, then such an exception is {@link
 * Throwable#addSuppressed(Throwable) suppressed}.
 * </p>
 */
// TODO: more complex than the input stuff; check again (.abort(), etc)
public final class GoogleDriveOutputStream extends OutputStream {

    private final AtomicBoolean closeCalled = new AtomicBoolean(false);

    private Drive drive;
    private String filename;
    private java.io.File file;
    private OutputStream out;

    private Consumer<File> consumer;
    
    public GoogleDriveOutputStream(Drive drive, @Nonnull final java.io.File file, String filename, Consumer<File> consumer) throws IOException {
        this.drive = drive;
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
         * GoogleDriveIOException if the stream was OK. */

        try {
//System.out.println("close: " + uploader.getUploadFile().length());
            File fileMetadata = new File();
            fileMetadata.setName(filename);

            FileContent mediaContent = new FileContent(null, file);

            Drive.Files.Create insert = drive.files().create(fileMetadata, mediaContent);
            MediaHttpUploader uploader = insert.getMediaHttpUploader();
            uploader.setDirectUploadEnabled(true);
            uploader.setProgressListener(System.err::println);
            File file = insert.setFields("id, name, size, mimeType, createdTime").execute(); // TODO file is not finished status!

            consumer.accept(file);
        } catch (IOException e) {
            if (exception == null)
                exception = new GoogleDriveIOException(e);
            else
                exception.addSuppressed(e);
        }

        if (exception != null)
            throw exception;
    }
}
