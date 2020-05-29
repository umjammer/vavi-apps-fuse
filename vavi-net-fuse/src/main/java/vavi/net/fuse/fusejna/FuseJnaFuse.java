/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.fusejna;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.Map;

import vavi.net.fuse.Fuse;

import net.fusejna.FuseException;
import net.fusejna.util.FuseFilesystemAdapterAssumeImplemented;


/**
 * JnaFuseFuse. (jna-fuse engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/29 umjammer initial version <br>
 */
public class FuseJnaFuse implements Fuse {

    /** */
    private FuseFilesystemAdapterAssumeImplemented fuse;

    @Override
    public void mount(FileSystem fs, String mountPoint, Map<String, String> env) throws IOException {
        try {
            fuse = new JavaFsFS(fs);
            fuse.mount(Paths.get(mountPoint).toFile());
        } catch (FuseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void unmount() throws IOException {
        try {
            fuse.unmount();
        } catch (FuseException e) {
            throw new IOException(e);
        }
    }
}

/* */
