/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static vavi.nio.file.Util.toPathString;


/**
 * UploadMonitor.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/19 umjammer initial version <br>
 */
public class UploadMonitor {

    /** NFC normalized {@link String} */
    private Set<String> uploadFlags = new HashSet<>();

    /** */
    public void start(Path path) throws IOException {
        uploadFlags.add(toPathString(path));
    }

    /** */
    public void finish(Path path) throws IOException {
        uploadFlags.remove(toPathString(path));
    }

    /** */
    public boolean isUploading(Path path) throws IOException {
        return uploadFlags.contains(toPathString(path));
    }
}

/* */
