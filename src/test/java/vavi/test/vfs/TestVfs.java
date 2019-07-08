package vavi.test.vfs;
/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * TestVfs (smb).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/08/08 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
public class TestVfs {

    @Property(name = "vfs.domain.{0}")
    private String domain;
    @Property(name = "vfs.username.{0}")
    private String username;
    @Property(name = "vfs.password.{0}")
    private transient String password;
    private String baseUrl;
    private String alias;

    /**
     * @param args 0: base url, 1: alias
     */
    public static void main(String[] args) throws Exception {
        TestVfs app = new TestVfs();
        app.baseUrl = args[0];
        app.alias = args[1];
        PropsEntity.Util.bind(app, app.alias);
        app.proceed();
    }

    void proceed() throws IOException {
        StaticUserAuthenticator auth = new StaticUserAuthenticator(domain, username, password);
        FileSystemOptions opts = new FileSystemOptions();
        DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
        FileSystemManager fs = VFS.getManager();
        if (!fs.hasProvider("smb"))
            throw new RuntimeException("Provider missing: smb");
System.err.println("Connecting \"" + baseUrl + "\" with " + opts);
        FileObject smbFile = fs.resolveFile(baseUrl, opts); // added opts!
System.err.println(smbFile.exists() + " " + smbFile.getContent().getLastModifiedTime());
        if (smbFile.getType().equals(FileType.FOLDER)) {
            for (FileObject fo : smbFile.getChildren()) {
System.err.println(fo.getName());
            }
        }
    }
}

/* */
