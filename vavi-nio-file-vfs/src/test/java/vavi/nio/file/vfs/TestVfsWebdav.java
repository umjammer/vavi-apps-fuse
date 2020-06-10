package vavi.nio.file.vfs;
/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.net.URLEncoder;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.webdav4.Webdav4FileSystemConfigBuilder;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * TestVfs2 (webdav).
 *
 * TODO jackrabbit webdav client cannot deal utf-8?
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/08/09 umjammer initial version <br>
 */
@Deprecated
@PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
public class TestVfsWebdav {

    @Property(name = "vfs.username.{0}")
    private String username;
    @Property(name = "vfs.password.{0}")
    private transient String password;
    @Property(name = "vfs.host.{0}")
    private String host;
    @Property(name = "vfs.port.{0}")
    private String port;
    private String baseUrl;
    private String alias;
    private String component;

    /**
     * @param args 0: base url (should be replaced by user name, host, port), 1: alias
     */
    public static void main(String[] args) throws Exception {
        TestVfsWebdav app = new TestVfsWebdav();
        app.alias = args[1];
        PropsEntity.Util.bind(app, app.alias);
        app.baseUrl = args[0];
        app.component = args[2];
        app.proceed();
    }

    void proceed() throws IOException {
        FileSystemOptions options = new FileSystemOptions();
        Webdav4FileSystemConfigBuilder.getInstance().setUrlCharset(options, "utf-8");
        FileSystemManager fs = VFS.getManager();
        if (!fs.hasProvider("webdav"))
            throw new RuntimeException("Provider missing: webdav");
        String baseUrl = String.format(this.baseUrl, username, password, host, port, URLEncoder.encode(component, "utf-8"));
System.err.println("Connecting \"" + baseUrl + "\" with " + options);
        FileObject davFile = fs.resolveFile(baseUrl, options); // added opts!
//System.err.println(smbFile.exists() + " " + smbFile.getContent().getLastModifiedTime());
        if (davFile.isFolder()) {
            for (FileObject fo : davFile.getChildren()) {
System.err.println(fo.getName()); // TODO 文字化け
            }
        }
    }
}

/* */
