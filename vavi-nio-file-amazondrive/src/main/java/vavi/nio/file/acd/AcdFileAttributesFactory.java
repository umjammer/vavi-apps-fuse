/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import org.yetiz.lib.acd.Entity.NodeInfo;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase.ExtendsdFileAttributesFactory;


/**
 * AcdFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public final class AcdFileAttributesFactory extends ExtendsdFileAttributesFactory {

    public AcdFileAttributesFactory() {
        setMetadataClass(NodeInfo.class);
        addImplementation("basic", AcdBasicFileAttributesProvider.class);
    }
}
