/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.nuxeo.onedrive.client.OneDriveFile;
import org.nuxeo.onedrive.client.OneDriveJsonObject;
import org.nuxeo.onedrive.client.OneDriveUploadSession;

import vavi.util.Debug;


/**
 * Wrapper over {@link OneDriveUploadSession} extending {@link OutputStream}
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
 * OneDriveUploadSession#cancelUpload()} may throw and wrap it into a {@link
 * IOException}. If the underlying output stream <em>did</em> throw an
 * exception, however, then such an exception is {@link
 * Throwable#addSuppressed(Throwable) suppressed}.
 * </p>
 *
 * TODO: more complex than the input stuff; check again (.abort(), etc)
 */
public final class OneDriveOutputStream extends OutputStream {

    private final OneDriveUploadSession upload;
    private final Path file;
    private final AtomicBoolean close = new AtomicBoolean();
    private long offset = 0L;
    private int length;
    private OneDriveFile entry;
    private Consumer<OneDriveFile> consumer;

    public OneDriveOutputStream(final OneDriveUploadSession upload, final Path file, int length, Consumer<OneDriveFile> consumer) {
        this.upload = upload;
        this.file = file;
        this.length = length;
        this.consumer = consumer;
    }

    @Override
    public void write(final int b) throws IOException {
        throw new IOException(new UnsupportedOperationException());
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        final byte[] content = Arrays.copyOfRange(b, off, len);
        final String header = String.format("%d-%d/%d", offset, offset + content.length - 1, length);
Debug.printf("header %s", header);
        OneDriveJsonObject object = upload.uploadFragment(header, content);
        if (OneDriveFile.Metadata.class.isInstance(object)) {
            entry = OneDriveFile.Metadata.class.cast(object).getResource();
Debug.printf("Completed upload for %s", file);
        } else {
Debug.printf(Level.FINE, "Uploaded fragment %s for file %s", header, file);
        }
        offset += content.length;
    }

    @Override
    public void close() throws IOException {
        try {
            if (close.get()) {
Debug.printf(Level.WARNING, "Skip double close of stream %s", this);
                return;
            }
            if (0L == offset) {
Debug.printf(Level.WARNING, "Abort upload session %s with no completed parts", upload);
                // Use touch feature for empty file upload
                upload.cancelUpload();
            }
            consumer.accept(entry);
        } finally {
            close.set(true);
        }
    }
}
