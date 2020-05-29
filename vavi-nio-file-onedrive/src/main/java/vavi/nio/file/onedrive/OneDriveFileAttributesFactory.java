/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import com.github.fge.filesystem.attributes.FileAttributesFactory;

import de.tuberlin.onedrivesdk.common.OneItem;


/**
 * OneDriveFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
public final class OneDriveFileAttributesFactory extends FileAttributesFactory {

    public OneDriveFileAttributesFactory() {
        setMetadataClass(OneItem.class);
        addImplementation("basic", OneDriveBasicFileAttributesProvider.class);
    }
}
