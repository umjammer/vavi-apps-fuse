/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.javafs;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.Map;

import vavi.net.fuse.Fuse;

import co.paralleluniverse.javafs.JavaFS;


/**
 * JavaFSFuse. (javafs engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/29 umjammer initial version <br>
 */
public class JavaFSFuse implements Fuse {

    private String mountPoint;

    @Override
    public void mount(FileSystem fs, String mountPoint, Map<String, String> env) throws IOException {
        this.mountPoint = mountPoint;
        JavaFS.mount(fs, Paths.get(mountPoint), false, true, env);
    }

    @Override
    public void unmount() throws IOException {
        JavaFS.unmount(Paths.get(mountPoint));
    }
}

/* */
