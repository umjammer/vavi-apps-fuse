/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import com.github.fge.filesystem.attributes.FileAttributesFactory;
import com.microsoft.graph.models.DriveItem;


/**
 * OneDriveFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
public final class OneDriveFileAttributesFactory extends FileAttributesFactory {

    static class Metadata {
        OneDriveFileSystemDriver driver;
        DriveItem driveItem;
        Metadata(OneDriveFileSystemDriver driver, DriveItem driveItem) {
            this.driver = driver;
            this.driveItem = driveItem;
        }
    }

    public OneDriveFileAttributesFactory() {
        setMetadataClass(Metadata.class);
        addImplementation("basic", OneDriveBasicFileAttributesProvider.class);
        addImplementation("user", OneDriveUserDefinedFileAttributesProvider.class);
    }
}
