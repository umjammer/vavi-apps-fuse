/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.gathered;

import com.github.fge.filesystem.provider.FileSystemProviderBase;


/**
 * GatheredFsFileSystemProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/06 umjammer initial version <br>
 */
public final class GatheredFileSystemProvider extends FileSystemProviderBase {

    /** The key for the parameter 'env'. value class is {@link java.util.Map}<String, FileSystem> */
    public static final String ENV_FILESYSTEMS = "fileSystems";

    /** The key for the parameter 'env'. value class is {@link NameMap} */
    public static final String ENV_NAME_MAP = "nameMap";

    public GatheredFileSystemProvider() {
        super(new GatheredFileSystemRepository());
    }
}
