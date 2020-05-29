/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.flickr;

import com.github.fge.filesystem.provider.FileSystemFactoryProvider;


/**
 * FlickrFileSystemFactoryProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public final class FlickrFileSystemFactoryProvider extends FileSystemFactoryProvider {

    public FlickrFileSystemFactoryProvider() {
        setAttributesFactory(new FlickrFileAttributesFactory());
        setOptionsFactory(new FlickrFileSystemOptionsFactory());
    }
}
