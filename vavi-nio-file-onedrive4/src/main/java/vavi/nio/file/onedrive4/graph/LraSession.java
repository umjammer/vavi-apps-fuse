/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;

/**
 * THis internal class keeps a representation of the CopySession provided by
 * the OneDriveAPI
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/08 umjammer initial version <br>
 */
public class LraSession {

    String monitorUrl;

    /**
     * Gives the uploadURL to where the next range should be uploaded to
     *
     * @return the monitorURL to monitor
     */
    public String getMonitorURL() {
        return monitorUrl;
    }
}
