/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;


/**
 * Wrapper over {@link File} extending {@link InputStream}
 *
 * <p>
 * This class wraps a GoogleDrive downloader class by extending {@code
 * InputStream} and delegating all of its methods to the downloader's
 * included stream. As such, this means this class is usable in a
 * try-with-resources statement (which the DropBox class isn't).
 * </p>
 *
 * <p>
 * Note about exception handling: unfortunately, the GoogleDrive API class used
 * to wrap an input stream defines a close method which is not declared to
 * throw an exception; which means it may throw none, or it may throw an
 * <em>unchecked</em> exception. As such, the {@link #close()} method of this
 * class captures all {@link RuntimeException}s which {@link
 * com.google.api.client.googleapis.media.MediaHttpDownloader#close()} may throw and wrap it into a {@link
 * GoogleDriveIOException}. If the underlying input stream <em>did</em> throw an
 * exception, however, then such an exception is {@link
 * Throwable#addSuppressed(Throwable) suppressed}.
 * </p>
 *
 * @see FileSystemDriver#newInputStream(Path, OpenOption...)
 */
@ParametersAreNonnullByDefault
public final class GoogleDriveInputStream extends InputStream {

    private final InputStream delegate;
    
    public GoogleDriveInputStream(Drive drive, File file, final java.io.File downloadFile) throws IOException {
        OutputStream out = new FileOutputStream(downloadFile);
//        MediaHttpDownloader downloader = new MediaHttpDownloader(httpTransport, drive.getRequestFactory().getInitializer());
//        downloader.setDirectDownloadEnabled(true);
//        downloader.setProgressListener(System.err::println);
//        downloader.download(new GenericUrl(uploadedFile.toURI()), out);        
        drive.files().get(file.getId()).executeMediaAndDownloadTo(out);
        out.close();
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
