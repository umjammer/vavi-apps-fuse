/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.fusejna;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import vavi.net.fuse.Fuse;
import vavi.util.Debug;

import co.paralleluniverse.fuse.TypeMode;
import net.fusejna.FuseException;
import net.fusejna.FuseFilesystem;


/**
 * JnaFuseFuse. (jna-fuse engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/29 umjammer initial version <br>
 */
public class FuseJnaFuse implements Fuse {

    /** */
    public static final String ENV_NO_APPLE_DOUBLE = JavaNioFileFS.ENV_NO_APPLE_DOUBLE;

    /** */
    private FuseFilesystem fuse;

    @Override
    public void mount(FileSystem fs, String mountPoint, Map<String, Object> env) throws IOException {
        try {
            if (env.containsKey(ENV_SINGLE_THREAD) && (Boolean) env.get(ENV_SINGLE_THREAD)) {
                fuse = new SingleThreadJavaNioFileFS(fs, env);
Debug.println("use single thread");
            } else {
                fuse = new JavaNioFileFS(fs, env);
            }
            fuse.mount(Paths.get(mountPoint).toFile(), false);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { close(); } catch (Exception e) { e.printStackTrace(); }}));
        } catch (FuseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (fuse != null) {
Debug.println("unmount...");
                fuse.unmount();
                fuse = null;
Debug.println("unmount done");
            }
        } catch (FuseException e) {
Debug.println(Level.WARNING, "unmount: " + e);
            throw new IOException(e);
        }
    }

    /** */
    static boolean[] permissionsToMode(Set<PosixFilePermission> permissions) {
        boolean[] mode = new boolean[9];
        for (PosixFilePermission px : permissions) {
            switch (px) {
            case OWNER_READ: mode[0] = true; break;
            case OWNER_WRITE: mode[1] = true; break;
            case OWNER_EXECUTE: mode[2] = true; break;
            case GROUP_READ: mode[3] = true; break;
            case GROUP_WRITE: mode[4] = true; break;
            case GROUP_EXECUTE: mode[5] = true; break;
            case OTHERS_READ: mode[6] = true; break;
            case OTHERS_WRITE: mode[7] = true; break;
            case OTHERS_EXECUTE: mode[8] = true; break;
            }
        }
        return mode;
    }

    /** */
    static Set<PosixFilePermission> modeToPermissions(long mode) {
        final EnumSet<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
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
