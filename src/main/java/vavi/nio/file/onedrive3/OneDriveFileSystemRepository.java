/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.nuxeo.onedrive.client.JavaNetRequestExecutor;
import org.nuxeo.onedrive.client.OneDriveAPI;
import org.nuxeo.onedrive.client.OneDriveBasicAPI;
import org.nuxeo.onedrive.client.RequestExecutor;
import org.nuxeo.onedrive.client.RequestHeader;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.net.auth.oauth2.BasicAppCredential;
import vavi.net.auth.oauth2.LocalOAuth2;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * OneDriveFileSystemRepository.
 * <p>
 * set "authenticatorClassName" in "classpath:onedrive.properties"
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
@PropsEntity(url = "classpath:onedrive.properties")
public final class OneDriveFileSystemRepository extends FileSystemRepositoryBase {

    /** */
    public OneDriveFileSystemRepository() {
        super("onedrive", new OneDriveFileSystemFactoryProvider());
    }

    /** should be {@link vavi.net.auth.oauth2.Authenticator} and have a constructor with args (String, String) */
    @Property
    private String authenticatorClassName;

    /* */
    {
        try {
            PropsEntity.Util.bind(this);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * TODO root from uri
     * @throws NoSuchElementException required values are not in env
     */
    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        Map<String, String> params = getParamsMap(uri);
        if (!params.containsKey(OneDriveFileSystemProvider.PARAM_ID)) {
            throw new NoSuchElementException("uri not contains a param " + OneDriveFileSystemProvider.PARAM_ID);
        }
        final String email = params.get(OneDriveFileSystemProvider.PARAM_ID);

        if (!env.containsKey(OneDriveFileSystemProvider.ENV_CREDENTIAL)) {
            throw new NoSuchElementException("app credential not contains a param " + OneDriveFileSystemProvider.ENV_CREDENTIAL);
        }
        BasicAppCredential appCredential = BasicAppCredential.class.cast(env.get(OneDriveFileSystemProvider.ENV_CREDENTIAL));

        String accessToken = new LocalOAuth2(appCredential, true, authenticatorClassName).authorize(email);
Debug.println("accessToken: " + accessToken);

        RequestExecutor executor = new JavaNetRequestExecutor(accessToken) {
            @Override
            public void addAuthorizationHeader(final Set<RequestHeader> headers) {
                headers.add(new RequestHeader("Authorization", String.format("Bearer %s", accessToken)));
                // HttpURLConnection adds "accept" header which is unavailable to onedrive.
                headers.add(new RequestHeader("Accept", "application/json"));
            }

            /** TODO for debug */
            @Override
            protected Response toResponse(final HttpURLConnection connection) throws IOException {
                int responseCode = connection.getResponseCode();
                if (responseCode >= 400 || responseCode == -1) {
                    StringBuilder sb = new StringBuilder();
                    InputStream stream = connection.getErrorStream();
                    Scanner scanner = new Scanner(stream);
                    while (scanner.hasNextLine()) {
                        sb.append(scanner.nextLine());
                        sb.append("\n");
                    }
                    scanner.close();
new Exception(sb.toString()).printStackTrace();
                    throw new Error();
                }
                return super.toResponse(connection);
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
                return String.format("https://graph.microsoft.com%s", "/v1.0/me");
            }

            @Override
            public String getEmailURL() {
                return String.format("https://graph.microsoft.com%s", "/v1.0/me");
            }
        };

        final OneDriveFileStore fileStore = new OneDriveFileStore(client, factoryProvider.getAttributesFactory());
        return new OneDriveFileSystemDriver(fileStore, factoryProvider, client, env);
    }
}
