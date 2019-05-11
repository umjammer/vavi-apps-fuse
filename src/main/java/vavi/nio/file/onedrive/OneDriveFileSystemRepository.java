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
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.fge.filesystem.driver.FileSystemDriver;
import com.github.fge.filesystem.provider.FileSystemRepositoryBase;

import vavi.net.auth.oauth2.Authenticator;
import vavi.net.auth.oauth2.microsoft.OneDriveAuthenticator;
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
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/11 umjammer initial version <br>
 */
@ParametersAreNonnullByDefault
@PropsEntity(url = "file://${user.home}/.vavifuse/onedrive.properties")
public final class OneDriveFileSystemRepository extends FileSystemRepositoryBase {

    @Property(name = "onedrive.clientId")
    private String clientId;
    @Property(name = "onedrive.clientSecret")
    private transient String clientSecret;
    @Property(name = "onedrive.redirectUrl")
    private String redirectUrl;

    public OneDriveFileSystemRepository() {
        super("onedrive", new OneDriveFileSystemFactoryProvider());
    }

    /** */
    private transient OneDriveSDK client;

    /** for refreshToken */
    private File file;

    @Nonnull
    @Override
    public FileSystemDriver createDriver(final URI uri, final Map<String, ?> env) throws IOException {
        try {
            final String email = (String) env.get("email");
            if (email == null)
                throw new IllegalArgumentException("email not found");

            file = new File(System.getProperty("user.home"), ".vavifuse/onedrive/" + email);

            PropsEntity.Util.bind(this, email);

            client = OneDriveFactory.createOneDriveSDK(clientId,
                                                       clientSecret,
                                                       redirectUrl,
                                                       OneDriveScope.OFFLINE_ACCESS);
            String url = client.getAuthenticationURL();

            String refreshToken = readRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                authenticateByBrowser(url, email);
            } else {
                try {
                    client.authenticateWithRefreshToken(refreshToken);
                } catch (OneDriveAuthenticationException e) {
Debug.println("refreshToken: timeout?");
                    authenticateByBrowser(url, email);
                }
            }

            client.startSessionAutoRefresh(this::writeRefreshToken);

            final OneDriveFileStore fileStore = new OneDriveFileStore(client, factoryProvider.getAttributesFactory());
            return new OneDriveFileSystemDriver(fileStore, factoryProvider, client, env);

        } catch (OneDriveException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    private void authenticateByBrowser(String url, String email) throws IOException, OneDriveException {
        Authenticator authenticator = new OneDriveAuthenticator(email, redirectUrl);
        String code = authenticator.get(url);

        client.authenticate(code);
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
            if (!oldRefreshToken.equals(refreshToken)) {
                FileWriter writer = new FileWriter(file);
Debug.println("refreshToken: " + refreshToken);
                writer.write("onedrive.refreshToken=" + refreshToken);
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
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
