/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.hfs;

import org.catacombae.storage.fs.FSEntry;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase.ExtendsdFileAttributesFactory;


/**
 * HfsFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/06 umjammer initial version <br>
 */
public final class HfsFileAttributesFactory extends ExtendsdFileAttributesFactory {

    public HfsFileAttributesFactory() {
        setMetadataClass(FSEntry.class);
        addImplementation("basic", HfsBasicFileAttributesProvider.class);
    }
}
