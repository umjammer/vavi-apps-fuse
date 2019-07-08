/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.oauth2;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.dmfs.httpessentials.client.HttpRequestExecutor;
import org.dmfs.httpessentials.exceptions.ProtocolError;
import org.dmfs.httpessentials.exceptions.ProtocolException;
import org.dmfs.httpessentials.httpurlconnection.HttpUrlConnectionExecutor;
import org.dmfs.jems.optional.Optional;
import org.dmfs.oauth2.client.BasicOAuth2AuthorizationProvider;
import org.dmfs.oauth2.client.BasicOAuth2Client;
import org.dmfs.oauth2.client.BasicOAuth2ClientCredentials;
import org.dmfs.oauth2.client.OAuth2AccessToken;
import org.dmfs.oauth2.client.OAuth2AuthorizationProvider;
import org.dmfs.oauth2.client.OAuth2Client;
import org.dmfs.oauth2.client.OAuth2ClientCredentials;
import org.dmfs.oauth2.client.OAuth2InteractiveGrant;
import org.dmfs.oauth2.client.OAuth2Scope;
import org.dmfs.oauth2.client.grants.AuthorizationCodeGrant;
import org.dmfs.oauth2.client.grants.TokenRefreshGrant;
import org.dmfs.oauth2.client.scope.StringScope;
import org.dmfs.rfc3986.encoding.Precoded;
import org.dmfs.rfc3986.uris.LazyUri;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Duration;

import vavi.util.Debug;


/**
 * OAuth2.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/04 umjammer initial version <br>
 */
public class OAuth2 {

    /** http client for oauth */
    private static HttpRequestExecutor oauthExecutor = new HttpUrlConnectionExecutor();

    /** */
    private OAuth2Client oauth;

    /** */
    private BasicAppCredential appCredential;

    /** should be {@link Authenticator} */
    private String authenticatorClassName;

    /** */
    private boolean startTokenRefresher;

    /** */
    private TokenRefresher refresher;

    /** */
    public OAuth2(BasicAppCredential appCredential, boolean startTokenRefresher, String authenticatorClassName) {
        this.appCredential = appCredential;
        this.startTokenRefresher = startTokenRefresher;
        this.authenticatorClassName = authenticatorClassName;

        // Create OAuth2 provider
        OAuth2AuthorizationProvider provider = new BasicOAuth2AuthorizationProvider(
            URI.create(appCredential.getOAuthAuthorizationUrl()),
            URI.create(appCredential.getOAuthTokenUrl()),
            new Duration(1, 0, 3600) /* default expiration time in case the server doesn't return any */);

        // Create OAuth2 client credentials
        OAuth2ClientCredentials credentials = new BasicOAuth2ClientCredentials(
            appCredential.getClientId(), appCredential.getClientSecret());

        // Create OAuth2 client
        oauth = new BasicOAuth2Client(
            provider,
            credentials,
            new LazyUri(new Precoded(appCredential.getRedirectUrl())));
    }

    /** */
    public String getAccessToken(String id) throws IOException {
        try {
            Path file = Paths.get(System.getProperty("user.home"), ".vavifuse/" + appCredential.getScheme() + "/" + id);
            refresher = new TokenRefresher(file, this::refresh);

            OAuth2AccessToken token;
            OAuth2AccessToken refreshToken = readRefreshToken();
            if (!refreshToken.hasRefreshToken()) {
                OAuth2InteractiveGrant grant = new AuthorizationCodeGrant(oauth, new StringScope(appCredential.getScope()));

                // Get the authorization URL and open it in a WebView
                URI authorizationUrl = grant.authorizationUrl();

                // Open the URL in a WebView and wait for the redirect to the redirect URL
                // After the redirect, feed the URL to the grant to retrieve the access token
                String code = authenticate(id, authorizationUrl.toString());
                token = grant.withRedirect(new LazyUri(new Precoded(code))).accessToken(oauthExecutor);
Debug.println("scope: " + token.scope());

            } else {
                token = new TokenRefreshGrant(oauth, refreshToken).accessToken(oauthExecutor);
Debug.println("no refreshToken: timeout? or firsttime");
            }

            if (startTokenRefresher) {
                refresher.start(token.refreshToken().toString(), token.expirationDate().getTimestamp() - System.currentTimeMillis());
            }

            return token.accessToken().toString();
        } catch (ProtocolError | ProtocolException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    private String authenticate(String id, String authorizationUrl) throws IOException {
        try {
            Authenticator authenticator = Authenticator.class.cast(Class.forName(authenticatorClassName)
                .getDeclaredConstructor(String.class, String.class).newInstance(id, appCredential.getRedirectUrl()));
            return authenticator.get(authorizationUrl);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    };

    /** */
    private long refresh() {
        try {
            OAuth2AccessToken token = new TokenRefreshGrant(oauth, readRefreshToken()).accessToken(oauthExecutor);
            refresher.writeRefreshToken(token.refreshToken().toString());
            return token.expirationDate().getTimestamp();
        } catch (ProtocolError | ProtocolException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    private OAuth2AccessToken readRefreshToken() throws IOException {
        String refreshToken = refresher.readRefreshToken();
        return new OAuth2AccessToken() {
            public CharSequence accessToken() throws ProtocolException {
                throw new UnsupportedOperationException("accessToken not present");
            }
            @Override
            public CharSequence tokenType() throws ProtocolException {
                throw new UnsupportedOperationException("tokenType not present");
            }
            @Override
            public boolean hasRefreshToken() {
               return refreshToken != null;
            }
            @Override
            public CharSequence refreshToken() throws ProtocolException {
                return refreshToken;
            }
            @Override
            public DateTime expirationDate() throws ProtocolException {
                throw new UnsupportedOperationException("expirationDate not present");
            }
            @Override
            public OAuth2Scope scope() throws ProtocolException {
                return new StringScope(appCredential.getScope());
            }
            @Override
            public Optional<CharSequence> extraParameter(String parameterName) {
                return null;
            }
        };
    }

    /** */
    protected void finalize() {
        refresher.terminate();
    }
}

/* */
