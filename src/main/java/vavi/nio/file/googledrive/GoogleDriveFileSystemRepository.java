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
import com.google.api.services.drive.Drive;

import vavi.net.auth.WithTotpUserCredential;
import vavi.net.auth.oauth2.google.GoogleAppCredential;
import vavi.net.auth.oauth2.google.GoogleLocalAppCredential;
import vavi.net.auth.oauth2.google.GoogleOAuth2;
import vavi.net.auth.web.google.GoogleLocalUserCredential;


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

    /**
     * TODO root from uri
     * @throws NoSuchElementException
     */
    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        // 1. user credential
        WithTotpUserCredential userCredential = null;

        if (env.containsKey(GoogleDriveFileSystemProvider.ENV_USER_CREDENTIAL)) {
            userCredential = WithTotpUserCredential.class.cast(env.get(GoogleDriveFileSystemProvider.ENV_USER_CREDENTIAL));
        }

        Map<String, String> params = getParamsMap(uri);
        if (userCredential == null && params.containsKey(GoogleDriveFileSystemProvider.PARAM_ID)) {
            String email = params.get(GoogleDriveFileSystemProvider.PARAM_ID);
            userCredential = new GoogleLocalUserCredential(email);
        }

        if (userCredential == null) {
            throw new NoSuchElementException("uri not contains a param " + GoogleDriveFileSystemProvider.PARAM_ID + " nor " +
                                             "env not contains a param " + GoogleDriveFileSystemProvider.ENV_USER_CREDENTIAL);
        }

        // 2. app credential
        GoogleAppCredential appCredential = null;

        if (env.containsKey(GoogleDriveFileSystemProvider.ENV_APP_CREDENTIAL)) {
            appCredential = GoogleAppCredential.class.cast(env.get(GoogleDriveFileSystemProvider.ENV_APP_CREDENTIAL));
        }

        if (appCredential == null) {
            appCredential = new GoogleLocalAppCredential(); // TODO use props
        }

        // 3. process
        Drive drive = new GoogleOAuth2(appCredential).authorize(userCredential);
        GoogleDriveFileStore fileStore = new GoogleDriveFileStore(drive, factoryProvider.getAttributesFactory());
        return new GoogleDriveFileSystemDriver(fileStore, factoryProvider, drive, env);
    }
}
