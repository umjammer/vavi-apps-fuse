/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.flickr;

import com.flickr4java.flickr.photos.Photo;
import com.github.fge.filesystem.attributes.FileAttributesFactory;


/**
 * FlickrFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public final class FlickrFileAttributesFactory extends FileAttributesFactory {

    public FlickrFileAttributesFactory() {
        setMetadataClass(Photo.class);
        addImplementation("basic", FlickrBasicFileAttributesProvider.class);
    }
}
