/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.archive;

import com.github.fge.filesystem.provider.FileSystemProviderBase;


/**
 * ArchiveFileSystemProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/06 umjammer initial version <br>
 */
public final class ArchiveFileSystemProvider extends FileSystemProviderBase {

    public ArchiveFileSystemProvider() {
        super(new ArchiveFileSystemRepository());
    }
}
