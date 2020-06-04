/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.javafs;

import vavi.net.fuse.Fuse;
import vavi.net.fuse.FuseProvider;


/**
 * JavaFSFuseProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/06/03 umjammer initial version <br>
 */
public class JavaFSFuseProvider implements FuseProvider {

    @Override
    public Fuse getFuse() {
        return new JavaFSFuse();
    }
}

/* */
