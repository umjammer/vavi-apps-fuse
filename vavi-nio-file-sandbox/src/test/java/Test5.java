/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * Test5. (fuse-nio-adapter)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/28 umjammer initial version <br>
 */
public class Test5 {

    @Test
    @Disabled("doesn't work")
    public void test01() throws Exception {
//        String email = System.getenv("TEST5_GOOGLE_ACCOUNT");
        String mp = System.getenv("TEST5_GOOGLE_MOUNT_POINT");

//        URI uri = URI.create("googledrive:///?id=" + email);
//        Map<String, Object> env = new HashMap<>();
//        env.put("ignoreAppleDouble", true);
//        FileSystem fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, env);
//        Path remote = fs.getRootDirectories().iterator().next();
        Path remote = Paths.get("/tmp");

//        Path mountPoint = Paths.get(mp);
//        Mounter mounter = FuseMountFactory.getMounter();
//        EnvironmentVariables envVars = EnvironmentVariables.create()
//                .withFlags(mounter.defaultMountFlags())
//                .withMountPoint(mountPoint)
//                .withRevealCommand("nautilus")
//                .build();
//        Mount mnt = mounter.mount(remote, envVars);
//        mnt.revealInFileManager();
//        System.out.println("Wait for it...");
//        System.in.read();
//        mnt.unmountForced();
    }
}

/* */
