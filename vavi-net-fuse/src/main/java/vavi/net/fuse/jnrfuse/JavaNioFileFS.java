/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.jnrfuse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.TimeUnit;

import vavi.util.Debug;

import jnr.ffi.Pointer;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import ru.serce.jnrfuse.struct.Statvfs;


/**
 * JavaFsFS. (jnr-fuse)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
class JavaFsFS extends FuseStubFS {

    private static final int BUFFER_SIZE = 4096;

    /** */
    private transient FileSystem fileSystem;

    /**
     * @param fileSystem
     */
    public JavaFsFS(FileSystem fileSystem) throws IOException {
        this.fileSystem = fileSystem;
    }

    @Override
    public int access(String path, int access) {
Debug.println("path: " + path);
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
Debug.println("path: " + path);
        if (Files.exists(fileSystem.getPath(path))) {
            return -ErrorCodes.EEXIST();
        }
        return 0;
    }

    @Override
    public int getattr(String path, FileStat stat) {
Debug.println("path: " + path);
        try {
            if (Files.isDirectory(fileSystem.getPath(path))) {
                stat.st_mode.set(FileStat.S_IFDIR | 0555);
                stat.st_atim.tv_sec.set(Files.getLastModifiedTime(fileSystem.getPath(path)).to(TimeUnit.SECONDS));
            } else {
                stat.st_mode.set(FileStat.S_IFDIR | 0555);
                stat.st_atim.tv_sec.set(Files.getLastModifiedTime(fileSystem.getPath(path)).to(TimeUnit.SECONDS));
                stat.st_size.set(Files.size(fileSystem.getPath(path)));
            }
            return 0;
        } catch (NoSuchFileException e) {
Debug.println(e);
            return -ErrorCodes.ENOENT();
        } catch (IOException e) {
Debug.println(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int fgetattr(String path, FileStat stat, FuseFileInfo info)
    {
Debug.println("path: " + path);
        return 0;
    }

    @Override
    public int mkdir(String path, @mode_t long mode) {
Debug.println("path: " + path);
        try {
            Files.createDirectory(fileSystem.getPath(path));
            return 0;
        } catch (IOException e) {
Debug.println(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int open(String path, FuseFileInfo info) {
Debug.println("path: " + path);
        if (Files.exists(fileSystem.getPath(path))) {
            return 0;
        } else {
            return -ErrorCodes.ENOENT();
        }
    }

    @Override
    public int read(String path, Pointer buf, long num, long offset, FuseFileInfo info) {
Debug.println("path: " + path + ", " + offset);
        try {
            SeekableByteChannel channel = Files.newByteChannel(fileSystem.getPath(path));
            long size = channel.size();
            if (offset >= size) {
                return 0;
            } else {
                ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
                long pos = 0;
                channel.position(offset);
Debug.printf("Attempting to read {}-{}:", offset, offset + num);
                do {
                    bb.clear();
                    bb.limit((int) Math.min(bb.capacity(), num));
                    int read = channel.read(bb);
                    if (read == -1) {
Debug.println("Reached EOF");
                        return (int) pos; // reached EOF TODO: wtf cast
                    } else {
Debug.printf("Reading {}-{}", offset + pos, offset + pos + read);
                        buf.put(pos, bb.array(), 0, read);
                        pos += read;
                    }
                } while (pos < num);
            }
            return 0;
        } catch (IOException e) {
Debug.println(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filler, @off_t long offset, FuseFileInfo fi) {
        try {
            Files.list(fileSystem.getPath(path)).forEach(p -> filler.apply(buf, p.getFileName().toString(), null, 0));
            return 0;
        } catch (IOException e) {
Debug.println(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int rename(String path, String newName) {
Debug.println("path: " + path);
        try {
            Files.move(fileSystem.getPath(path), fileSystem.getPath(newName));
            return 0;
        } catch (IOException e) {
Debug.println(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int rmdir(String path) {
Debug.println("path: " + path);
        try {
            Files.delete(fileSystem.getPath(path));
            return 0;
        } catch (IOException e) {
Debug.println(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int truncate(String path, long offset) {
Debug.println("path: " + path);
        // TODO
        return 0;
    }

    @Override
    public int unlink(String path) {
Debug.println("path: " + path);
        try {
            Files.delete(fileSystem.getPath(path));
            return 0;
        } catch (IOException e) {
Debug.println(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
Debug.println("path: " + path + ", " + offset);
        try {
            ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
            long written = 0;
            SeekableByteChannel channel = Files.newByteChannel(fileSystem.getPath(path));
            channel.position(offset);
            do {
                long remaining = size - written;
                bb.clear();
                int len = (int) Math.min(remaining, bb.capacity());
                buf.get(written, bb.array(), 0, len);
                bb.limit(len);
                channel.write(bb); // TODO check return value
                written += len;
            } while (written < size);
            return 0;
        } catch (IOException e) {
Debug.println(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int statfs(String path, Statvfs stbuf) {
Debug.println("path: " + path);
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
Debug.println(e);
            return -ErrorCodes.EIO();
        }
    }
}
