/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import com.github.fge.filesystem.provider.FileSystemFactoryProvider;


/**
 * GoogleDriveFileSystemFactoryProvider. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public final class GoogleDriveFileSystemFactoryProvider extends FileSystemFactoryProvider {

    public GoogleDriveFileSystemFactoryProvider() {
        setAttributesFactory(new GoogleDriveFileAttributesFactory());
        setOptionsFactory(new GoogleDriveFileSystemOptionsFactory());
    }
}
