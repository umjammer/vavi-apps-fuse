package vavi.nio.file.vfs;
/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.net.URI;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.junit.jupiter.api.Test;

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
        FileObject File = fs.resolveFile(baseUrl, opts); // added opts!
System.err.println(File.exists() + " " + File.getContent().getLastModifiedTime());
        if (File.isFolder()) {
            for (FileObject fo : File.getChildren()) {
System.err.println(fo.getName());
            }
        }
    }

    @Test
    void test00() throws Exception {
        URI uri = URI.create("vfs:sftp://user:password@nsanomac4.local:10022/Users/nsano?alias=alias");
        System.err.println(uri.getScheme());
        System.err.println(uri.getHost());
        System.err.println(uri.getPath());
        System.err.println(uri.getPort());
        System.err.println(uri.getQuery());
        System.err.println(uri.getFragment());
        System.err.println(uri.getAuthority());
        System.err.println(uri.getUserInfo());

        String uriString = uri.toString();
        URI subUri = URI.create(uriString.substring(uriString.indexOf(':') + 1));
        System.err.println(subUri.getScheme());
        System.err.println(subUri.getHost());
        System.err.println(subUri.getPath());
        System.err.println(subUri.getPort());
        System.err.println(subUri.getQuery());
        System.err.println(subUri.getFragment());
        System.err.println(subUri.getAuthority());
        System.err.println(subUri.getUserInfo());
    }
}

/* */
