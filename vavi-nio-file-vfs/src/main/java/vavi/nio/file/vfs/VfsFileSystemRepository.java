/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.vfs;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.net.auth.proprietary.vfs.VfsAuthenticator;
import vavi.net.auth.proprietary.vfs.VfsCredential;
import vavi.util.Debug;


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

    /**
     * @param uri "vfs:protocol:///?alias=alias", sub url (after "vfs:") parts will be replaced by properties.
     *            if you don't use alias, the url must include username, password, host, port.
     */
    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        String uriString = uri.toString();
        URI subUri = URI.create(uriString.substring(uriString.indexOf(':') + 1));
        String protocol = subUri.getScheme();
Debug.println(Level.FINE, "protocol: " + protocol);

        Map<String, String> params = getParamsMap(subUri);
        String alias = params.get(VfsFileSystemProvider.PARAM_ALIAS);

        VfsAuthenticator authenticator = VfsAuthenticator.getAuthenticator(subUri);
        VfsCredential credential = authenticator.getCredential(alias, subUri);
        FileSystemOptions options = authenticator.authorize(credential);

        String baseUrl = credential.buildBaseUrl();
        if (subUri.getPath() != null) {
            baseUrl += subUri.getPath();
        }
Debug.println(Level.FINE, "baseUrl: " + baseUrl);

        FileSystemManager manager = VFS.getManager();
        if (!manager.hasProvider(protocol)) {
if (Debug.isLoggable(Level.FINE)) {
 for (String scheme : manager.getSchemes()) {
  System.err.println("scheme: " + scheme);
 }
}
            throw new IllegalStateException("missing provider: " + protocol);
        }

        FileObject root = manager.resolveFile(baseUrl, options);
        VfsFileStore fileStore = new VfsFileStore(root, factoryProvider.getAttributesFactory());
        return new VfsFileSystemDriver(fileStore, factoryProvider, manager, options, baseUrl, env);
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
