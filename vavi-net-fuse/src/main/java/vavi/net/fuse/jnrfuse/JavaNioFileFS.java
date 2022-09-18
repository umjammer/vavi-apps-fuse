/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.jnrfuse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import jnr.constants.platform.Errno;
import jnr.ffi.Pointer;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.Flock;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import ru.serce.jnrfuse.struct.Statvfs;
import vavi.nio.file.Util;
import vavi.util.Debug;


/**
 * JavaNioFileFS. (jnr-fuse)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
class JavaNioFileFS extends FuseStubFS {

    private static final int BUFFER_SIZE = 8192;

    /** */
    private transient FileSystem fileSystem;

    /** */
    static final String ENV_NO_APPLE_DOUBLE = "no_apple_double";

    /** */
    private final AtomicLong fileHandle = new AtomicLong(0);

    /** <file handle, channel> */
    private final ConcurrentMap<Long, SeekableByteChannel> fileHandles = new ConcurrentHashMap<>();

    /**
     * @param fileSystem a file system to wrap by fuse
     */
    public JavaNioFileFS(FileSystem fileSystem, Map<String, Object> env) {
        this.fileSystem = fileSystem;
    }

    @Override
    public int access(String path, int access) {
Debug.println(Level.FINEST, "access: " + path);
        try {
            // TODO access
            fileSystem.provider().checkAccess(fileSystem.getPath(path));
            return 0;
        } catch (NoSuchFileException e) {
Debug.println(e);
            return -ErrorCodes.ENOENT();
        } catch (IOException e) {
Debug.println(e);
            return -ErrorCodes.EACCES();
        }
    }

    @Override
    public int create(String path, @mode_t long mode, FuseFileInfo info) {
Debug.println(Level.FINE, "create: " + path);
        try {
            Set<OpenOption> options = new HashSet<>();
            options.add(StandardOpenOption.WRITE);
            options.add(StandardOpenOption.CREATE_NEW);
            SeekableByteChannel channel = fileSystem.provider().newByteChannel(fileSystem.getPath(path), options);
            long fh = fileHandle.incrementAndGet();
            fileHandles.put(fh, channel);
            info.fh.set(fh);

            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int getattr(String path, FileStat stat) {
Debug.println(Level.FINEST, "getattr: " + path);
        try {
            BasicFileAttributes attributes =
                    fileSystem.provider().readAttributes(fileSystem.getPath(path), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

            if (attributes instanceof PosixFileAttributes) {
                long mode = JnrFuseFuse.permissionsToMode(((PosixFileAttributes) attributes).permissions());
                if (attributes.isDirectory()) {
                    stat.st_mode.set(FileStat.S_IFDIR | mode);
                    stat.st_mtim.tv_sec.set(attributes.lastModifiedTime().to(TimeUnit.SECONDS));
                    stat.st_ctim.tv_sec.set(attributes.creationTime().to(TimeUnit.SECONDS));
                } else {
                    stat.st_mode.set(FileStat.S_IFREG | mode);
                    stat.st_mtim.tv_sec.set(attributes.lastModifiedTime().to(TimeUnit.SECONDS));
                    stat.st_ctim.tv_sec.set(attributes.creationTime().to(TimeUnit.SECONDS));
                    stat.st_size.set(attributes.size());
                }
            } else {
                if (attributes.isDirectory()) {
                    stat.st_mode.set(FileStat.S_IFDIR | 0755);
                    stat.st_mtim.tv_sec.set(attributes.lastModifiedTime().to(TimeUnit.SECONDS));
                    stat.st_ctim.tv_sec.set(attributes.creationTime().to(TimeUnit.SECONDS));
                } else {
                    stat.st_mode.set(FileStat.S_IFREG | 0644);
                    stat.st_mtim.tv_sec.set(attributes.lastModifiedTime().to(TimeUnit.SECONDS));
                    stat.st_ctim.tv_sec.set(attributes.creationTime().to(TimeUnit.SECONDS));
                    stat.st_size.set(attributes.size());
                }
            }
            return 0;
        } catch (NoSuchFileException e) {
            if (e.getMessage().startsWith("ignore apple double file:")) {
Debug.println(Level.FINEST, e.getMessage());
                return 0;
            } else {
Debug.println(e);
                return -ErrorCodes.ENOENT();
            }
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int fgetattr(String path, FileStat stat, FuseFileInfo info)
    {
Debug.println(Level.FINE, "fgetattr: " + path);
        return getattr(path, stat);
    }

    @Override
    public int mkdir(String path, @mode_t long mode) {
Debug.println(Level.FINE, "mkdir: " + path);
        try {
            fileSystem.provider().createDirectory(fileSystem.getPath(path));
            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int open(String path, FuseFileInfo info) {
        try {
            Set<OpenOption> options = new HashSet<>();
            options.add(StandardOpenOption.READ);
            SeekableByteChannel channel = fileSystem.provider().newByteChannel(fileSystem.getPath(path), options);
            long fh = fileHandle.incrementAndGet();
            fileHandles.put(fh, channel);
            info.fh.set(fh);
Debug.println(Level.FINE, "open: " + path + ", fh: " + fh);

            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int read(String path, Pointer buf, long size, long offset, FuseFileInfo info) {
Debug.println(Level.FINE, "read: " + path + ", " + offset + ", " + size + ", fh: " + info.fh.get());
        try {
            if (fileHandles.containsKey(info.fh.get())) {
                SeekableByteChannel channel = fileHandles.get(info.fh.get());
                ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
                long pos = 0;
Debug.printf(Level.FINER, "Attempting to read %d-%d:", offset, offset + size);
                do {
                    bb.clear();
                    bb.limit((int) Math.min(bb.capacity(), size - pos));
                    int read = channel.read(bb);
                    if (read == -1) {
Debug.println(Level.FINER, "Reached EOF");
                        return (int) pos; // reached EOF TODO: wtf cast
                    } else {
Debug.printf(Level.FINER, "Reading %d-%d", offset + pos, offset + pos + read);
                        buf.put(pos, bb.array(), 0, read);
                        pos += read;
                    }
                } while (pos < size);
                return (int) pos;
            } else {
Debug.println(Level.FINE, "read: no fh: " + path);
                return -ErrorCodes.EEXIST();
            }
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filler, @off_t long offset, FuseFileInfo info) {
Debug.println(Level.FINER, "readdir: " + path);
        try {
            fileSystem.provider().newDirectoryStream(fileSystem.getPath(path), p -> true)
                .forEach(p -> {
Debug.println(Level.FINER, "p: " + p);
                    try {
                        filler.apply(buf, Util.toFilenameString(p), null, 0);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int rename(String path, String newName) {
Debug.println(Level.FINE, "rename: " + path);
        try {
            fileSystem.provider().move(fileSystem.getPath(path), fileSystem.getPath(newName));
            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int rmdir(String path) {
Debug.println(Level.FINE, "rmdir: " + path);
        try {
            fileSystem.provider().delete(fileSystem.getPath(path));
            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int truncate(String path, long offset) {
Debug.println(Level.FINE, "truncate: " + path);
        // TODO
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int unlink(String path) {
Debug.println(Level.FINE, "unlink: " + path);
        try {
            fileSystem.provider().delete(fileSystem.getPath(path));
            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo info) {
Debug.println(Level.FINE, "write: " + path + ", " + offset + ", " + size + ", fh: " + info.fh.get());
        try {
            if (fileHandles.containsKey(info.fh.get())) {
                ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
                long written = 0;
                SeekableByteChannel channel = fileHandles.get(info.fh.get());
try { // TODO ad-hoc
                channel.position(offset);
} catch (IOException e) {
 if (e.getMessage().contains("@vavi")) {
  long o = Long.parseLong(e.getMessage().substring(9, e.getMessage().length() - 1));
  if (offset > o) {
   Debug.println(Level.SEVERE, "write: skip bad position: " + offset);
   throw new IOException("cannot skip last bytes send", e);
  } else {
   Debug.println(Level.WARNING, "write: correct bad position: " + offset + " -> " + o);
   return Math.min((int) (o - offset), (int) size);
  }
 } else {
  throw e;
 }
}
                do {
                    long remaining = size - written;
                    bb.clear();
                    int len = (int) Math.min(remaining, bb.capacity());
                    buf.get(written, bb.array(), 0, len);
                    bb.limit(len);
                    int r = channel.write(bb);
                    written += r;
                } while (written < size);
                return (int) written;
            } else {
                return -ErrorCodes.EEXIST();
            }
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int statfs(String path, Statvfs stbuf) {
Debug.println(Level.FINEST, "statfs: " + path);
        try {
            FileStore fileStore = fileSystem.getFileStores().iterator().next();
//Debug.println("total: " + fileStore.getTotalSpace());
//Debug.println("free: " + fileStore.getUsableSpace());

            long blockSize = 512;

            long total = fileStore.getTotalSpace() / blockSize;
            long free = fileStore.getUsableSpace() / blockSize;
            long used = total - free;

            stbuf.f_bavail.set(used);
            stbuf.f_bfree.set(free);
            stbuf.f_blocks.set(total);
            stbuf.f_bsize.set(blockSize);
            stbuf.f_favail.set(-1);
            stbuf.f_ffree.set(-1);
            stbuf.f_files.set(-1);
            stbuf.f_frsize.set(1);

            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int chmod(String path, @mode_t long mode) {
Debug.println(Level.FINE, "chmod: " + path);
        try {
            if (fileSystem.provider().getFileStore(fileSystem.getPath(path)).supportsFileAttributeView(PosixFileAttributeView.class)) {
                PosixFileAttributeView attrs = fileSystem.provider().getFileAttributeView(fileSystem.getPath(path), PosixFileAttributeView.class);
                attrs.setPermissions(JnrFuseFuse.modeToPermissions(mode));
                return 0;
            } else {
                return -Errno.EAFNOSUPPORT.ordinal();
            }
        } catch (Exception e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int release(String path, FuseFileInfo info) {
Debug.println(Level.FINE, "release: " + path + ", fh: " + info.fh.get());
        try {
            if (fileHandles.containsKey(info.fh.get())) {
                Channel channel = fileHandles.get(info.fh.get());
                channel.close();
                return 0;
            } else {
Debug.println(Level.FINE, "release: no fh: " + path);
                return -ErrorCodes.EEXIST();
            }
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        } finally {
            fileHandles.remove(info.fh.get());
        }
    }

    @Override
    public int lock(String path, FuseFileInfo info, int cmd, Flock flock) {
Debug.println(Level.FINE, "lock: " + path + ", fh: " + info.fh.get());
        return 0;
    }
}
