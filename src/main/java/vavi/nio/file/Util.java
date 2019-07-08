/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import vavi.util.Debug;


/**
 * Util.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
public interface Util {

    /** */
    public static String toPathString(Path path) throws IOException {
        return Normalizer.normalize(path.toRealPath().toString(), Form.NFC);
    }

    /** */
    public static String toFilenameString(Path path) throws IOException {
        return Normalizer.normalize(path.toRealPath().getFileName().toString(), Form.NFC);
    }

    /** @see "ignoreAppleDouble" */
    public static boolean isAppleDouble(Path path) throws IOException {
//System.err.println("path.toRealPath(): " + path.toRealPath());
//System.err.println("path.getFileName(): " + path.getFileName());
        String filename = path.getFileName().toString();
        return filename.startsWith("._") ||
               filename.equals(".DS_Store") ||
               filename.equals(".localized") ||
               filename.equals(".hidden");
    }

    /** */
    public static DirectoryStream<Path> newDirectoryStream(final List<Path> list) {
        return new DirectoryStream<Path>() {
            private final AtomicBoolean alreadyOpen = new AtomicBoolean(false);

            @Override
            public Iterator<Path> iterator() {
                // required by the contract
                if (alreadyOpen.getAndSet(true)) {
                    throw new IllegalStateException("already open");
                }
                return list.iterator();
            }

            @Override
            public void close() throws IOException {
            }
        };
    }

    /** */
    public static abstract class SeekableByteChannelForWriting implements SeekableByteChannel {
        protected long written;
        private WritableByteChannel wbc;

        public SeekableByteChannelForWriting(OutputStream out) throws IOException {
            this.wbc = Channels.newChannel(out);
            this.written = getLeftOver();
        }

        protected abstract long getLeftOver() throws IOException;

        @Override
        public boolean isOpen() {
            return wbc.isOpen();
        }

        @Override
        public long position() throws IOException {
            return written;
        }

        @Override
        public SeekableByteChannel position(long pos) throws IOException {
            written = pos;
            return this;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            throw new NonReadableChannelException();
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
Debug.println("writable byte channel: truncate: " + size + ", " + written);
            // TODO implement correctly

            if (written > size) {
                written = size;
            }

            return this;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int n = wbc.write(src);
Debug.println("writable byte channel: write: " + n);
            written += n;
            return n;
        }

        @Override
        public long size() throws IOException {
            return written;
        }

        @Override
        public void close() throws IOException {
Debug.println("writable byte channel: close");
            wbc.close();
        }
    }

    /** */
    public static abstract class SeekableByteChannelForReading implements SeekableByteChannel {
        private long read = 0;
        private ReadableByteChannel rbc;
        private long size;

        public SeekableByteChannelForReading(InputStream in) throws IOException {
            this.rbc = Channels.newChannel(in);
            this.size = getSize();
        }

        protected abstract long getSize() throws IOException;

        @Override
        public boolean isOpen() {
            return rbc.isOpen();
        }

        @Override
        public long position() throws IOException {
            return read;
        }

        @Override
        public SeekableByteChannel position(long pos) throws IOException {
            read = pos;
            return this;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int n = rbc.read(dst);
            if (n > 0) {
                read += n;
            }
            return n;
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            throw new NonWritableChannelException();
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            throw new NonWritableChannelException();
        }

        @Override
        public long size() throws IOException {
            return size;
        }

        @Override
        public void close() throws IOException {
            rbc.close();
        }
    }

    /** */
    public static abstract class OutputStreamForUploading extends OutputStream {
        private final AtomicBoolean closed = new AtomicBoolean();

        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public void write(final int b) throws IOException {
            baos.write(b);
        }

        @Override
        public void write(final byte[] b) throws IOException {
            baos.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            baos.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            baos.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                if (closed.get()) {
Debug.printf("Skip double close of stream %s", this);
                    return;
                }

                baos.close();

                upload(new ByteArrayInputStream(baos.toByteArray())); // TODO engine
            } finally {
                closed.set(true);
            }
        }

        protected abstract void upload(InputStream in) throws IOException;
    }
}

/* */
