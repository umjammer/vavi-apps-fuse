/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.nio.file.CopyOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;


/**
 * OneDriveUploadOption.
 * <p>
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
        return Long.hashCode(-3760090552182064957L); // TODO ad-hoc
    }
}

/* */
