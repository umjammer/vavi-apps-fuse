/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.gathered;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase.ExtendsdFileAttributesFactory;


/**
 * GatheredFsFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/06 umjammer initial version <br>
 */
public final class GatheredFileAttributesFactory extends ExtendsdFileAttributesFactory {

    public GatheredFileAttributesFactory() {
        setMetadataClass(Object.class);
        addImplementation("basic", GatheredBasicFileAttributesProvider.class);
    }
}
