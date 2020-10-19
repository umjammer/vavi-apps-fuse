/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.fuse.fusejna;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.fusejna.DirectoryFiller;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;


/**
 * OneThreadJavaNioFileFS. (fuse-jna)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
class SingleThreadJavaNioFileFS extends JavaNioFileFS {

    /** */
    private ExecutorService singleService = Executors.newSingleThreadExecutor();

    /** */
    private ExecutorService multiService = Executors.newCachedThreadPool();

    /**
     * @param fileSystem
     */
    public SingleThreadJavaNioFileFS(FileSystem fileSystem, Map<String, Object> env) throws IOException {
        super(fileSystem, env);
    }

    @Override
    public int access(String path, int access) {
        Future<Integer> f = multiService.submit(() -> {
            return super.access(path, access);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int getattr(final String path, final StatWrapper stat) {
        Future<Integer> f = multiService.submit(() -> {
            return super.getattr(path, stat);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int create(final String path, final ModeWrapper mode, final FileInfoWrapper info) {
        Future<Integer> f = singleService.submit(() -> {
            return super.create(path, mode, info);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int open(final String path, final FileInfoWrapper info) {
        Future<Integer> f = singleService.submit(() -> {
            return super.open(path, info);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int read(final String path, final ByteBuffer buf, final long size, final long offset, final FileInfoWrapper info) {
        Future<Integer> f = singleService.submit(() -> {
            return super.read(path, buf, size, offset, info);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int write(final String path,
                     final ByteBuffer buf,
                     final long size,
                     final long offset,
                     final FileInfoWrapper info) {
        Future<Integer> f = singleService.submit(() -> {
            return super.write(path, buf, size, offset, info);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int release(final String path, final FileInfoWrapper info) {
        Future<Integer> f = singleService.submit(() -> {
            return super.release(path, info);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int chmod(String path, ModeWrapper mode) {
        Future<Integer> f = singleService.submit(() -> {
            return super.chmod(path, mode);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int mkdir(final String path, final ModeWrapper mode) {
        Future<Integer> f = singleService.submit(() -> {
            return super.mkdir(path, mode);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int rmdir(final String path) {
        Future<Integer> f = singleService.submit(() -> {
            return super.rmdir(path);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int readdir(final String path, final DirectoryFiller filler) {
        Future<Integer> f = singleService.submit(() -> {
            return super.readdir(path, filler);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int rename(final String path, final String newName) {
        Future<Integer> f = singleService.submit(() -> {
            return super.rename(path, newName);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int truncate(final String path, final long offset) {
        Future<Integer> f = singleService.submit(() -> {
            return super.truncate(path, offset);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int unlink(final String path) {
        Future<Integer> f = singleService.submit(() -> {
            return super.unlink(path);
        });
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }
}
