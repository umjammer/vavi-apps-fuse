/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import com.github.fge.filesystem.provider.FileSystemFactoryProvider;


/**
 * AcdFileSystemFactoryProvider. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public final class AcdFileSystemFactoryProvider extends FileSystemFactoryProvider {

    public AcdFileSystemFactoryProvider() {
        setAttributesFactory(new AcdFileAttributesFactory());
        setOptionsFactory(new AcdFileSystemOptionsFactory());
    }
}
