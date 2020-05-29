/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.cryptomator.frontend.fuse.mount.EnvironmentVariables;
import org.cryptomator.frontend.fuse.mount.FuseMountFactory;
import org.cryptomator.frontend.fuse.mount.Mount;
import org.cryptomator.frontend.fuse.mount.Mounter;


/**
 * Test5. (fuse-nio-adapter)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/28 umjammer initial version <br>
 */
public class Test5 {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        Path mountPoint = Files.createTempDirectory("fuse-mount");
        Mounter mounter = FuseMountFactory.getMounter();
        EnvironmentVariables envVars = EnvironmentVariables.create()
                .withFlags(mounter.defaultMountFlags())
                .withMountPoint(mountPoint)
                .withRevealCommand("nautilus")
                .build();
        Path tmp = Paths.get("/tmp");
        Mount mnt = mounter.mount(tmp, envVars);
        mnt.revealInFileManager();
        System.out.println("Wait for it...");
        System.in.read();
        mnt.unmountForced();
    }
}

/* */
