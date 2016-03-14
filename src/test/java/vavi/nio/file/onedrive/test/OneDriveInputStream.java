
package vavi.nio.file.onedrive.test;

import com.github.fge.filesystem.driver.FileSystemDriver;

import de.tuberlin.onedrivesdk.downloadFile.OneDownloadFile;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;


/**
 * Wrapper over {@link DbxClient.Downloader} extending {@link InputStream}
 *
 * <p>
 * This class wraps a DropBox downloader class by extending {@code
 * InputStream} and delegating all of its methods to the downloader's
 * included stream. As such, this means this class is usable in a
 * try-with-resources statement (which the DropBox class isn't).
 * </p>
 *
 * <p>
 * Note about exception handling: unfortunately, the DropBox API class used
 * to wrap an input stream defines a close method which is not declared to
 * throw an exception; which means it may throw none, or it may throw an
 * <em>unchecked</em> exception. As such, the {@link #close()} method of this
 * class captures all {@link RuntimeException}s which {@link
 * OneDownloadFile#close()} may throw and wrap it into a {@link
 * OneDriveIOException}. If the underlying input stream <em>did</em> throw an
 * exception, however, then such an exception is {@link
 * Throwable#addSuppressed(Throwable) suppressed}.
 * </p>
 *
 * @see FileSystemDriver#newInputStream(Path, OpenOption...)
 */
@ParametersAreNonnullByDefault
public final class OneDriveInputStream extends InputStream {

    private final OneDownloadFile downloader;

    private final InputStream delegate;

    public OneDriveInputStream(final OneDownloadFile downloader) {
        this.downloader = downloader;
        delegate = null;
//        delegate = downloader.getInputStream();
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
        IOException exception = null;

        try {
            delegate.close();
        } catch (IOException e) {
            exception = e;
        }

        try {
//            downloader.close();
        } catch (RuntimeException e) {
            if (exception != null)
                exception.addSuppressed(e);
            else
                exception = new IOException("tell me what to do, please", e);
        }

        if (exception != null)
            throw exception;
    }
}
