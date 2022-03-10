/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.yetiz.lib.acd.ACD;
import org.yetiz.lib.acd.Entity.FileInfo;
import org.yetiz.lib.acd.Entity.FolderInfo;


/**
 * Wrapper over {@link java.io.File} extending {@link OutputStream}
 *
 * <p>
 * This class wraps a Amazon Cloud Drive downloader class by extending {@code
 * InputStream} and delegating all of its methods to the downloader's
 * included stream. As such, this means this class is usable in a
 * try-with-resources statement (which the Amazon Cloud Drive class isn't).
 * </p>
 *
 * <p>
 * Note about exception handling: unfortunately, the Amazon Cloud Drive API class used
 * to wrap an output stream defines a close method which is not declared to
 * throw an exception; which means it may throw none, or it may throw an
 * <em>unchecked</em> exception. As such, the {@link #close()} method of this
 * class captures all {@link RuntimeException}s which {@link
 * java.io.File#close()} may throw and wrap it into a {@link
 * IOException}. If the underlying output stream <em>did</em> throw an
 * exception, however, then such an exception is {@link
 * Throwable#addSuppressed(Throwable) suppressed}.
 * </p>
 */
// TODO: more complex than the input stuff; check again (.abort(), etc)
// TODO to be eliminated
public final class AcdOutputStream extends OutputStream {

    private final AtomicBoolean closeCalled = new AtomicBoolean(false);

    private ACD drive;
    private String filename;
    private java.io.File file;
    private FolderInfo folderInfo;
    private OutputStream out;

    private Consumer<FileInfo> consumer;

    public AcdOutputStream(ACD drive, @Nonnull final java.io.File file, String filename, FolderInfo folderInfo, Consumer<FileInfo> consumer) throws IOException {
        this.drive = drive;
        this.file = file;
        this.filename = filename;
        this.folderInfo = folderInfo;
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
         * AcdIOException if the stream was OK. */

//System.out.println("close: " + uploader.getUploadFile().length());
        FileInfo fileInfo = drive.uploadFile(folderInfo.getId(), filename, file);

        consumer.accept(fileInfo);

        if (exception != null)
            throw exception;
    }
}
