/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.nuxeo.onedrive.client.JavaNetRequestExecutor;
import org.nuxeo.onedrive.client.OneDriveAPI;
import org.nuxeo.onedrive.client.OneDriveBasicAPI;
import org.nuxeo.onedrive.client.OneDriveDrive;
import org.nuxeo.onedrive.client.RequestExecutor;
import org.nuxeo.onedrive.client.RequestHeader;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.net.auth.WithTotpUserCredential;
import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphOAuth2;
import vavi.net.auth.web.microsoft.MicrosoftLocalUserCredential;


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
        String accessToken = new MicrosoftGraphOAuth2(appCredential, true).authorize(userCredential);
//Debug.println("accessToken: " + accessToken);

        RequestExecutor executor = new JavaNetRequestExecutor(accessToken) {
            @Override
            public void addAuthorizationHeader(final Set<RequestHeader> headers) {
                super.addAuthorizationHeader(headers);
                // HttpURLConnection adds "accept" header which is unavailable to onedrive.
                headers.add(new RequestHeader("Accept", "application/json"));
            }

            @Override
            public Upload doPatch(URL url, Set<RequestHeader> headers) throws IOException {
                headers.add(new RequestHeader("X-HTTP-Method-Override", "PATCH"));
                headers.add(new RequestHeader("X-HTTP-Method", "PATCH"));
                return super.doPost(url, headers);
            }
        };

        OneDriveAPI client = new OneDriveBasicAPI(executor) {
            @Override
            public RequestExecutor getExecutor() {
                return executor;
            }

            @Override
            public boolean isBusinessConnection() {
                return false;
            }

            @Override
            public boolean isGraphConnection() {
                return true;
            }

            @Override
            public String getBaseURL() {
                return String.format("https://graph.microsoft.com%s", "/v1.0");
            }

            @Override
            public String getEmailURL() {
                return String.format("https://graph.microsoft.com%s", "/v1.0/me");
            }
        };

        OneDriveDrive drive = OneDriveDrive.getDefaultDrive(client);
        final OneDriveFileStore fileStore = new OneDriveFileStore(drive, factoryProvider.getAttributesFactory());
        return new OneDriveFileSystemDriver(fileStore, factoryProvider, client, drive, env);
    }
}
