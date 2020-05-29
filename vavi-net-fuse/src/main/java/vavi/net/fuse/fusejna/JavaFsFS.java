/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.fusejna;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.concurrent.TimeUnit;

import vavi.util.Debug;

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
class JavaFsFS extends FuseFilesystemAdapterAssumeImplemented {

    /** */
    private transient FileSystem fileSystem;

    /**
     * @param fileSystem
     */
    public JavaFsFS(FileSystem fileSystem) throws IOException {
        this.fileSystem = fileSystem;
    }

    @Override
    public int access(final String path, final int access) {
//Debug.println("path: " + path);
        try {
            // TODO access
            fileSystem.provider().checkAccess(fileSystem.getPath(path));
            return 0;
        } catch (NoSuchFileException e) {
            return -ErrorCodes.ENOENT();
        } catch (AccessDeniedException e) {
            return -ErrorCodes.EACCES();
        } catch (IOException e) {
            return -ErrorCodes.EACCES();
        }
    }

    @Override
    public int create(final String path, final ModeWrapper mode, final FileInfoWrapper info) {
Debug.println("path: " + path);
        if (Files.exists(fileSystem.getPath(path))) {
            return -ErrorCodes.EEXIST();
        }
        return 0;
    }

    @Override
    public int getattr(final String path, final StatWrapper stat) {
Debug.println("path: " + path);
        try {
            if (Files.isDirectory(fileSystem.getPath(path))) {
                stat.setMode(NodeType.DIRECTORY, true, true, true, true, false, true, true, false, true)
                    .setAllTimesSec(Files.getLastModifiedTime(fileSystem.getPath(path)).to(TimeUnit.SECONDS));
            } else {
                stat.setMode(NodeType.FILE, true, true, false, true, false, false, true, false, false)
                    .setAllTimesSec(Files.getLastModifiedTime(fileSystem.getPath(path)).to(TimeUnit.SECONDS))
                    .size(Files.size(fileSystem.getPath(path)));
            }
            return 0;
        } catch (NoSuchFileException e) {
            return -ErrorCodes.ENOENT();
        } catch (IOException e) {
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int fgetattr(final String path, final StatWrapper stat, final FileInfoWrapper info)
    {
Debug.println("path: " + path);
        return 0;
    }

    @Override
    public int mkdir(final String path, final ModeWrapper mode) {
Debug.println("path: " + path);
        try {
            Files.createDirectory(fileSystem.getPath(path));
            return 0;
        } catch (IOException e) {
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int open(final String path, final FileInfoWrapper info) {
Debug.println("path: " + path);
        if (Files.exists(fileSystem.getPath(path))) {
            return 0;
        } else {
            return -ErrorCodes.ENOENT();
        }
    }

    @Override
    public int read(final String path, final ByteBuffer buffer, final long size, final long offset, final FileInfoWrapper info) {
Debug.println("path: " + path + ", " + offset);
        try {
            Files.newByteChannel(fileSystem.getPath(path)).read(buffer);
            return 0;
        } catch (IOException e) {
e.printStackTrace();
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int readdir(final String path, final DirectoryFiller filler) {
        try {
            Files.list(fileSystem.getPath(path)).forEach(p -> filler.add(p.getFileName().toString()));
            return 0;
        } catch (IOException e) {
e.printStackTrace();
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int rename(final String path, final String newName) {
Debug.println("path: " + path);
        try {
            Files.move(fileSystem.getPath(path), fileSystem.getPath(newName));
            return 0;
        } catch (IOException e) {
e.printStackTrace();
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int rmdir(final String path) {
Debug.println("path: " + path);
        try {
            Files.delete(fileSystem.getPath(path));
            return 0;
        } catch (IOException e) {
e.printStackTrace();
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int truncate(final String path, final long offset) {
Debug.println("path: " + path);
        // TODO
        return 0;
    }

    @Override
    public int unlink(final String path) {
Debug.println("path: " + path);
        try {
            Files.delete(fileSystem.getPath(path));
            return 0;
        } catch (IOException e) {
e.printStackTrace();
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int write(final String path,
                     final ByteBuffer buf,
                     final long bufSize,
                     final long writeOffset,
                     final FileInfoWrapper wrapper) {
Debug.println("path: " + path + ", " + writeOffset);
        try {
            Files.newByteChannel(fileSystem.getPath(path)).write(buf);
            return 0;
        } catch (IOException e) {
e.printStackTrace();
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int statfs(final String path, final StatvfsWrapper wrapper) {
Debug.println("path: " + path);
        try {
            FileStore fileStore = fileSystem.getFileStores().iterator().next();
//Debug.println("total: " + fileStore.getTotalSpace());
//Debug.println("free: " + fileStore.getUsableSpace());

            long blockSize = 512;

            long total = fileStore.getTotalSpace() / blockSize;
            long free = fileStore.getUsableSpace() / blockSize;
            long used = total - free;

            wrapper.bavail(used);
            wrapper.bfree(free);
            wrapper.blocks(total);
            wrapper.bsize(blockSize);
            wrapper.favail(-1);
            wrapper.ffree(-1);
            wrapper.files(-1);
            wrapper.frsize(1);

            return 0;
        } catch (IOException e) {
            return -ErrorCodes.EIO();
        }
    }
}
