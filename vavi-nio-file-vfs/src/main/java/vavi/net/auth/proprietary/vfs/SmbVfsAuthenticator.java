/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.proprietary.vfs;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;

import vavi.net.http.HttpUtil;
import vavi.nio.file.vfs.VfsFileSystemProvider;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * SmbVfsAuthenticator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/05/02 umjammer initial version <br>
 */
public class SmbVfsAuthenticator implements VfsAuthenticator {

    /**
     * <p>
     * properties file "~/vavifuse/credentials.properties"
     * <ul>
     * <li> smb.domain.alias
     * </ul>
     * </p>
     */
    @PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
    private static class SmbVfsCredential extends VfsCredential {

        @Property(name = "smb.domain.{0}")
        private String domain;

        public SmbVfsCredential(String alias) {
            super(alias);
        }

        /**
         * @param uri ?domain={domain}
         */
        public SmbVfsCredential(URI uri) {
            super(uri);
            try {
                Map<String, String[]> params = HttpUtil.splitQuery(uri);
                this.domain = params.get("domain")[0];
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String getClientId() {
            return "smb";
        }

        @Override
        public String buildBaseUrl() {
            StringBuilder sb = new StringBuilder();
            sb.append(getClientId());
            sb.append("://");
            if (host != null) {
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
            credential = new SmbVfsCredential(alias);
Debug.println("credential: by alias " + alias);
        } else {
            credential = new SmbVfsCredential(uri);
            if (credential.getId() == null || credential.getId().isEmpty()) {
                throw new NoSuchElementException("uri should have a username or a param " + VfsFileSystemProvider.PARAM_ALIAS);
            }
Debug.println("credential: by uri");
        }

        return credential;
    }

    @Override
    public FileSystemOptions authorize(VfsCredential credential) throws IOException {
        SmbVfsCredential c = SmbVfsCredential.class.cast(credential);

        FileSystemOptions options = new FileSystemOptions();
        StaticUserAuthenticator auth = new StaticUserAuthenticator(c.domain, c.username, c.password);
        DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(options, auth);
        return options;
    }
}

/* */
