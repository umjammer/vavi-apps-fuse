/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import org.nuxeo.onedrive.client.types.DriveItem;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase.ExtendedFileAttributesFactory;


/**
 * OneDriveFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
public final class OneDriveFileAttributesFactory extends ExtendedFileAttributesFactory {

    public OneDriveFileAttributesFactory() {
        setMetadataClass(DriveItem.Metadata.class);
        addImplementation("basic", OneDriveBasicFileAttributesProvider.class);
    }
}
