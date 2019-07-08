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

import vavi.net.auth.oauth2.BasicAppCredential;
import vavi.net.auth.oauth2.OAuth2;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
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

    /** */
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

        BasicAppCredential appCredential = new MicrosoftGraphLocalAppCredential();
        PropsEntity.Util.bind(appCredential);

        String accessToken = new OAuth2(appCredential, true, authenticatorClassName).getAccessToken(email);
Debug.println("accessToken: " + accessToken);

        IGraphServiceClient graphClient = GraphServiceClient.builder()
            .authenticationProvider(new IAuthenticationProvider() {
                @Override
                public void authenticateRequest(IHttpRequest request) {
                    request.addHeader("Authorization", "Bearer " + accessToken);
                }
            })
            .buildClient();

        final OneDriveFileStore fileStore = new OneDriveFileStore(graphClient, factoryProvider.getAttributesFactory());
        return new OneDriveFileSystemDriver(fileStore, factoryProvider, graphClient, env);
    }
}
