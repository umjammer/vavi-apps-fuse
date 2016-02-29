/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import vavi.net.fuse.onedrive.OneDriveFS;


/**
 * OneDriveFS. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
public class OneDrive {

    public static void main(final String... args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: OneDrive <mountpoint>");
            System.exit(1);
        }
        new OneDriveFS().mount(args[0]);
    }
}

/* */
