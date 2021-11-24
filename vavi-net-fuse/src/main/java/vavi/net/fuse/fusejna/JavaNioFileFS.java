/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.fusejna;

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

import vavi.nio.file.Util;
import vavi.util.Debug;

import jnr.constants.platform.Errno;
import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.StructStatvfs.StatvfsWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterAssumeImplemented;


/**
 * JavaFsFS. (fuse-jna)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
class JavaNioFileFS extends FuseFilesystemAdapterAssumeImplemented {

    /** */
    protected transient FileSystem fileSystem;

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
    public int access(final String path, final int access) {
Debug.println(Level.FINE, "access: " + path);
        try {
            // TODO access
            fileSystem.provider().checkAccess(fileSystem.getPath(path));
            return 0;
        } catch (NoSuchFileException e) {
            return -ErrorCodes.ENOENT();
        } catch (AccessDeniedException e) {
Debug.println(e);
            return -ErrorCodes.EACCES();
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EACCES();
        }
    }

    @Override
    public int create(final String path, final ModeWrapper mode, final FileInfoWrapper info) {
Debug.println(Level.FINE, "create: " + path);
        try {
            Set<OpenOption> options = new HashSet<>();
            options.add(StandardOpenOption.WRITE);
            options.add(StandardOpenOption.CREATE_NEW);
            SeekableByteChannel channel = fileSystem.provider().newByteChannel(fileSystem.getPath(path), options);
            long fh = fileHandle.incrementAndGet();
            fileHandles.put(fh, channel);
            info.fh(fh);

            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int getattr(final String path, final StatWrapper stat) {
Debug.println(Level.FINE, "getattr: " + path);
        try {
            BasicFileAttributes attributes =
                    fileSystem.provider().readAttributes(fileSystem.getPath(path), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

            if (attributes instanceof PosixFileAttributes) {
                boolean[] m = FuseJnaFuse.permissionsToMode(PosixFileAttributes.class.cast(attributes).permissions());
                if (attributes.isDirectory()) {
                    stat.setMode(NodeType.DIRECTORY, m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8])
                        .setAllTimesSec(attributes.lastModifiedTime().to(TimeUnit.SECONDS));
                } else {
                    stat.setMode(NodeType.FILE, m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8])
                        .setAllTimesSec(attributes.lastModifiedTime().to(TimeUnit.SECONDS))
                        .size(attributes.size());
                }
            } else {
                if (attributes.isDirectory()) {
                    stat.setMode(NodeType.DIRECTORY, true, true, true, true, false, true, true, false, true)
                        .setAllTimesSec(attributes.lastModifiedTime().to(TimeUnit.SECONDS));
                } else {
                    stat.setMode(NodeType.FILE, true, true, false, true, false, false, true, false, false)
                        .setAllTimesSec(attributes.lastModifiedTime().to(TimeUnit.SECONDS))
                        .size(attributes.size());
                }
            }
            return 0;
        } catch (NoSuchFileException e) {
            if (e.getMessage().startsWith("ignore apple double file:")) {
Debug.println(Level.FINE, e.getMessage());
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
    public int fgetattr(final String path, final StatWrapper stat, final FileInfoWrapper info)
    {
Debug.println(Level.FINE, "fgetattr: " + path);
        return getattr(path, stat);
    }

    @Override
    public int mkdir(final String path, final ModeWrapper mode) {
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
    public int open(final String path, final FileInfoWrapper info) {
Debug.println(Level.FINE, "open: " + path);
        try {
            Set<OpenOption> options = new HashSet<>();
            options.add(StandardOpenOption.READ);
            SeekableByteChannel channel = fileSystem.provider().newByteChannel(fileSystem.getPath(path), options);
            long fh = fileHandle.incrementAndGet();
            fileHandles.put(fh, channel);
            info.fh(fh);

            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    /** why not defined? */
    private static final int O_NONBLOCK = 04000;

    @Override
    public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final FileInfoWrapper info) {
Debug.println(Level.FINE, "read: " + path + ", " + offset + ", " + size + ", " + info.fh());
        try {
            if (fileHandles.containsKey(info.fh())) {
                SeekableByteChannel channel = fileHandles.get(info.fh());
                if (info.nonseekable()) {
                    assert offset == channel.position();
                } else {
                    channel.position(offset);
                }
                int n = channel.read(buffer);
                if (n > 0) {
                    if ((info.flags() & O_NONBLOCK) != 0) {
                        assert n <= 0 || n == size;
                    } else {
                        int c;
                        while (n < size) {
                            if ((c = channel.read(buffer)) <= 0)
                                break;
                            n += c;
                        }
                    }
Debug.println(Level.FINE, "read: " + n);
                    return n;
                } else {
Debug.println(Level.FINE, "read: 0");
                    return 0; // we did not read any bytes
                }
            } else {
                return -ErrorCodes.EEXIST();
            }
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int readdir(final String path, final DirectoryFiller filler) {
Debug.println(Level.FINE, "readdir: " + path);
        try {
            fileSystem.provider().newDirectoryStream(fileSystem.getPath(path), p -> true)
                .forEach(p -> {
                    try {
                        filler.add(Util.toFilenameString(p));
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
    public int rename(final String path, final String newName) {
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
    public int rmdir(final String path) {
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
    public int truncate(final String path, final long offset) {
Debug.println(Level.FINE, "truncate: " + path);
        // TODO
        return -ErrorCodes.ENOSYS();
    }

    @Override
    public int unlink(final String path) {
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
    public int write(final String path,
                     final ByteBuffer buf,
                     final long size,
                     final long offset,
                     final FileInfoWrapper info) {
Debug.println(Level.FINE, "write: " + path + ", " + offset + ", " + size + ", " + info.fh());
        try {
            if (fileHandles.containsKey(info.fh())) {
                SeekableByteChannel channel = fileHandles.get(info.fh());
                if (!info.append() && !info.nonseekable()) {
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
                }
                int n = channel.write(buf);
                if (n > 0) {
                    if ((info.flags() & O_NONBLOCK) != 0) {
                        assert n <= 0 || n == size;
                    } else {
                        int c;
                        while (n < size) {
                            if ((c = channel.write(buf)) <= 0) {
                                break;
                            }
                            n += c;
                        }
                    }
                }
                return n;
            } else {
                return -ErrorCodes.EEXIST();
            }
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int statfs(final String path, final StatvfsWrapper stat) {
Debug.println(Level.FINE, "statfs: " + path);
        try {
            FileStore fileStore = fileSystem.getFileStores().iterator().next();
//Debug.println("total: " + fileStore.getTotalSpace());
//Debug.println("free: " + fileStore.getUsableSpace());

            long blockSize = 512;

            long total = fileStore.getTotalSpace() / blockSize;
            long free = fileStore.getUsableSpace() / blockSize;
            long used = total - free;

            stat.bavail(used);
            stat.bfree(free);
            stat.blocks(total);
            stat.bsize(blockSize);
            stat.favail(-1);
            stat.ffree(-1);
            stat.files(-1);
            stat.frsize(1);

            return 0;
        } catch (IOException e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int release(final String path, final FileInfoWrapper info) {
Debug.println(Level.FINE, "release: " + path);
        try {
            if (fileHandles.containsKey(info.fh())) {
                Channel channel = fileHandles.get(info.fh());
                channel.close();
                return 0;
            } else {
                return -ErrorCodes.EEXIST();
            }
        } catch (IOException e) {
Debug.println(e);
            return -ErrorCodes.EIO();
        } finally {
            fileHandles.remove(info.fh());
        }
    }

    @Override
    public int chmod(String path, ModeWrapper mode) {
Debug.println(Level.FINE, "chmod: " + path);
        try {
            if (fileSystem.provider().getFileStore(fileSystem.getPath(path)).supportsFileAttributeView(PosixFileAttributeView.class)) {
                PosixFileAttributeView attrs = fileSystem.provider().getFileAttributeView(fileSystem.getPath(path), PosixFileAttributeView.class);
                attrs.setPermissions(FuseJnaFuse.modeToPermissions(mode.mode()));
                return 0;
            } else {
                return -Errno.EAFNOSUPPORT.ordinal();
            }
        } catch (Exception e) {
Debug.printStackTrace(e);
            return -ErrorCodes.EIO();
        }
    }
}
