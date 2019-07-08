/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * OneDrivePath.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/05 umjammer initial version <br>
 */
public class OneDrivePath implements Path {

    private final OneDriveFileSystem ofs;
    private final byte[] path;
    private volatile int[] offsets;
    private int hashcode = 0;  // cached hashcode (created lazily)

    OneDrivePath(OneDriveFileSystem ofs, byte[] path) {
        this(ofs, path, false);
    }

    OneDrivePath(OneDriveFileSystem ofs, byte[] path, boolean normalized) {
        this.ofs = ofs;
        if (normalized)
            this.path = path;
        else
            this.path = normalize(path);
    }

    /* @see java.nio.file.Path#getFileSystem() */
    @Override
    public OneDriveFileSystem getFileSystem() {
        return ofs;
    }

    /* @see java.nio.file.Path#isAbsolute() */
    @Override
    public boolean isAbsolute() {
        return (this.path.length > 0 && path[0] == '/');
    }

    /* @see java.nio.file.Path#getRoot() */
    @Override
    public Path getRoot() {
        if (this.isAbsolute()) {
            return new OneDrivePath(ofs, new byte[]{ path[0] });
        } else {
            return null;
        }
    }

    /* @see java.nio.file.Path#getFileName() */
    @Override
    public Path getFileName() {
        initOffsets();
        int count = offsets.length;
        if (count == 0)
            return null;  // no elements so no name
        if (count == 1 && path[0] != '/')
            return this;
        int lastOffset = offsets[count-1];
        int len = path.length - lastOffset;
        byte[] result = new byte[len];
        System.arraycopy(path, lastOffset, result, 0, len);
        return new OneDrivePath(ofs, result);
    }

    /* @see java.nio.file.Path#getParent() */
    @Override
    public Path getParent() {
        initOffsets();
        int count = offsets.length;
        if (count == 0)    // no elements so no parent
            return null;
        int len = offsets[count-1] - 1;
        if (len <= 0)      // parent is root only (may be null)
            return getRoot();
        byte[] result = new byte[len];
        System.arraycopy(path, 0, result, 0, len);
        return new OneDrivePath(ofs, result);
    }

    /* @see java.nio.file.Path#getNameCount() */
    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    /* @see java.nio.file.Path#getName(int) */
    @Override
    public Path getName(int index) {
        initOffsets();
        if (index < 0 || index >= offsets.length)
            throw new IllegalArgumentException();
        int begin = offsets[index];
        int len;
        if (index == (offsets.length-1))
            len = path.length - begin;
        else
            len = offsets[index+1] - begin - 1;
        // construct result
        byte[] result = new byte[len];
        System.arraycopy(path, begin, result, 0, len);
        return new OneDrivePath(ofs, result);
    }

    /* @see java.nio.file.Path#subpath(int, int) */
    @Override
    public Path subpath(int beginIndex, int endIndex) {
        initOffsets();
        if (beginIndex < 0 ||
            beginIndex >=  offsets.length ||
            endIndex > offsets.length ||
            beginIndex >= endIndex)
            throw new IllegalArgumentException();

        // starting offset and length
        int begin = offsets[beginIndex];
        int len;
        if (endIndex == offsets.length)
            len = path.length - begin;
        else
            len = offsets[endIndex] - begin - 1;
        // construct result
        byte[] result = new byte[len];
        System.arraycopy(path, begin, result, 0, len);
        return new OneDrivePath(ofs, result);
    }

    /* @see java.nio.file.Path#startsWith(java.nio.file.Path) */
    @Override
    public boolean startsWith(Path other) {
        final OneDrivePath o = checkPath(other);
        if (o.isAbsolute() != this.isAbsolute() ||
            o.path.length > this.path.length)
            return false;
        int olast = o.path.length;
        for (int i = 0; i < olast; i++) {
            if (o.path[i] != this.path[i])
                return false;
        }
        olast--;
        return o.path.length == this.path.length ||
               o.path[olast] == '/' ||
               this.path[olast + 1] == '/';
    }

    /* @see java.nio.file.Path#startsWith(java.lang.String) */
    @Override
    public boolean startsWith(String other) {
        return startsWith(getFileSystem().getPath(other));
    }

