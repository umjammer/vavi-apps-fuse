/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import org.apache.commons.vfs2.FileObject;

import com.github.fge.filesystem.attributes.FileAttributesFactory;


/**
 * VfsFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/06 umjammer initial version <br>
 */
public final class VfsFileAttributesFactory extends FileAttributesFactory {

    public VfsFileAttributesFactory() {
        setMetadataClass(FileObject.class);
        addImplementation("basic", VfsBasicFileAttributesProvider.class);
    }
}
