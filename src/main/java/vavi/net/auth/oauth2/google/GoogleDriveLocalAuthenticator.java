/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.oauth2.google;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;

import vavi.net.auth.oauth2.AuthUI;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * GoogleDriveLocalAuthenticator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/04 umjammer initial version <br>
 */
@PropsEntity(url = "file://${HOME}/.vavifuse/credentials.properties")
public class GoogleDriveLocalAuthenticator {

    @Property(name = "google.password.{0}")
    private transient String password;
    @Property(name = "google.totpSecret.{0}")
    private String totpSecret;

    private AuthorizationCodeInstalledApp app;

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".vavifuse/googledrive");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    public static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    public static HttpTransport HTTP_TRANSPORT;

    /**
     * Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.vavifuse/googledrive
     */
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @throws IOException
     */
    public GoogleDriveLocalAuthenticator() throws IOException {
        // Load client secrets.
        InputStream in = new FileInputStream(new java.io.File(DATA_STORE_DIR.getParent(), "googledrive.json"));
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        // Build flow.
        app = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()) {
            /** */
            private String userId;

            public Credential authorize(String userId) throws IOException {
                this.userId = userId;

                return super.authorize(userId);
            }

            /* */
            protected void onAuthorization(AuthorizationCodeRequestUrl authorizationUrl) throws IOException {
                PropsEntity.Util.bind(GoogleDriveLocalAuthenticator.this, userId);

                AuthUI<?> ui = new JavaFxAuthUI(userId, password, totpSecret, authorizationUrl.build(), authorizationUrl.getRedirectUri());
                ui.auth();
            }
        };
    }

    public Credential authorize(String email) throws IOException {
        // Trigger user authorization request.
        Credential credential = app.authorize(email);
Debug.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }
}

/* */
