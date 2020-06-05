/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.nio.file.CopyOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;


/**
 * OneDriveUploadOption.
 * <p>
 * for large file.
 * </p>
 * TODO CopyOption doesn't work.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/31 umjammer initial version <br>
 */
public class OneDriveUploadOption implements OpenOption, CopyOption {

    /** */
    private Path source;

    /** */
    public OneDriveUploadOption(Path source) {
        this.source = source;
    }

    /** */
    public Path getSource() {
        return source;
    }

    @Override
    public boolean equals(Object other) {
        return other != null && OneDriveUploadOption.class.isInstance(other); // TODO ad-hoc
    }

    @Override
    public int hashCode() {
        return Long.hashCode(5575546140441990410L); // TODO ad-hoc
    }
}

/* */
