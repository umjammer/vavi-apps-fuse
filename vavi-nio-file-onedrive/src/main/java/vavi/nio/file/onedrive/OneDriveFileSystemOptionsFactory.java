/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.nio.file.LinkOption;

import com.github.fge.filesystem.options.FileSystemOptionsFactory;


/**
 * OneDriveFileSystemOptionsFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
public class OneDriveFileSystemOptionsFactory extends FileSystemOptionsFactory {

    public OneDriveFileSystemOptionsFactory() {
        addLinkOption(LinkOption.NOFOLLOW_LINKS);
        addCopyOption(new OneDriveUploadOption(null));
        addWriteOpenOption(new OneDriveUploadOption(null));
    }
}

/* */
