/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;

import vavi.net.auth.WithTotpUserCredential;
import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphOAuth2;
import vavi.net.auth.web.microsoft.MicrosoftLocalUserCredential;
import vavi.nio.file.onedrive4.graph.MyLogger;


/**
 * OneDriveFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
public final class OneDriveFileSystemRepository extends FileSystemRepositoryBase {

    /** */
    public OneDriveFileSystemRepository() {
        super("onedrive", new OneDriveFileSystemFactoryProvider());
    }

    /**
     * TODO root from uri
     * @throws NoSuchElementException required values are not in env
     */
    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        // 1. user credential
        WithTotpUserCredential userCredential = null;

        if (env.containsKey(OneDriveFileSystemProvider.ENV_USER_CREDENTIAL)) {
            userCredential = WithTotpUserCredential.class.cast(env.get(OneDriveFileSystemProvider.ENV_USER_CREDENTIAL));
        }

        Map<String, String> params = getParamsMap(uri);
        if (userCredential == null && params.containsKey(OneDriveFileSystemProvider.PARAM_ID)) {
            String email = params.get(OneDriveFileSystemProvider.PARAM_ID);
            userCredential = new MicrosoftLocalUserCredential(email);
        }

        if (userCredential == null) {
            throw new NoSuchElementException("uri not contains a param " + OneDriveFileSystemProvider.PARAM_ID + " nor " +
                                             "env not contains a param " + OneDriveFileSystemProvider.ENV_USER_CREDENTIAL);
        }

        // 2. app credential
        OAuth2AppCredential appCredential = null;

        if (env.containsKey(OneDriveFileSystemProvider.ENV_APP_CREDENTIAL)) {
            appCredential = OAuth2AppCredential.class.cast(env.get(OneDriveFileSystemProvider.ENV_APP_CREDENTIAL));
        }

        if (appCredential == null) {
            appCredential = new MicrosoftGraphLocalAppCredential();
        }

        // 3. process
        MicrosoftGraphOAuth2 oAuth2 = new MicrosoftGraphOAuth2(appCredential, true);
        String accessToken = oAuth2.authorize(userCredential);
//Debug.println("accessToken: " + accessToken);

        IAuthenticationProvider authenticationProvider = new IAuthenticationProvider() {
            @Override
            public void authenticateRequest(IHttpRequest request) {
                request.addHeader("Authorization", "Bearer " + accessToken);
            }
        };
        IGraphServiceClient graphClient = GraphServiceClient.builder()
            .authenticationProvider(authenticationProvider)
            .logger(new MyLogger())
            .buildClient();

        final OneDriveFileStore fileStore = new OneDriveFileStore(graphClient, factoryProvider.getAttributesFactory());
        return new OneDriveFileSystemDriver(fileStore, factoryProvider, graphClient, () -> oAuth2.close(), env);
    }
}
