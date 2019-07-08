/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import com.github.fge.filesystem.provider.FileSystemProviderBase;


/**
 * VfsFileSystemProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/06 umjammer initial version <br>
 */
public final class VfsFileSystemProvider extends FileSystemProviderBase {

    public VfsFileSystemProvider() {
        super(new VfsFileSystemRepository());
    }
}
