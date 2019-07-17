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

    public interface Options {
        FileSystemOptions getFileSystemOptions() throws IOException;
        default String buildBaseUrl(String baseUrl) {
            return baseUrl;
        }
        static Options getOptions(String protocol) {
            switch (protocol) {
            case "smb": return new SmbOptions();
            case "sftp": return new SftpOptions();
            case "webdav": return new WebdavOptions();
            default: throw new IllegalArgumentException(protocol);
            }
        }
    }

    @PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
    private static class SmbOptions implements Options {
        @Property(name = "vfs.domain.{0}")
        private String domain;
        @Property(name = "vfs.username.{0}")
        private String username;
        @Property(name = "vfs.password.{0}")
        private transient String password;
        @Override
        public FileSystemOptions getFileSystemOptions() throws IOException {
            FileSystemOptions options = new FileSystemOptions();
            StaticUserAuthenticator auth = new StaticUserAuthenticator(domain, username, password);
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(options, auth);
            return options;
        }
    }

    @PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
    private static class SftpOptions implements Options {
        class SftpPassphraseUserInfo implements UserInfo {
            String passPhrase = null;
            public SftpPassphraseUserInfo(final String pp) {
                passPhrase = pp;
            }
            public String getPassphrase() {
                return passPhrase;
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
        @Property(name = "vfs.keyPath.{0}")
        private String keyPath;
        @Property(name = "vfs.passphrase.{0}")
        private transient String passphrase;
        @Property(name = "vfs.host.{0}")
        private String host;
        @Property(name = "vfs.port.{0}")
        private String port;
        @Property(name = "vfs.username.{0}")
        private String username;
        @Override
        public FileSystemOptions getFileSystemOptions() throws IOException {
            FileSystemOptions options = new FileSystemOptions();
            SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(options, false);
            SftpFileSystemConfigBuilder.getInstance().setSessionTimeoutMillis(options, 10000);
            SftpFileSystemConfigBuilder.getInstance().setUserInfo(options, new SftpPassphraseUserInfo(passphrase));
            SftpFileSystemConfigBuilder.getInstance().setIdentityInfo(options, new IdentityInfo(new File(keyPath)));
            return options;
        }
        @Override
        public String buildBaseUrl(String baseUrl) {
            return String.format(baseUrl, username, host, port);
        }
    }

    @PropsEntity(url = "file://${user.home}/.vavifuse/credentials.properties")
    private static class WebdavOptions implements Options {
        @Property(name = "vfs.username.{0}")
        private String username;
        @Property(name = "vfs.password.{0}")
        private transient String password;
        @Property(name = "vfs.host.{0}")
        private String host;
        @Property(name = "vfs.port.{0}")
        private String port;
        @Override
        public FileSystemOptions getFileSystemOptions() throws IOException {
            FileSystemOptions options = new FileSystemOptions();
            return options;
        }
        @Override
        public String buildBaseUrl(String baseUrl) {
            return String.format(baseUrl, username, password, host, port);
        }
    }

    /**
     * @param env { "baseUrl": "smb://10.3.1.1/Temporary Share/", "alias": "cysconas" }
     */
    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        final String alias = (String) env.get("alias");
        if (alias == null) {
            throw new IllegalArgumentException("env: alias not found");
        }
        final String baseUrl = (String) env.get("baseUrl");
        if (baseUrl == null) {
            throw new IllegalArgumentException("env: baseUrl not found");
        }

        String protocol = baseUrl.substring(0, baseUrl.indexOf(':'));
System.err.println("protocol: " + protocol);
        Options options = Options.getOptions(protocol);
        PropsEntity.Util.bind(options, alias);

        FileSystemManager manager = VFS.getManager();
        for (String scheme : manager.getSchemes()) {
            System.err.println("scheme: " + scheme);
        }
        if (!manager.hasProvider(protocol)) {
            throw new IllegalStateException("missing provider: " + protocol);
        }
        FileObject root = manager.resolveFile(baseUrl, options.getFileSystemOptions());
        final VfsFileStore fileStore = new VfsFileStore(root, factoryProvider.getAttributesFactory());
        return new VfsFileSystemDriver(fileStore, factoryProvider, manager, options, env);
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
