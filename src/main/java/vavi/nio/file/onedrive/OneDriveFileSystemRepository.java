/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.net.auth.oauth2.BasicAppCredential;
import vavi.net.auth.oauth2.WithTotpUserCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftLocalAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftLocalUserCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftOAuth2;
import vavi.util.Debug;

import static vavi.net.auth.oauth2.BasicAppCredential.wrap;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveFactory;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneDriveScope;
import de.tuberlin.onedrivesdk.networking.OneDriveAuthenticationException;


/**
 * OneDriveFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class OneDriveFileSystemRepository extends FileSystemRepositoryBase {

    public OneDriveFileSystemRepository() {
        super("onedrive", new OneDriveFileSystemFactoryProvider());
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

        Map<String, String> params = getParamsMap(uri);
        if (params.containsKey(OneDriveFileSystemProvider.PARAM_ID)) {
            String email = params.get(OneDriveFileSystemProvider.PARAM_ID);
            userCredential = new MicrosoftLocalUserCredential(email);
        }

        if (env.containsKey(OneDriveFileSystemProvider.ENV_USER_CREDENTIAL)) {
            userCredential = WithTotpUserCredential.class.cast(env.get(OneDriveFileSystemProvider.ENV_USER_CREDENTIAL));
        }

        if (userCredential == null) {
            throw new NoSuchElementException("uri not contains a param " + OneDriveFileSystemProvider.PARAM_ID + " nor " +
                                             "env not contains a param " + OneDriveFileSystemProvider.ENV_USER_CREDENTIAL);
        }

        // 2. app credential
        BasicAppCredential appCredential = null;

        if (env.containsKey(OneDriveFileSystemProvider.ENV_APP_CREDENTIAL)) {
            appCredential = BasicAppCredential.class.cast(env.get(OneDriveFileSystemProvider.ENV_APP_CREDENTIAL));
        }

        if (appCredential == null) {
            appCredential = new MicrosoftLocalAppCredential();
        }

        // 3. process
        try {
            OneDriveSDK client = OneDriveFactory.createOneDriveSDK(appCredential.getClientId(),
                                                                   appCredential.getClientSecret(),
                                                                   appCredential.getRedirectUrl(),
                                                                   OneDriveScope.OFFLINE_ACCESS);
            String url = client.getAuthenticationURL();

            MicrosoftOAuth2 oauth2 = new MicrosoftOAuth2(wrap(appCredential, url), userCredential.getId());
            String code = null;
            String refreshToken = oauth2.readRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                code = oauth2.authorize(userCredential);
                client.authenticate(code);
            } else {
                try {
                    client.authenticateWithRefreshToken(refreshToken);
                } catch (OneDriveAuthenticationException e) {
Debug.println("refreshToken: timeout?");
                    code = oauth2.authorize(userCredential);
                    client.authenticate(code);
                }
            }
//client.getRefreshToken()

            // start refresh token system
            Supplier<String> callback = () -> {
                try {
                    return client.getRefreshToken();
                } catch (OneDriveException e) {
                    throw new IllegalStateException(e);
                }
            };
            oauth2.writeRefreshToken(callback);
            client.startSessionAutoRefresh(() -> { oauth2.writeRefreshToken(callback); });

            final OneDriveFileStore fileStore = new OneDriveFileStore(client, factoryProvider.getAttributesFactory());
            return new OneDriveFileSystemDriver(fileStore, factoryProvider, client, env);

        } catch (OneDriveException e) {
            throw new IllegalStateException(e);
        }
    }
}
