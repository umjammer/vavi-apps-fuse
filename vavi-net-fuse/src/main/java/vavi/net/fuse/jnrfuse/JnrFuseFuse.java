/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.jnrfuse;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.Map;

import vavi.net.fuse.Fuse;

import ru.serce.jnrfuse.FuseStubFS;


/**
 * JnrFuseFuse. (jnr-fuse engine)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/29 umjammer initial version <br>
 */
public class JnrFuseFuse implements Fuse {

    /** */
    private FuseStubFS fuse;

    @Override
    public void mount(FileSystem fs, String mountPoint, Map<String, String> env) throws IOException {
        fuse = new JavaFsFS(fs);
        fuse.mount(Paths.get(mountPoint));
    }

    @Override
    public void unmount() throws IOException {
        fuse.umount();
    }
}

/* */
