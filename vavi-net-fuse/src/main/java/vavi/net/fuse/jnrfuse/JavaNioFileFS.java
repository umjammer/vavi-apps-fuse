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
import java.nio.file.AccessDeniedException;
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

import vavi.util.Debug;

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


/**
 * JavaFsFS. (jnr-fuse)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
class JavaNioFileFS extends FuseStubFS {

    private static final int BUFFER_SIZE = 4096;

    /** */
    private transient FileSystem fileSystem;

    /** */
    static final String ENV_NO_APPLE_DOUBLE = "no_apple_double";

    /** */
    private final AtomicLong fileHandle = new AtomicLong(0);

    /** <file handle, channel> */
    private final ConcurrentMap<Long, SeekableByteChannel> fileHandles = new ConcurrentHashMap<>();

    /**
     * @param fileSystem
     */
    public JavaNioFileFS(FileSystem fileSystem, Map<String, Object> env) throws IOException {
        this.fileSystem = fileSystem;
    }

    @Override
    public int access(String path, int access) {
Debug.println(Level.FINE, "access: " + path);
        try {
            // TODO access
            fileSystem.provider().checkAccess(fileSystem.getPath(path));
            return 0;
        } catch (NoSuchFileException e) {
Debug.println(e);
            return -ErrorCodes.ENOENT();
        } catch (AccessDeniedException e) {
Debug.println(e);
            return -ErrorCodes.EACCES();
        } catch (IOException e) {
Debug.println(e);
            return -ErrorCodes.EACCES();
        }
    }

    @Override
    public int create(String path, @mode_t long mode, FuseFileInfo fi) {
Debug.println("create: " + path);
        try {
            Set<OpenOption> options = new HashSet<>();
            options.add(StandardOpenOption.WRITE);
            options.add(StandardOpenOption.CREATE_NEW);
            SeekableByteChannel channel = fileSystem.provider().newByteChannel(fileSystem.getPath(path), options);
            long fh = fileHandle.incrementAndGet();
            fileHandles.put(fh, channel);
            fi.fh.set(fh);

            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int getattr(String path, FileStat stat) {
Debug.println(Level.FINE, "getattr: " + path);
        try {
            BasicFileAttributes attributes =
                    fileSystem.provider().readAttributes(fileSystem.getPath(path), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

            if (attributes instanceof PosixFileAttributes) {
                long mode = JnrFuseFuse.permissionsToMode(PosixFileAttributes.class.cast(attributes).permissions());
                if (attributes.isDirectory()) {
                    stat.st_mode.set(FileStat.S_IFDIR | mode);
                    stat.st_atim.tv_sec.set(attributes.lastModifiedTime().to(TimeUnit.SECONDS));
                } else {
                    stat.st_mode.set(FileStat.S_IFREG | mode);
                    stat.st_atim.tv_sec.set(attributes.lastModifiedTime().to(TimeUnit.SECONDS));
                    stat.st_size.set(attributes.size());
                }
            } else {
                if (attributes.isDirectory()) {
                    stat.st_mode.set(FileStat.S_IFDIR | 0755);
                    stat.st_atim.tv_sec.set(attributes.lastModifiedTime().to(TimeUnit.SECONDS));
                } else {
                    stat.st_mode.set(FileStat.S_IFREG | 0644);
                    stat.st_atim.tv_sec.set(attributes.lastModifiedTime().to(TimeUnit.SECONDS));
                    stat.st_size.set(attributes.size());
                }
            }
            return 0;
        } catch (NoSuchFileException e) {
Debug.println(e);
            return -ErrorCodes.ENOENT();
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
Debug.println("mkdir: " + path);
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
Debug.println("open: " + path);
        try {
            Set<OpenOption> options = new HashSet<>();
            options.add(StandardOpenOption.READ);
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
    public int read(String path, Pointer buf, long num, long offset, FuseFileInfo info) {
Debug.println("read: " + path + ", " + offset);
        try {
            SeekableByteChannel channel = fileHandles.get(info.fh.get());
            ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
            long pos = 0;
Debug.printf("Attempting to read %d-%d:", offset, offset + num);
            do {
                bb.clear();
                bb.limit((int) Math.min(bb.capacity(), num));
                int read = channel.read(bb);
                if (read == -1) {
Debug.println("Reached EOF");
                    return (int) pos; // reached EOF TODO: wtf cast
                } else {
Debug.printf("Reading %d-%d", offset + pos, offset + pos + read);
                    buf.put(pos, bb.array(), 0, read);
                    pos += read;
                }
            } while (pos < num);
            return (int) pos;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    // TODO https://github.com/SerCeMan/jnr-fuse/issues/72
    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filler, @off_t long offset, FuseFileInfo fi) {
Debug.println("readdir: " + path);
        try {
            fileSystem.provider().newDirectoryStream(fileSystem.getPath(path), p -> true)
                .forEach(p -> filler.apply(buf, p.getFileName().toString(), null, 0));
            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int rename(String path, String newName) {
Debug.println("rename: " + path);
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
Debug.println("rmdir: " + path);
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
Debug.println("truncate: " + path);
        // TODO
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int unlink(String path) {
Debug.println("unlink: " + path);
        try {
            fileSystem.provider().delete(fileSystem.getPath(path));
            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
Debug.println("write: " + path + ", " + offset);
        try {
            ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
            long written = 0;
            SeekableByteChannel channel = fileHandles.get(fi.fh.get());
            do {
                long remaining = size - written;
                bb.clear();
                int len = (int) Math.min(remaining, bb.capacity());
                buf.get(written, bb.array(), 0, len);
                bb.limit(len);
                channel.write(bb); // TODO check return value
                written += len;
            } while (written < size);
            return (int) written;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int statfs(String path, Statvfs stbuf) {
Debug.println(Level.FINE, "statfs: " + path);
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
Debug.println("chmod: " + path);
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
Debug.println("release: " + path);
        try {
            Channel channel = fileHandles.get(info.fh.get());
            channel.close();
            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        } finally {
            fileHandles.remove(info.fh.get());
        }
    }

    @Override
    public int lock(String path, FuseFileInfo fi, int cmd, Flock flock) {
Debug.println("lock: " + path);
        return 0;
    }
}
