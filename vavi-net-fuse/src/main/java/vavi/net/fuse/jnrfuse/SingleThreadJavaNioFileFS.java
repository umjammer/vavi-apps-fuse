/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.jnrfuse;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jnr.ffi.Pointer;
import jnr.ffi.types.off_t;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;


/**
 * SingleThreadJavaNioFileFS. (jnr-fuse)
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
    public int getattr(final String path, final FileStat stat) {
        Future<Integer> f = multiService.submit(() -> super.getattr(path, stat));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int create(final String path, final long mode, final FuseFileInfo info) {
        Future<Integer> f = singleService.submit(() -> super.create(path, mode, info));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int open(final String path, final FuseFileInfo info) {
        Future<Integer> f = singleService.submit(() -> super.open(path, info));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int read(final String path, final Pointer buf, final long size, final long offset, final FuseFileInfo info) {
        Future<Integer> f = singleService.submit(() -> super.read(path, buf, size, offset, info));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int write(final String path,
                     final Pointer buf,
                     final long size,
                     final long offset,
                     final FuseFileInfo info) {
        Future<Integer> f = singleService.submit(() -> super.write(path, buf, size, offset, info));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int release(final String path, final FuseFileInfo info) {
        Future<Integer> f = singleService.submit(() -> super.release(path, info));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int chmod(String path, long mode) {
        Future<Integer> f = singleService.submit(() -> super.chmod(path, mode));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int mkdir(final String path, final long mode) {
        Future<Integer> f = singleService.submit(() -> super.mkdir(path, mode));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int rmdir(final String path) {
        Future<Integer> f = singleService.submit(() -> super.rmdir(path));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int readdir(final String path, Pointer buf, final FuseFillDir filler, @off_t long offset, FuseFileInfo info) {
        Future<Integer> f = singleService.submit(() -> super.readdir(path, buf, filler, offset, info));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int rename(final String path, final String newName) {
        Future<Integer> f = singleService.submit(() -> super.rename(path, newName));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int truncate(final String path, final long offset) {
        Future<Integer> f = singleService.submit(() -> super.truncate(path, offset));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int unlink(final String path) {
        Future<Integer> f = singleService.submit(() -> super.unlink(path));
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }
}
