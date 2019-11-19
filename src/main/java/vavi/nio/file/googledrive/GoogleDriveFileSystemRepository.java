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
import com.google.api.services.drive.Drive;

import vavi.net.auth.oauth2.Authenticator;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.net.auth.oauth2.google.GoogleDriveLocalAuthenticator.HTTP_TRANSPORT;
import static vavi.net.auth.oauth2.google.GoogleDriveLocalAuthenticator.JSON_FACTORY;


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

        Credential credential = authorize(email);
        Drive drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

        GoogleDriveFileStore fileStore = new GoogleDriveFileStore(drive, factoryProvider.getAttributesFactory());
        return new GoogleDriveFileSystemDriver(fileStore, factoryProvider, drive, env);
    }

    /** */
    private Credential authorize(String id) throws IOException {
        try {
            Authenticator<Credential> authenticator = Authenticator.class.cast(Class.forName(authenticatorClassName)
                .getDeclaredConstructor().newInstance());
            return authenticator.authorize(id);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    };
}