    /* @see java.nio.file.Path#endsWith(java.nio.file.Path) */
    @Override
    public boolean endsWith(Path other) {
        final OneDrivePath o = checkPath(other);
        int olast = o.path.length - 1;
        if (olast > 0 && o.path[olast] == '/')
            olast--;
        int last = this.path.length - 1;
        if (last > 0 && this.path[last] == '/')
            last--;
        if (olast == -1)    // o.path.length == 0
            return last == -1;
        if ((o.isAbsolute() &&(!this.isAbsolute() || olast != last)) ||
            (last < olast))
            return false;
        for (; olast >= 0; olast--, last--) {
            if (o.path[olast] != this.path[last])
                return false;
        }
        return o.path[olast + 1] == '/' ||
               last == -1 || this.path[last] == '/';
    }

    /* @see java.nio.file.Path#endsWith(java.lang.String) */
    @Override
    public boolean endsWith(String other) {
        return endsWith(getFileSystem().getPath(other));
    }

    /* @see java.nio.file.Path#normalize() */
    @Override
    public Path normalize() {
        byte[] resolved = getResolved();
        if (resolved == path)    // no change
            return this;
        return new OneDrivePath(ofs, resolved, true);
    }

    /* @see java.nio.file.Path#resolve(java.nio.file.Path) */
    @Override
    public Path resolve(Path other) {
        final OneDrivePath o = checkPath(other);
        if (o.isAbsolute())
            return o;
        byte[] resolved = null;
        if (this.path[path.length - 1] == '/') {
            resolved = new byte[path.length + o.path.length];
            System.arraycopy(path, 0, resolved, 0, path.length);
            System.arraycopy(o.path, 0, resolved, path.length, o.path.length);
        } else {
            resolved = new byte[path.length + 1 + o.path.length];
            System.arraycopy(path, 0, resolved, 0, path.length);
            resolved[path.length] = '/';
            System.arraycopy(o.path, 0, resolved, path.length + 1, o.path.length);
        }
        return new OneDrivePath(ofs, resolved);
    }

    /* @see java.nio.file.Path#resolve(java.lang.String) */
    @Override
    public Path resolve(String other) {
        return resolve(getFileSystem().getPath(other));
    }

    /* @see java.nio.file.Path#resolveSibling(java.nio.file.Path) */
    @Override
    public Path resolveSibling(Path other) {
        if (other == null)
            throw new NullPointerException();
        Path parent = getParent();
        return (parent == null) ? other : parent.resolve(other);
    }

    /* @see java.nio.file.Path#resolveSibling(java.lang.String) */
    @Override
    public Path resolveSibling(String other) {
        return resolveSibling(getFileSystem().getPath(other));
    }

    /* @see java.nio.file.Path#relativize(java.nio.file.Path) */
    @Override
    public Path relativize(Path other) {
        final OneDrivePath o = checkPath(other);
        if (o.equals(this))
            return new OneDrivePath(getFileSystem(), new byte[0], true);
        if (/* this.getFileSystem() != o.getFileSystem() || */
            this.isAbsolute() != o.isAbsolute()) {
            throw new IllegalArgumentException();
        }
        int mc = this.getNameCount();
        int oc = o.getNameCount();
        int n = Math.min(mc, oc);
        int i = 0;
        while (i < n) {
            if (!equalsNameAt(o, i))
                break;
            i++;
        }
        int dotdots = mc - i;
        int len = dotdots * 3 - 1;
        if (i < oc)
            len += (o.path.length - o.offsets[i] + 1);
        byte[] result = new byte[len];

        int pos = 0;
        while (dotdots > 0) {
            result[pos++] = (byte)'.';
            result[pos++] = (byte)'.';
            if (pos < len)       // no tailing slash at the end
                result[pos++] = (byte)'/';
            dotdots--;
        }
        if (i < oc)
            System.arraycopy(o.path, o.offsets[i],
                             result, pos,
                             o.path.length - o.offsets[i]);
        return new OneDrivePath(getFileSystem(), result);
    }

    /* @see java.nio.file.Path#toUri() */
    @Override
    public URI toUri() {
        try {
            return new URI(ofs.provider().getScheme(),
                           new String(toAbsolutePath().path),
                           null);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    /* @see java.nio.file.Path#toAbsolutePath() */
    @Override
    public OneDrivePath toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        } else {
            //add / bofore the existing path
            byte[] defaultdir = new byte[] { '/' }; // TODO
            int defaultlen = defaultdir.length;
            boolean endsWith = (defaultdir[defaultlen - 1] == '/');
            byte[] t = null;
            if (endsWith)
                t = new byte[defaultlen + path.length];
            else
                t = new byte[defaultlen + 1 + path.length];
            System.arraycopy(defaultdir, 0, t, 0, defaultlen);
            if (!endsWith)
                t[defaultlen++] = '/';
            System.arraycopy(path, 0, t, defaultlen, path.length);
            return new OneDrivePath(ofs, t, true);  // normalized
        }
    }

