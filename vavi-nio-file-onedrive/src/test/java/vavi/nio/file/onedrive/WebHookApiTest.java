/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.IOException;
import java.net.URI;

import vavi.net.auth.oauth2.microsoft.MicrosoftLocalAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftOAuth2;
import vavi.net.auth.web.microsoft.MicrosoftLocalUserCredential;
import vavi.util.Debug;

import static vavi.net.auth.oauth2.OAuth2AppCredential.wrap;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveFactory;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.OneDriveScope;
import de.tuberlin.onedrivesdk.common.OneItem;
import de.tuberlin.onedrivesdk.common.Subscription;
import de.tuberlin.onedrivesdk.folder.OneFolder;
import de.tuberlin.onedrivesdk.networking.OneDriveAuthenticationException;


/**
 * WebHookApiTest. onedrive
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/03 umjammer initial version <br>
 * @see "https://app.box.com/developers/console/app/216798/webhooks"
 * @see "https://developer.box.com/guides/webhooks/"
 */
public class WebHookApiTest {

    static final String VAVI_APPS_WEBHOOK_SECRET = System.getenv("VAVI_APPS_WEBHOOK_SECRET");
    static String webhooktUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBHOOK_ONEDRIVE_URL");
    static String websocketBaseUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_BASE_URL");
    static String websocketPath = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_ONEDRIVE_PATH");
    static String email = System.getenv("MICROSOFT_TEST_ACCOUNT");

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        MicrosoftLocalUserCredential userCredential = new MicrosoftLocalUserCredential(email);
        MicrosoftLocalAppCredential appCredential = new MicrosoftLocalAppCredential();

        OneDriveSDK client = null;
        try {
            client = OneDriveFactory.createOneDriveSDK(appCredential.getClientId(),
                                                       appCredential.getClientSecret(),
                                                       appCredential.getRedirectUrl(),
                                                       OneDriveScope.OFFLINE_ACCESS); // TODO out source
            String url = client.getAuthenticationURL();

            MicrosoftOAuth2 oauth2 = new MicrosoftOAuth2(wrap(appCredential, url), userCredential.getId());
            String code = null;
            String refreshToken = oauth2.readRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                code = oauth2.authorize(userCredential);
                client.authenticate(code);
            } else {
                try {
                    client.authenticateWithRefreshToken(refreshToken);
                } catch (OneDriveAuthenticationException e) {
Debug.println("refreshToken: timeout?");
                    code = oauth2.authorize(userCredential);
                    client.authenticate(code);
                }
            }

        } catch (OneDriveException e) {
            throw new IOException(e);
        }

        // create
        URI uri = URI.create(webhooktUrl);
        // Listen for file upload events in the specified folder
        OneFolder rootFolder = client.getRootFolder();
        for (OneItem i : rootFolder.getChildren()) {
            if (i.getName().equals("TEST_WEBHOOK")) {
System.out.println("rmdir " + i.getName());
                i.delete();
            }
        }
System.out.println("mkdir " + "TEST_WEBHOOK");
        OneFolder newFolder = rootFolder.createFolder("TEST_WEBHOOK");
        // cannot set to root folder!
System.out.println("[create] webhook");
        Subscription subscription = client.subscribe(uri.toURL().toString(), VAVI_APPS_WEBHOOK_SECRET);
Debug.println(subscription.getId());

System.out.println("mkdir " + "TEST_WEBHOOK/" + "NEW FOLDER");
        OneFolder subFolder = newFolder.createFolder("NEW FOLDER");

        Thread.sleep(5000);

        // update
System.out.println("[update] webhook");
        subscription = subscription.update();

        // delete
System.out.println("[delete] webhook");
        subscription.delete();

System.out.println("rmdir " + subFolder.getName());
        subFolder.delete();
System.out.println("rmdir " + newFolder.getName());
        newFolder.delete();
    }
}

/* */
