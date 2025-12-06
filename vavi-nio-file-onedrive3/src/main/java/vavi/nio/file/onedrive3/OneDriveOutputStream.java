/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.nuxeo.onedrive.client.OneDriveJsonObject;
import org.nuxeo.onedrive.client.UploadSession;
import org.nuxeo.onedrive.client.types.DriveItem;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;
import static java.lang.System.getLogger;


/**
 * Wrapper over {@link UploadSession} extending {@link OutputStream}
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
 * UploadSession#cancelUpload()} may throw and wrap it into a {@link
 * IOException}. If the underlying output stream <em>did</em> throw an
 * exception, however, then such an exception is {@link
 * Throwable#addSuppressed(Throwable) suppressed}.
 * </p>
 *
 * TODO: more complex than the input stuff; check again (.abort(), etc)
 */
public final class OneDriveOutputStream extends OutputStream {

    private static final Logger logger = getLogger(OneDriveOutputStream.class.getName());

    private final UploadSession upload;
    private final Path file;
    private final AtomicBoolean close = new AtomicBoolean();
    private long offset = 0L;
    private final int length;
    private DriveItem.Metadata entry;
    private final Consumer<DriveItem.Metadata> consumer;

    public OneDriveOutputStream(final UploadSession upload, final Path file, int length, Consumer<DriveItem.Metadata> consumer) {
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
try {
        final byte[] content = Arrays.copyOfRange(b, off, off + len);
        final String header;
        if (length == -1) {
            header = "%d-%d/*".formatted(offset, offset + content.length - 1); // TODO got error response, not in the specs.?
        } else {
            header = "%d-%d/%d".formatted(offset, offset + content.length - 1, length);
        }
logger.log(DEBUG, "header %s".formatted(header));
        OneDriveJsonObject object = upload.uploadFragment(header, content);
        if (object instanceof DriveItem.Metadata) {
            entry = (DriveItem.Metadata) object;
logger.log(DEBUG, "Completed upload for %s".formatted(file));
        } else {
logger.log(DEBUG, "Uploaded fragment %s for file %s".formatted(header, file));
        }
        offset += content.length;
logger.log(DEBUG, "offset: %d (%d)".formatted(offset, content.length));
} catch (Throwable e) {
 logger.log(ERROR, e.getMessage(), e);
}
    }

    @Override
    public void close() throws IOException {
        try {
            if (close.get()) {
logger.log(WARNING, "Skip double close of stream %s".formatted(this));
                return;
            }
            if (0L == offset) {
logger.log(WARNING, "Abort upload session %s with no completed parts".formatted(upload));
                // Use touch feature for empty file upload
                upload.cancelUpload();
            }
            consumer.accept(entry);
        } finally {
            close.set(true);
        }
    }
}
