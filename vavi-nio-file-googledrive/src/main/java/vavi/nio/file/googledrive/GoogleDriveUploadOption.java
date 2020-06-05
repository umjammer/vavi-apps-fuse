/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.nio.file.CopyOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;


/**
 * GoogleDriveUploadCopyOption.
 * <p>
 * for large file.
 * </p>
 * TODO CopyOption doesn't work.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/31 umjammer initial version <br>
 */
public class GoogleDriveUploadOption implements OpenOption, CopyOption {

    private Path source;

    /** */
    public GoogleDriveUploadOption(Path source) {
        this.source = source;
    }

    /** */
    public Path getSource() {
        return source;
    }

    @Override
    public boolean equals(Object other) {
        return other != null && GoogleDriveUploadOption.class.isInstance(other); // TODO ad-hoc
    }

    @Override
    public int hashCode() {
        return Long.hashCode(2245825155403778802L); // TODO ad-hoc
    }
}

/* */
