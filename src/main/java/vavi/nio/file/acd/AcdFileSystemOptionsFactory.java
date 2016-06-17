/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.acd;

import java.nio.file.LinkOption;

import com.github.fge.filesystem.options.FileSystemOptionsFactory;


/**
 * AcdFileSystemOptionsFactory. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
public class AcdFileSystemOptionsFactory extends FileSystemOptionsFactory {

    public AcdFileSystemOptionsFactory() {
        addLinkOption(LinkOption.NOFOLLOW_LINKS);
    }
}

/* */
