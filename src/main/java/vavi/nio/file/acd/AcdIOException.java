/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import java.io.Closeable;
import java.io.IOException;


/**
 * Class used as a wrapper over the Amazon Cloud Drive API's unchecked exceptions when
 * an {@link IOException} is needed
 *
 * <p>
 * The problem mainly comes from {@link File} and {@link
 * File}. Both of these methods define a {@code close()} method
 * but none of them implement {@link Closeable}. Worse than that, at least as
 * far as the uploader is concerned, this method is not even idempotent, and all
 * exceptions it throws are <strong>unchecked</strong>.
 * </p>
 *
 * <p>
 * The file system API, however, needs "correct" {@link IOException}s to be
 * thrown. We therefore capture all {@link RuntimeException}s thrown by either a
 * downloader or an uploader, and wrap it into such an exception.
 * </p>
 *
 * @see AcdInputStream
 * @see AcdOutputStream
 */
public final class AcdIOException extends IOException {

    public static AcdIOException wrap(final Exception e) {
        return new AcdIOException("GoogleDrive API error", e);
    }

    /**
     * Constructs an {@code IOException} with {@code null}
     * as its error detail message.
     */
    public AcdIOException() {
    }

    /**
     * Constructs an {@code IOException} with the specified detail message.
     *
     * @param message The detail message (which is saved for later retrieval
     *            by the {@link #getMessage()} method)
     */
    public AcdIOException(final String message) {
        super(message);
    }

    /**
     * Constructs an {@code IOException} with the specified detail message
     * and cause.
     * <p>
     * Note that the detail message associated with {@code cause} is
     * <i>not</i> automatically incorporated into this exception's detail
     * message.
     *
     * @param message The detail message (which is saved for later retrieval
     *            by the {@link #getMessage()} method)
     * @param cause The cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.)
     * @since 1.6
     */
    public AcdIOException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an {@code IOException} with the specified cause and a
     * detail message of {@code (cause==null ? null : cause.toString())}
     * (which typically contains the class and detail message of {@code cause}).
     * This constructor is useful for IO exceptions that are little more
     * than wrappers for other throwables.
     *
     * @param cause The cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.)
     * @since 1.6
     */
    public AcdIOException(final Throwable cause) {
        super(cause);
    }
}
