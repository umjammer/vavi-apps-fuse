/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.nio.file.CopyOption;


/**
 * GoogleDriveCopyOption.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/01 umjammer initial version <br>
 */
public enum GoogleDriveCopyOption implements CopyOption {

    /**
     * you can use for OCR image files.
     */
    EXPORT_AS_GDOCS("application/vnd.google-apps.document");

    /** */
    private String value;

    /** */
    private GoogleDriveCopyOption(String value) {
        this.value = value;
    }

    /** */
    public String getValue() {
        return value;
    }
}

/* */
