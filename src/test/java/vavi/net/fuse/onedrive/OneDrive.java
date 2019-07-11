/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.onedrive;

/**
 * test onedrive on fuse-jna.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
public class OneDrive {

    /**
     * @param args 0: mount point, 1: email
     */
    public static void main(final String... args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: OneDrive <mountpoint> <email>");
            System.exit(1);
        }

        String email = args[1];

        new OneDriveFS(email).mount(args[0]);
    }
}

/* */
