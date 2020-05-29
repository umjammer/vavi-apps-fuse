/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import com.github.fge.filesystem.provider.FileSystemProviderBase;


/**
 * AcdFileSystemProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public final class AcdFileSystemProvider extends FileSystemProviderBase {

    public static final String ENV_ID = "email";

    public AcdFileSystemProvider() {
        super(new AcdFileSystemRepository());
    }
}
