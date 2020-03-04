/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.nio.file.OpenOption;


/**
 * GoogleDriveOpenOption.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/01 umjammer initial version <br>
 */
public enum GoogleDriveOpenOption implements OpenOption {

    EXPORT_WITH_GDOCS_DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    EXPORT_WITH_GDOCS_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    /** */
    private String value;

    /** */
    private GoogleDriveOpenOption(String value) {
        this.value = value;
    }

    /** */
    public String getValue() {
        return value;
    }
}

/* */
