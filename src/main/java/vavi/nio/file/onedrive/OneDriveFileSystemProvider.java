/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import com.github.fge.filesystem.provider.FileSystemProviderBase;


/**
 * OneDriveFileSystemProvider.
 * <p>
 * set "authenticatorClassName" in "classpath:onedrive.properties"
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
public final class OneDriveFileSystemProvider extends FileSystemProviderBase {

    public static final String PARAM_ID = "id";

    public OneDriveFileSystemProvider() {
        super(new OneDriveFileSystemRepository());
    }
}
