/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.proprietary.vfs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.sftp.IdentityInfo;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import com.jcraft.jsch.UserInfo;

import vavi.net.http.HttpUtil;
import vavi.nio.file.vfs.VfsFileSystemProvider;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * SftpVfsAuthenticator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/02 umjammer initial version <br>
 */
public class SftpVfsAuthenticator implements VfsAuthenticator {

    /**
     * <p>
     * properties file "~/vavifuse/credentials.properties"
     * <ul>
     * <li> ssh.keyPath.alias
     * <li> ssh.passphrase.alias
     * </ul>
     * </p>
     */
    @PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
    private static class SftpVfsCredential extends VfsCredential {
        @Property(name = "ssh.keyPath.{0}")
        private String keyPath;
        @Property(name = "ssh.passphrase.{0}")
        private transient String passphrase;

        public SftpVfsCredential(String alias) {
            super(alias);
        }

        /**
         * @param uri ?keyPath={keyPath}&passphrase={passphrase}
         */
        public SftpVfsCredential(URI uri) {
            super(uri);
            try {
                Map<String, String[]> params = HttpUtil.splitQuery(uri);
                this.keyPath = params.get("keyPath")[0];
                this.passphrase = params.get("passphrase")[0];
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String getClientId() {
            return "sftp";
        }

        @Override
        public String buildBaseUrl() {
            StringBuilder sb = new StringBuilder();
            sb.append(getClientId());
            sb.append("://");
            if (username != null) {
                sb.append(username);
            }
            if (host != null) {
                if (username != null) {
                    sb.append("@");
                }
                sb.append(host);
            }
            if (port != -1) {
                sb.append(":");
                sb.append(port);
            }
            return sb.toString();
        }
    }

    @Override
    public VfsCredential getCredential(String alias, URI uri) {
        VfsCredential credential;
        if (alias != null) {
            credential = new SftpVfsCredential(alias);
Debug.println("credential: by alias " + alias);
        } else {
            credential = new SftpVfsCredential(uri);
            if (credential.getId() == null || credential.getId().isEmpty()) {
                throw new NoSuchElementException("uri should have a username or a param " + VfsFileSystemProvider.PARAM_ALIAS);
            }
Debug.println("credential: by uri");
        }

        return credential;
    }

    private static class SftpUserInfo implements UserInfo {
        boolean pkc;
        String passString = null;
        public SftpUserInfo(final String passString, boolean pkc) {
            this.passString = passString;
            this.pkc = pkc;
        }
        public String getPassphrase() {
            return pkc ? passString : null;
        }
        public String getPassword() {
            return pkc ? null : passString;
        }
        public boolean promptPassphrase(String prompt) {
            return pkc;
        }
        public boolean promptPassword(String prompt) {
            return !pkc;
        }
        public void showMessage(String message) {
        }
        public boolean promptYesNo(String str) {
            return true;
        }
    }

    @Override
    public FileSystemOptions authorize(VfsCredential credential) throws IOException {
        SftpVfsCredential c = SftpVfsCredential.class.cast(credential);

        boolean pkc = c.passphrase != null;
        FileSystemOptions options = new FileSystemOptions();
        SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder.getInstance();
        builder.setUserDirIsRoot(options, false);
        builder.setConnectTimeoutMillis(options, 30000);
        builder.setSessionTimeoutMillis(options, 30000);
        builder.setUserInfo(options, new SftpUserInfo(pkc ? c.passphrase : c.password, pkc));
        if (pkc) {
            builder.setIdentityProvider(options, new IdentityInfo(new File(c.keyPath)));
        }
        return options;
    }
}

/* */
