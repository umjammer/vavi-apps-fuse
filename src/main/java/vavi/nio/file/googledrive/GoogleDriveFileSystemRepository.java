/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.drive.Drive;

import vavi.net.auth.oauth2.google.GoogleAuthenticator;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * GoogleDriveFileSystemRepository.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/30 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
@PropsEntity(url = "classpath:googledrive.properties")
public final class GoogleDriveFileSystemRepository extends FileSystemRepositoryBase {

    public GoogleDriveFileSystemRepository() {
        super("googledrive", new GoogleDriveFileSystemFactoryProvider());
    }

    /** Application name. TODO app credencial? */
    private static final String APPLICATION_NAME = "vavi-apps-fuse";

    /** should have a constructor without args */
    @Property(value = "vavi.net.auth.oauth2.google.GoogleDriveLocalAuthenticator")
    private String authenticatorClassName;

    /* */
    {
        try {
            PropsEntity.Util.bind(this);
        } catch (Exception e) {
Debug.println(Level.WARNING, "no onedrive.properties in classpath, use defaut");
            authenticatorClassName = "vavi.net.auth.oauth2.google.GoogleDriveLocalAuthenticator";
        }
    }

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

        GoogleAuthenticator<Credential> authenticator = getAuthenticator();
        Credential credential = authenticator.authorize(email);
        Drive drive = new Drive.Builder(authenticator.getHttpTransport(), authenticator.getJsonFactory(), credential)
                .setHttpRequestInitializer(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest httpRequest) throws IOException {
                        credential.initialize(httpRequest);
                        httpRequest.setConnectTimeout(30 * 1000);
                        httpRequest.setReadTimeout(30 * 1000);
                    }
                })
                .setApplicationName(APPLICATION_NAME)
                .build();

        GoogleDriveFileStore fileStore = new GoogleDriveFileStore(drive, factoryProvider.getAttributesFactory());
        return new GoogleDriveFileSystemDriver(fileStore, factoryProvider, drive, env);
    }

    /** */
    private GoogleAuthenticator<Credential> getAuthenticator() {
        try {
            GoogleAuthenticator<Credential> authenticator = GoogleAuthenticator.class.cast(Class.forName(authenticatorClassName)
                .getDeclaredConstructor().newInstance());
            return authenticator;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    };
}
