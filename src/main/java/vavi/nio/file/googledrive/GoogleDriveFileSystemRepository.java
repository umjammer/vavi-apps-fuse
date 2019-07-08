/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.Drive;

import vavi.net.auth.oauth2.google.GoogleDriveLocalAuthenticator;

import static vavi.net.auth.oauth2.google.GoogleDriveLocalAuthenticator.HTTP_TRANSPORT;
import static vavi.net.auth.oauth2.google.GoogleDriveLocalAuthenticator.JSON_FACTORY;


/**
 * GoogleDriveFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class GoogleDriveFileSystemRepository extends FileSystemRepositoryBase {

    public GoogleDriveFileSystemRepository() {
        super("googledrive", new GoogleDriveFileSystemFactoryProvider());
    }

    /** Application name. TODO app credencial? */
    private static final String APPLICATION_NAME = "vavi-apps-fuse";

    /**
     * TODO root from uri
     * @throws NoSuchElementException
     */
    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        Map<String, String> params = getParamsMap(uri);
        if (!params.containsKey(GoogleDriveFileSystemProvider.PARAM_ID)) {
            throw new NoSuchElementException("uri not contains a param " + GoogleDriveFileSystemProvider.PARAM_ID);
        }
        final String email = params.get(GoogleDriveFileSystemProvider.PARAM_ID);

        GoogleDriveLocalAuthenticator authenticator = new GoogleDriveLocalAuthenticator();
        Credential credential = authenticator.authorize(email);
        Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

        GoogleDriveFileStore fileStore = new GoogleDriveFileStore(drive, factoryProvider.getAttributesFactory());
        return new GoogleDriveFileSystemDriver(fileStore, factoryProvider, drive, env);
    }
}
