/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.fuse.fusejna;

import vavi.nio.fuse.Fuse;
import vavi.nio.fuse.FuseProvider;


/**
 * FuseJnaFuseProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/06/03 umjammer initial version <br>
 */
public class FuseJnaFuseProvider implements FuseProvider {

    @Override
    public Fuse getFuse() {
        return new FuseJnaFuse();
    }
}

/* */
