/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.net.auth.oauth2.Authenticator;
import vavi.net.auth.oauth2.BasicAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftLocalAppCredential;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveFactory;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneDriveScope;
import de.tuberlin.onedrivesdk.networking.OneDriveAuthenticationException;


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

    public OneDriveFileSystemRepository() {
        super("onedrive", new OneDriveFileSystemFactoryProvider());
    }

    /** application credential */
    private BasicAppCredential appCredential;

    /** cloud driver */
    private transient OneDriveSDK client;

    /** for refreshToken */
    private File file;

    @Property
    private String authenticatorClassName;

    /** */
    private Authenticator authenticator;

    {
        try {
            PropsEntity.Util.bind(this);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * TODO root from uri
     * @throws NoSuchElementException
     */
    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        try {
            Map<String, String> params = getParamsMap(uri);
            if (!params.containsKey(OneDriveFileSystemProvider.PARAM_ID)) {
                throw new NoSuchElementException("uri not contains a param " + OneDriveFileSystemProvider.PARAM_ID);
            }
            final String email = params.get(OneDriveFileSystemProvider.PARAM_ID);

            appCredential = new MicrosoftLocalAppCredential();
            PropsEntity.Util.bind(appCredential);

            file = new File(System.getProperty("user.home"), ".vavifuse/" + appCredential.getScheme() + "/" + email);

            client = OneDriveFactory.createOneDriveSDK(appCredential.getClientId(),
                                                       appCredential.getClientSecret(),
                                                       appCredential.getRedirectUrl(),
                                                       OneDriveScope.OFFLINE_ACCESS);
            String url = client.getAuthenticationURL();

            String refreshToken = readRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                authenticate(url, email);
            } else {
                try {
                    client.authenticateWithRefreshToken(refreshToken);
                } catch (OneDriveAuthenticationException e) {
Debug.println("refreshToken: timeout?");
                    authenticate(url, email);
                }
            }

            writeRefreshToken();
            client.startSessionAutoRefresh(this::writeRefreshToken);

            final OneDriveFileStore fileStore = new OneDriveFileStore(client, factoryProvider.getAttributesFactory());
            return new OneDriveFileSystemDriver(fileStore, factoryProvider, client, env);

        } catch (OneDriveException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    private void authenticate(String url, String email) throws IOException, OneDriveException {
        try {
            authenticator = Authenticator.class.cast(Class.forName(authenticatorClassName)
                .getDeclaredConstructor(String.class, String.class).newInstance(email, appCredential.getRedirectUrl()));
            String code = authenticator.get(url);
            code = code.substring(code.indexOf("code=") + "code=".length());
            client.authenticate(code);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    private void writeRefreshToken() {
        try {
//Debug.println("here");
            String oldRefreshToken = readRefreshToken();
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            String refreshToken = client.getRefreshToken();
            if (oldRefreshToken == null || !oldRefreshToken.equals(refreshToken)) {
                FileWriter writer = new FileWriter(file);
Debug.println("refreshToken: " + refreshToken);
                writer.write("onedrive.refreshToken=" + refreshToken);
                writer.close();
            }
        } catch (Exception e) {
e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /** */
    private String readRefreshToken() throws IOException {
        String refreshToken = null;
        if (file.exists()) {
            FileReader reader = new FileReader(file);
            Properties props = new Properties();
            props.load(reader);
            refreshToken = props.getProperty("onedrive.refreshToken");
            reader.close();
        }
        return refreshToken;
    }
}
