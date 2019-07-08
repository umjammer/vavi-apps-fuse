/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.test.googledrive;

import java.io.IOException;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;

import vavi.net.auth.oauth2.AuthUI;
import vavi.net.auth.oauth2.google.JavaFxAuthUI;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * AuthorizationCodeInstalledApp.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/04 umjammer initial version <br>
 */
@PropsEntity(url = "file://${HOME}/.vavifuse/credentials.properties")
public class AuthorizationCodeInstalledApp
        extends com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp {

    /**
     * @throws IOException
     */
    public AuthorizationCodeInstalledApp(GoogleAuthorizationCodeFlow flow, VerificationCodeReceiver receiver) throws IOException {
        super(flow, new LocalServerReceiver());
    }

    /** */
    private String userId;
    @Property(name = "googledrive.password.{0}")
    private transient String password;
    @Property(name = "googledrive.totpSecret.{0}")
    private String totpSecret;

    public Credential authorize(String userId) throws IOException {
        this.userId = userId;

        return super.authorize(userId);
    }

    /* */
    protected void onAuthorization(AuthorizationCodeRequestUrl authorizationUrl) throws IOException {
        PropsEntity.Util.bind(this, userId);

        AuthUI<?> ui = new JavaFxAuthUI(userId, password, totpSecret, authorizationUrl.build(), authorizationUrl.getRedirectUri());
        ui.auth();
    }
}

/* */
