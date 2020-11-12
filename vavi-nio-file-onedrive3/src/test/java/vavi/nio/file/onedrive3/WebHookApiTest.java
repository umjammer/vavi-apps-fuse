/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Set;

import org.nuxeo.onedrive.client.JavaNetRequestExecutor;
import org.nuxeo.onedrive.client.OneDriveAPI;
import org.nuxeo.onedrive.client.OneDriveBasicAPI;
import org.nuxeo.onedrive.client.OneDriveDrive;
import org.nuxeo.onedrive.client.OneDriveFolder;
import org.nuxeo.onedrive.client.OneDriveItem;
import org.nuxeo.onedrive.client.RequestExecutor;
import org.nuxeo.onedrive.client.RequestHeader;

import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphOAuth2;
import vavi.net.auth.web.microsoft.MicrosoftLocalUserCredential;


/**
 * WebHookApiTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/03 umjammer initial version <br>
 * @see "https://docs.microsoft.com/graph/api/subscription-post-subscriptions?view=graph-rest-1.0&tabs=http"
 */
public class WebHookApiTest {

    static final String VAVI_APPS_WEBHOOK_SECRET = System.getenv("VAVI_APPS_WEBHOOK_SECRET");
    static String websocketBaseUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_BASE_URL");
    static String websocketPath = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_MICROSOFT_PATH");

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String email = System.getenv("MICROSOFT3_TEST_ACCOUNT");

        MicrosoftLocalUserCredential userCredential = new MicrosoftLocalUserCredential(email);
        MicrosoftGraphLocalAppCredential appCredential = new MicrosoftGraphLocalAppCredential();

        String accessToken = new MicrosoftGraphOAuth2(appCredential, true).authorize(userCredential);
//Debug.println("accessToken: " + accessToken);

        RequestExecutor executor = new JavaNetRequestExecutor(accessToken) {
            @Override
            public void addAuthorizationHeader(final Set<RequestHeader> headers) {
                super.addAuthorizationHeader(headers);
                // HttpURLConnection adds "accept" header which is unavailable to onedrive.
                headers.add(new RequestHeader("Accept", "application/json"));
            }

            @Override
            public Upload doPatch(URL url, Set<RequestHeader> headers) throws IOException {
                headers.add(new RequestHeader("X-HTTP-Method-Override", "PATCH"));
                headers.add(new RequestHeader("X-HTTP-Method", "PATCH"));
                return super.doPost(url, headers);
            }
        };

        OneDriveAPI client = new OneDriveBasicAPI(executor) {
            @Override
            public RequestExecutor getExecutor() {
                return executor;
            }

            @Override
            public boolean isBusinessConnection() {
                return false;
            }

            @Override
            public boolean isGraphConnection() {
                return true;
            }

            @Override
            public String getBaseURL() {
                return String.format("https://graph.microsoft.com%s", "/v1.0");
            }

            @Override
            public String getEmailURL() {
                return String.format("https://graph.microsoft.com%s", "/v1.0/me");
            }
        };

        OneDriveDrive drive = OneDriveDrive.getDefaultDrive(client);

        // create
        URI uri = URI.create(websocketBaseUrl + websocketPath);
        // Listen for file upload events in the specified folder
        OneDriveFolder rootFolder = drive.getRoot();
        for (OneDriveItem.Metadata i : rootFolder.getChildren()) {
            if (i.getName().equals("TEST_WEBHOOK")) {
System.out.println("rmdir " + i.getName());
                i.getResource().delete();
            }
        }
System.out.println("mkdir " + "TEST_WEBHOOK");
        OneDriveFolder newFolder = rootFolder.create("TEST_WEBHOOK").getResource();

System.out.println("[create] webhook");
//        Subscription preSubscription = new Subscription();
////        preSubscription.changeType = "created,updated,deleted"; // root is only supported update
//        preSubscription.changeType = "created,updated,deleted";
//        preSubscription.notificationUrl = uri.toString();
//        preSubscription.resource = "me/drive/root";
//        preSubscription.expirationDateTime = calendar;
//        preSubscription.clientState = VAVI_APPS_WEBHOOK_SECRET;
//        Subscription subscription = graphClient.subscriptions().buildRequest().post(preSubscription);
//Debug.println(subscription.id);

System.out.println("[ls] webhook");
//        for (Subscription s : subscPages.getCurrentPage()) {
//System.out.println(s);
//        }

System.out.println("mkdir " + "TEST_WEBHOOK/" + "NEW FOLDER");
        OneDriveFolder subFolder = newFolder.create("NEW FOLDER").getResource();

        // update
System.out.println("[update] webhook");
//        subscription.expirationDateTime = calendar;
//        subscription = drive.subscriptions(subscription.id).buildRequest().patch(subscription);

        // delete
System.out.println("[delete] webhook");
//        drive.subscriptions(subscription.id).buildRequest().delete();

System.out.println("rmdir " + subFolder.getMetadata().getName());
        subFolder.delete();
System.out.println("rmdir " + newFolder.getMetadata().getName());
        newFolder.delete();
    }
}

/* */
