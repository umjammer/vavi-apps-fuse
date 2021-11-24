package vavi.nio.file.vfs;
/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.IdentityInfo;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import com.jcraft.jsch.UserInfo;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * TestVfs2 (sftp).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/08/09 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
public class TestVfsSftp {

    @Property(name = "vfs.keyPath.{0}")
    private String keyPath;
    @Property(name = "vfs.passphrase.{0}")
    private transient String passphrase;
    @Property(name = "vfs.host.{0}")
    private String host;
    @Property(name = "vfs.port.{0}", value = "22")
    private String port;
    @Property(name = "vfs.username.{0}")
    private String username;
    private String baseUrl;
    private String alias;

    /**
     * @param args 0: base url (should be replaced by user name, host, port), 1: alias
     */
    public static void main(String[] args) throws Exception {
        TestVfsSftp app = new TestVfsSftp();
        app.alias = args[1];
        PropsEntity.Util.bind(app, app.alias);
        app.baseUrl = args[0];
        app.proceed();
    }

    public static class SftpPassphraseUserInfo implements UserInfo {
        private String passphrase = null;
        public SftpPassphraseUserInfo(final String pp) {
            passphrase = pp;
        }
        public String getPassphrase() {
            return passphrase;
        }
        public String getPassword() {
            return null;
        }
        public boolean promptPassphrase(String prompt) {
            return true;
        }
        public boolean promptPassword(String prompt) {
            return false;
        }
        public void showMessage(String message) {
        }
        public boolean promptYesNo(String str) {
            return true;
        }
    }

    void proceed() throws IOException {
        FileSystemOptions options = new FileSystemOptions();
//        SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(options, "no");
        SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(options, false);
        SftpFileSystemConfigBuilder.getInstance().setSessionTimeout(options, Duration.ofMillis(10000));
        SftpFileSystemConfigBuilder.getInstance().setUserInfo(options, new SftpPassphraseUserInfo(passphrase));
        SftpFileSystemConfigBuilder.getInstance().setIdentityProvider(options, new IdentityInfo(new File(keyPath)));
        FileSystemManager fs = VFS.getManager();
        if (!fs.hasProvider("sftp"))
            throw new RuntimeException("Provider missing: sftp");
        String baseUrl = String.format(this.baseUrl, username, host, port);
System.err.println("Connecting \"" + baseUrl + "\" with " + options);
        FileObject File = fs.resolveFile(baseUrl, options); // added opts!
System.err.println("providerCapabilities ---");
fs.getProviderCapabilities("sftp").forEach(System.err::println);
System.err.println("---");
//System.err.println(smbFile.exists() + " " + smbFile.getContent().getLastModifiedTime());
        if (File.isFolder()) {
            for (FileObject fo : File.getChildren()) {
System.err.println(fo.getName());
            }
        }
    }
}

/* */
