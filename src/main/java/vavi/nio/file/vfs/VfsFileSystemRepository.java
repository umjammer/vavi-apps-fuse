/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.IdentityInfo;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;
import com.jcraft.jsch.UserInfo;

import vavi.nio.file.onedrive4.OneDriveFileSystemProvider;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * VfsFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/04/06 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class VfsFileSystemRepository extends FileSystemRepositoryBase {

    public VfsFileSystemRepository() {
        super("vfs", new VfsFileSystemFactoryProvider());
    }

    /** */
    static abstract class Factory {
        @Property(name = "vfs.username.{0}")
        protected String username;
        @Property(name = "vfs.password.{0}")
        protected transient String password;
        @Property(name = "vfs.host.{0}")
        protected String host;
        @Property(name = "vfs.port.{0}")
        protected String port;
        /** */
        protected URI uri;

        /** */
        Factory(URI uri) {
            this.uri = uri;
            String[] userInfo = uri.getUserInfo() != null ? uri.getUserInfo().split(":") : null;
            this.username = userInfo != null && !userInfo[0].isEmpty() ? userInfo[0] : null;
            this.password = userInfo != null && !userInfo[1].isEmpty() ? userInfo[1] : null;
            this.host = uri.getHost();
            this.port = uri.getPort() != -1 ? String.valueOf(uri.getPort()) : null;
        }

        /** */
        public String buildBaseUrl() {
            StringBuilder sb = new StringBuilder();
            sb.append(uri.getScheme());
            sb.append("://");
            if (username != null) {
                sb.append(username);
            }
            if (password != null) {
                sb.append(":");
                sb.append(password);
            }
            if (host != null) {
                if (username != null || password != null) {
                    sb.append("@");
                }
                sb.append(host);
            }
            if (port != null) {
                sb.append(":");
                sb.append(port);
            }
            if (uri.getPath() != null) {
                sb.append(uri.getPath());
            }
            return sb.toString();
        }

        /** */
        abstract FileSystemOptions getFileSystemOptions() throws IOException;

        /** */
        static Factory getFactory(URI uri) {
            String protocol = uri.getScheme();
            switch (protocol) {
            case "smb": return new SmbFactory(uri);
            case "sftp": return new SftpFactory(uri);
            case "webdav": return new WebdavFactory(uri);
            default: throw new IllegalArgumentException(protocol);
            }
        }
    }

    @PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
    private static class SmbFactory extends Factory {
        @Property(name = "vfs.domain.{0}")
        private String domain;
        SmbFactory(URI uri) {
            super(uri);
        }
        @Override
        public FileSystemOptions getFileSystemOptions() throws IOException {
            FileSystemOptions options = new FileSystemOptions();
            StaticUserAuthenticator auth = new StaticUserAuthenticator(domain, username, password);
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(options, auth);
            return options;
        }
        /** */
        public String buildBaseUrl() {
            StringBuilder sb = new StringBuilder();
            sb.append(uri.getScheme());
            sb.append("://");
            if (host != null) {
                sb.append(host);
            }
            if (port != null) {
                sb.append(":");
                sb.append(port);
            }
            if (uri.getPath() != null) {
                sb.append(uri.getPath());
            }
            return sb.toString();
        }
    }

    @PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
    private static class SftpFactory extends Factory {
        class SftpUserInfo implements UserInfo {
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
        @Property(name = "vfs.keyPath.{0}")
        private String keyPath;
        @Property(name = "vfs.passphrase.{0}")
        private transient String passphrase;
        SftpFactory(URI uri) {
            super(uri);
        }
        @Override
        public FileSystemOptions getFileSystemOptions() throws IOException {
            boolean pkc = passphrase != null;
            FileSystemOptions options = new FileSystemOptions();
            SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(options, false);
            SftpFileSystemConfigBuilder.getInstance().setSessionTimeoutMillis(options, 10000);
            SftpFileSystemConfigBuilder.getInstance().setUserInfo(options, new SftpUserInfo(pkc ? passphrase : password, pkc));
            if (pkc) {
                SftpFileSystemConfigBuilder.getInstance().setIdentityInfo(options, new IdentityInfo(new File(keyPath)));
            }
            return options;
        }
        @Override
        public String buildBaseUrl() {
            StringBuilder sb = new StringBuilder();
            sb.append(uri.getScheme());
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
            if (port != null) {
                sb.append(":");
                sb.append(port);
            }
            if (uri.getPath() != null) {
                sb.append(uri.getPath());
            }
            return sb.toString();
        }
    }

    @PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
    private static class WebdavFactory extends Factory {
        WebdavFactory(URI uri) {
            super(uri);
        }
        @Override
        public FileSystemOptions getFileSystemOptions() throws IOException {
            FileSystemOptions options = new FileSystemOptions();
            return options;
        }
    }

    /**
     * @param uri "vfs:protocol:///?id=alias", sub url (after "vfs:") parts will be replaced by properties.
     */
    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        String uriString = uri.toString();
        URI subUri = URI.create(uriString.substring(uriString.indexOf(':') + 1));
        String protocol = subUri.getScheme();
Debug.println("protocol: " + protocol);

        Map<String, String> params = getParamsMap(subUri);
        if (!params.containsKey(OneDriveFileSystemProvider.PARAM_ID)) {
            throw new NoSuchElementException("sub uri not contains a param " + OneDriveFileSystemProvider.PARAM_ID);
        }
        final String alias = params.get(OneDriveFileSystemProvider.PARAM_ID);

        Factory factory = Factory.getFactory(subUri);
        PropsEntity.Util.bind(factory, alias);
Debug.println("baseUrl: " + factory.buildBaseUrl());

        FileSystemManager manager = VFS.getManager();
//for (String scheme : manager.getSchemes()) {
// System.err.println("scheme: " + scheme);
//}
        if (!manager.hasProvider(protocol)) {
            throw new IllegalStateException("missing provider: " + protocol);
        }

        FileObject root = manager.resolveFile(factory.buildBaseUrl(), factory.getFileSystemOptions());
        final VfsFileStore fileStore = new VfsFileStore(root, factoryProvider.getAttributesFactory());
        return new VfsFileSystemDriver(fileStore, factoryProvider, manager, factory, env);
    }

    /* ad-hoc hack for ignoring checking opacity */
    protected void checkURI(@Nullable final URI uri) {
        Objects.requireNonNull(uri);
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("uri is not absolute");
        }
        if (!getScheme().equals(uri.getScheme())) {
            throw new IllegalArgumentException("bad scheme");
        }
    }
}
