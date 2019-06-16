/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.oauth2.microsoft;

import java.io.IOException;
import java.net.URL;

import vavi.net.auth.oauth2.AuthUI;
import vavi.net.auth.oauth2.Authenticator;
import vavi.net.http.HttpServer;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * OneDriveAuthenticator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/16 umjammer initial version <br>
 */
@PropsEntity(url = "file://${HOME}/.vavifuse/credentials.properties")
public class OneDriveAuthenticator implements Authenticator {

    /** */
    private final String email;
    @Property(name = "onedrive.password.{0}")
    private transient String password;
    /** */
    private final String redirectUrl;

    /** */
    public OneDriveAuthenticator(String email, String redirectUrl) throws IOException {
        this.email = email;
        this.redirectUrl = redirectUrl;

        PropsEntity.Util.bind(this, email);
    }

    /* @see Authenticator#get(java.lang.String) */
    @Override
    public String get(String url) throws IOException {

        URL redirectUrl = new URL(this.redirectUrl);
        String host = redirectUrl.getHost();
        int port = redirectUrl.getPort();
        HttpServer httpServer = new HttpServer(host, port);
        httpServer.start();

        AuthUI<String> ui = new JavaFxAuthUI(email, password, url, this.redirectUrl);
        ui.auth();

        httpServer.stop();

        if (ui.getException() != null) {
            throw new IllegalStateException(ui.getException());
        }

        return ui.getResult();
    }
}

/* */
