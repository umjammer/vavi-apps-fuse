/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import com.github.fge.filesystem.driver.ExtendedFileSystemDriverBase.ExtendsdFileAttributesFactory;
import com.google.api.services.drive.model.File;


/**
 * GoogleDriveFileAttributesFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public final class GoogleDriveFileAttributesFactory extends ExtendsdFileAttributesFactory {

    static class Metadata {
        GoogleDriveFileSystemDriver driver;
        File file;
        Metadata(GoogleDriveFileSystemDriver driver, File file) {
            this.driver = driver;
            this.file = file;
        }
    }

    public GoogleDriveFileAttributesFactory() {
        setMetadataClass(Metadata.class);
        addImplementation("basic", GoogleDriveBasicFileAttributesProvider.class);
        addImplementation("user", GoogleDriveUserDefinedFileAttributesProvider.class);
    }
}
