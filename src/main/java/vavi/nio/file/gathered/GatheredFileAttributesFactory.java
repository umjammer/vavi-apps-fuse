/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.gathered;

import java.nio.file.FileSystem;

import com.github.fge.filesystem.attributes.FileAttributesFactory;


/**
 * GatheredFsFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/06 umjammer initial version <br>
 */
public final class GatheredFileAttributesFactory extends FileAttributesFactory {

    public GatheredFileAttributesFactory() {
        setMetadataClass(FileSystem.class);
        addImplementation("basic", GatheredBasicFileAttributesProvider.class);
    }
}
