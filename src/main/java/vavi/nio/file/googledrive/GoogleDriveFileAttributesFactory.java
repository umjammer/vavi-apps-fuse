/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.google.api.services.drive.model.File;


/**
 * GoogleDriveFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public final class GoogleDriveFileAttributesFactory extends FileAttributesFactory {

    public GoogleDriveFileAttributesFactory() {
        setMetadataClass(File.class);
        addImplementation("basic", GoogleDriveBasicFileAttributesProvider.class);
    }
}
