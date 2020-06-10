/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.jnrfuse;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import vavi.net.fuse.Fuse;
import vavi.util.Debug;

import co.paralleluniverse.fuse.TypeMode;
import ru.serce.jnrfuse.FuseStubFS;


/**
 * JnrFuseFuse. (jnr-fuse engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/29 umjammer initial version <br>
 */
public class JnrFuseFuse implements Fuse {

    /** */
    public static final String ENV_NO_APPLE_DOUBLE = JavaNioFileFS.ENV_NO_APPLE_DOUBLE;

    /** */
    private FuseStubFS fuse;

    @Override
    public void mount(FileSystem fs, String mountPoint, Map<String, Object> env) throws IOException {
        fuse = new JavaNioFileFS(fs, env);
        fuse.mount(Paths.get(mountPoint));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { close(); } catch (IOException e) { throw new IllegalStateException(e); }}));
    }

    @Override
    public void close() throws IOException {
        if (fuse != null) {
Debug.println("unmount");
            fuse.umount();
            fuse = null;
        }
    }

    /** */
    static long permissionsToMode(Set<PosixFilePermission> permissions) {
        long mode = 0;
        for (PosixFilePermission px : permissions) {
            switch (px) {
            case OWNER_READ: mode |= TypeMode.S_IRUSR; break;
            case OWNER_WRITE: mode |= TypeMode.S_IWUSR; break;
            case OWNER_EXECUTE: mode |= TypeMode.S_IXUSR; break;
            case GROUP_READ: mode |= TypeMode.S_IRGRP; break;
            case GROUP_WRITE: mode |= TypeMode.S_IWGRP; break;
            case GROUP_EXECUTE: mode |= TypeMode.S_IXGRP; break;
            case OTHERS_READ: mode |= TypeMode.S_IROTH; break;
            case OTHERS_WRITE: mode |= TypeMode.S_IWOTH; break;
            case OTHERS_EXECUTE: mode |= TypeMode.S_IXOTH; break;
            }
        }
        return mode;
    }

    /** */
    static Set<PosixFilePermission> modeToPermissions(long mode) {
        EnumSet<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
        if ((mode & TypeMode.S_IRUSR) != 0)
            permissions.add(PosixFilePermission.OWNER_READ);
        if ((mode & TypeMode.S_IWUSR) != 0)
            permissions.add(PosixFilePermission.OWNER_WRITE);
        if ((mode & TypeMode.S_IXUSR) != 0)
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        if ((mode & TypeMode.S_IRGRP) != 0)
            permissions.add(PosixFilePermission.GROUP_READ);
        if ((mode & TypeMode.S_IWGRP) != 0)
            permissions.add(PosixFilePermission.GROUP_WRITE);
        if ((mode & TypeMode.S_IXGRP) != 0)
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
        if ((mode & TypeMode.S_IROTH) != 0)
            permissions.add(PosixFilePermission.OTHERS_READ);
        if ((mode & TypeMode.S_IWOTH) != 0)
            permissions.add(PosixFilePermission.OTHERS_WRITE);
        if ((mode & TypeMode.S_IXOTH) != 0)
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        return permissions;
    }
}

/* */
