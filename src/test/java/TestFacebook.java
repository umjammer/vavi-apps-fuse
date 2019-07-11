/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;

import vavi.net.auth.oauth2.facebook.FacebookAuthenticator;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * TestFacebook.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/08/08 umjammer initial version <br>
 */
@PropsEntity(url = "file://${HOME}/.vavi_apps_fb.properties")
public class TestFacebook {

    @Property(name = "{0}.id")
    private String clientId;
    @Property(name = "{0}.secret")
    private String clientSecret;
    @Property(name = "{0}.token")
    private String accessToken;
    private String group;

    /**
     * @param args group
     */
    public static void main(String[] args) throws Exception {
        TestFacebook app = new TestFacebook();
        app.group = args[0];
        PropsEntity.Util.bind(app, app.group);
        app.process();
    }

    void process() throws IOException {
        String url = "https://www.facebook.com/dialog/oauth?client_id=%s&redirect_uri=%s&response_type=token";
        String redirectUrl = "https://www.facebook.com/connect/login_success.html";
        String token = new FacebookAuthenticator("ns777@104.net", clientId, redirectUrl).authorize(url);
        System.err.println("token: " + token);
    }
}

/* */
