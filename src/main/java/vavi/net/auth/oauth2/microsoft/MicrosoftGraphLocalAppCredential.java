/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.oauth2.microsoft;

import vavi.net.auth.oauth2.BasicAppCredential;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * MicrosoftGraphLocalAppCredential.
 *
 * properties file "onedrive.properties"
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/19 umjammer initial version <br>
 */
@PropsEntity(url = "file://${user.home}/.vavifuse/onedrive.properties")
public class MicrosoftGraphLocalAppCredential implements BasicAppCredential {

    @Property(name = "microsoft.graph.clientId")
    private String clientId;
    @Property(name = "microsoft.graph.clientSecret")
    private transient String clientSecret;
    @Property(name = "microsoft.graph.redirectUrl")
    private String redirectUrl;

    @Override
    public String getScheme() {
        return "msgraph";
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    @Override
    public String getRedirectUrl() {
        return redirectUrl;
    }

    @Override
    public String getOAuthAuthorizationUrl() {
        return "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    }

    @Override
    public String getOAuthTokenUrl() {
        return "https://login.microsoftonline.com/common/oauth2/v2.0/token";
    }

    @Override
    public String getScope() {
        return "Files.ReadWrite.All Sites.ReadWrite.All User.Read offline_access";
    }
}

/* */
