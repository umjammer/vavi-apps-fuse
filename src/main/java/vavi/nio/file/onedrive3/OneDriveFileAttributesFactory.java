/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import org.nuxeo.onedrive.client.OneDriveItem;

import com.github.fge.filesystem.attributes.FileAttributesFactory;


/**
 * OneDriveFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
public final class OneDriveFileAttributesFactory extends FileAttributesFactory {

    public OneDriveFileAttributesFactory() {
        setMetadataClass(OneDriveItem.class);
        addImplementation("basic", OneDriveBasicFileAttributesProvider.class);
    }
}