    /* @see java.nio.file.Path#toRealPath(java.nio.file.LinkOption[]) */
    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        OneDrivePath realPath = new OneDrivePath(ofs, getResolvedPath()).toAbsolutePath();
        realPath.checkAccess();
        return realPath;
    }

    /* @see java.nio.file.Path#toFile() */
    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    /* @see java.nio.file.Path#register(java.nio.file.WatchService, java.nio.file.WatchEvent.Kind[], java.nio.file.WatchEvent.Modifier[]) */
    @Override
    public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
        if (watcher == null || events == null || modifiers == null) {
            throw new NullPointerException();
        }
        throw new UnsupportedOperationException();
    }

    /* @see java.nio.file.Path#register(java.nio.file.WatchService, java.nio.file.WatchEvent.Kind[]) */
    @Override
    public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {
        return register(watcher, events, new WatchEvent.Modifier[0]);
    }

    /* @see java.nio.file.Path#iterator() */
    @Override
    public Iterator<Path> iterator() {
        return new Iterator<Path>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return (i < getNameCount());
            }

            @Override
            public Path next() {
                if (i < getNameCount()) {
                    Path result = getName(i);
                    i++;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                throw new ReadOnlyFileSystemException();
            }
        };
    }

    /* @see java.nio.file.Path#compareTo(java.nio.file.Path) */
    @Override
    public int compareTo(Path other) {
        final OneDrivePath o = checkPath(other);
        int len1 = this.path.length;
        int len2 = o.path.length;

        int n = Math.min(len1, len2);
        byte v1[] = this.path;
        byte v2[] = o.path;

        int k = 0;
        while (k < n) {
            int c1 = v1[k] & 0xff;
            int c2 = v2[k] & 0xff;
            if (c1 != c2)
                return c1 - c2;
            k++;
        }
        return len1 - len2;
    }

    private boolean equalsNameAt(OneDrivePath other, int index) {
        int mbegin = offsets[index];
        int mlen = 0;
        if (index == (offsets.length-1))
            mlen = path.length - mbegin;
        else
            mlen = offsets[index + 1] - mbegin - 1;
        int obegin = other.offsets[index];
        int olen = 0;
        if (index == (other.offsets.length - 1))
            olen = other.path.length - obegin;
        else
            olen = other.offsets[index + 1] - obegin - 1;
        if (mlen != olen)
            return false;
        int n = 0;
        while(n < mlen) {
            if (path[mbegin + n] != other.path[obegin + n])
                return false;
            n++;
        }
        return true;
    }

    private OneDrivePath checkPath(Path path) {
        if (path == null) {
            throw new NullPointerException();
        }
        if (!OneDrivePath.class.isInstance(path)) {
            throw new ProviderMismatchException(path.toString());
        }
        return OneDrivePath.class.cast(path);
    }

    // create offset list if not already created
    private void initOffsets() {
        if (offsets == null) {
            int count, index;
            // count names
            count = 0;
            index = 0;
            while (index < path.length) {
                byte c = path[index++];
                if (c != '/') {
                    count++;
                    while (index < path.length && path[index] != '/')
                        index++;
                }
            }
            // populate offsets
            int[] result = new int[count];
            count = 0;
            index = 0;
            while (index < path.length) {
                byte c = path[index];
                if (c == '/') {
                    index++;
                } else {
                    result[count++] = index++;
                    while (index < path.length && path[index] != '/')
                        index++;
                }
            }
            synchronized (this) {
                if (offsets == null)
                    offsets = result;
            }
        }
    }

    // resolved path for locating zip entry inside the zip file,
    // the result path does not contain ./ and .. components
    private volatile byte[] resolved = null;

    byte[] getResolvedPath() {
        byte[] r = resolved;
        if (r == null) {
            if (isAbsolute())
                r = getResolved();
            else
                r = toAbsolutePath().getResolvedPath();
            if (r[0] == '/')
                r = Arrays.copyOfRange(r, 1, r.length);
            resolved = r;
        }
        return resolved;
    }

    // removes redundant slashs, replace "\" to zip separator "/"
    // and check for invalid characters
    private byte[] normalize(byte[] path) {
        if (path.length == 0)
            return path;
        byte prevC = 0;
        for (int i = 0; i < path.length; i++) {
            byte c = path[i];
            if (c == '\\')
                return normalize(path, i);
            if (c == (byte)'/' && prevC == '/')
                return normalize(path, i - 1);
            if (c == '\u0000')
                throw new InvalidPathException(new String(path), "Path: nul character not allowed");
            prevC = c;
        }
        return path;
    }

    private byte[] normalize(byte[] path, int off) {
        byte[] to = new byte[path.length];
        int n = 0;
        while (n < off) {
            to[n] = path[n];
            n++;
        }
        int m = n;
        byte prevC = 0;
        while (n < path.length) {
            byte c = path[n++];
            if (c == (byte)'\\')
                c = (byte)'/';
            if (c == (byte)'/' && prevC == (byte)'/')
                continue;
            if (c == '\u0000')
                throw new InvalidPathException(new String(path), "Path: nul character not allowed");
            to[m++] = c;
            prevC = c;
        }
        if (m > 1 && to[m - 1] == '/')
            m--;
        return (m == to.length)? to : Arrays.copyOf(to, m);
    }

    // Remove DotSlash(./) and resolve DotDot (..) components
    private byte[] getResolved() {
        if (path.length == 0)
            return path;
        for (int i = 0; i < path.length; i++) {
            byte c = path[i];
            if (c == (byte)'.')
                return resolve0();
        }
        return path;
    }

    // TBD: performance, avoid initOffsets
    private byte[] resolve0() {
        byte[] to = new byte[path.length];
        int nc = getNameCount();
        int[] lastM = new int[nc];
        int lastMOff = -1;
        int m = 0;
        for (int i = 0; i < nc; i++) {
            int n = offsets[i];
            int len = (i == offsets.length - 1)?
                      (path.length - n):(offsets[i + 1] - n - 1);
            if (len == 1 && path[n] == (byte)'.') {
                if (m == 0 && path[0] == '/')   // absolute path
                    to[m++] = '/';
                continue;
            }
            if (len == 2 && path[n] == '.' && path[n + 1] == '.') {
                if (lastMOff >= 0) {
                    m = lastM[lastMOff--];  // retreat
                    continue;
                }
                if (path[0] == '/') {  // "/../xyz" skip
                    if (m == 0)
                        to[m++] = '/';
                } else {               // "../xyz" -> "../xyz"
                    if (m != 0 && to[m-1] != '/')
                        to[m++] = '/';
                    while (len-- > 0)
                        to[m++] = path[n++];
                }
                continue;
            }
            if (m == 0 && path[0] == '/' ||   // absolute path
                m != 0 && to[m-1] != '/') {   // not the first name
                to[m++] = '/';
            }
            lastM[++lastMOff] = m;
            while (len-- > 0)
                to[m++] = path[n++];
        }
        if (m > 1 && to[m - 1] == '/')
            m--;
        return (m == to.length)? to : Arrays.copyOf(to, m);
    }

    void checkAccess(AccessMode... modes) throws IOException {
        boolean w = false;
        boolean x = false;
        for (AccessMode mode : modes) {
            switch (mode) {
            case READ:
                break;
            case WRITE:
                w = true;
                break;
            case EXECUTE:
                x = true;
                break;
            }
        }
        OneDriveFileAttributes attrs = ofs.getFileAttributes(getResolvedPath());
        if (attrs == null && (path.length != 1 || path[0] != '/')) {
            throw new NoSuchFileException(toString());
        }
        if (w) {
            if (ofs.isReadOnly()) {
                throw new AccessDeniedException(toString());
            }
        }
        if (x) {
            throw new AccessDeniedException(toString());
        }
    }

    void setTimes(FileTime mtime, FileTime atime, FileTime ctime) throws IOException {
        ofs.setTimes(getResolvedPath(), mtime, atime, ctime);
    }

    @Override
    public String toString() {
        return new String(path);
    }

    @Override
    public int hashCode() {
        int h = hashcode;
        if (h == 0)
            hashcode = h = Arrays.hashCode(path);
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null &&
               OneDrivePath.class.isInstance(obj) &&
               this.ofs == OneDrivePath.class.cast(obj).ofs &&
               compareTo(Path.class.cast(obj)) == 0;
    }
}

/* */
