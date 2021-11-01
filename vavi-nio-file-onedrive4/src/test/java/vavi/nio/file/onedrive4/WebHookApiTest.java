/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.net.URI;
import java.util.Calendar;

import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.Folder;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.Subscription;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IDriveItemCollectionPage;
import com.microsoft.graph.requests.extensions.ISubscriptionCollectionPage;

import vavi.net.auth.oauth2.microsoft.MicrosoftGraphLocalAppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftGraphOAuth2;
import vavi.net.auth.web.microsoft.MicrosoftLocalUserCredential;
import vavi.nio.file.onedrive4.graph.MyLogger;
import vavi.util.Debug;


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
    static String email = System.getenv("MICROSOFT4_TEST_ACCOUNT");

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        MicrosoftLocalUserCredential userCredential = new MicrosoftLocalUserCredential(email);
        MicrosoftGraphLocalAppCredential appCredential = new MicrosoftGraphLocalAppCredential();

        @SuppressWarnings("resource")
		String accessToken = new MicrosoftGraphOAuth2(appCredential, true).authorize(userCredential);
//Debug.println("accessToken: " + accessToken);

        IAuthenticationProvider authenticationProvider = new IAuthenticationProvider() {
            @Override
            public void authenticateRequest(IHttpRequest request) {
                request.addHeader("Authorization", "Bearer " + accessToken);
            }
        };
        IGraphServiceClient graphClient = GraphServiceClient.builder()
            .authenticationProvider(authenticationProvider)
            .logger(new MyLogger())
            .buildClient();
        graphClient.getHttpProvider().getConnectionConfig().setReadTimeout(30 * 1000);

        // create
        URI uri = URI.create(websocketBaseUrl + websocketPath);
        // Listen for file upload events in the specified folder
        DriveItem rootFolder = graphClient.drive().root().buildRequest().get();
        IDriveItemCollectionPage pages = graphClient.drive().items(rootFolder.id).children().buildRequest().get();
        for (DriveItem i : pages.getCurrentPage()) {
            if (i.name.equals("TEST_WEBHOOK")) {
System.out.println("rmdir " + i.name);
                graphClient.drive().items(i.id).buildRequest().delete();
            }
        }
System.out.println("mkdir " + "TEST_WEBHOOK");
        DriveItem preEntry = new DriveItem();
        preEntry.name = "TEST_WEBHOOK";
        preEntry.folder = new Folder();
        DriveItem newFolder = graphClient.drive().items(rootFolder.id).children().buildRequest().post(preEntry);

System.out.println("[create] webhook");
        Subscription preSubscription = new Subscription();
//        preSubscription.id = UUID.randomUUID().toString(); // doesn't work (spec.)
//        preSubscription.changeType = "created,updated,deleted"; // root is only supported 'update'
        preSubscription.changeType = "updated";
        preSubscription.notificationUrl = uri.toString();
        preSubscription.resource = "me/drive/root";
        Calendar calendar = Calendar.getInstance();
        calendar.roll(Calendar.DATE, true);
        preSubscription.expirationDateTime = calendar;
        preSubscription.clientState = VAVI_APPS_WEBHOOK_SECRET;
//        Subscription subscription = graphClient.subscriptions().buildRequest().post(preSubscription);
        Subscription subscription = graphClient.subscriptions().buildRequest().post(preSubscription);
Debug.println(subscription.id);

System.out.println("[ls] webhook");
        ISubscriptionCollectionPage subscPages = graphClient.subscriptions().buildRequest().get();
        for (Subscription s : subscPages.getCurrentPage()) {
System.out.println(s);
        }

System.out.println("mkdir " + "TEST_WEBHOOK/" + "NEW FOLDER");
        preEntry = new DriveItem();
        preEntry.name = "NEW FOLDER";
        preEntry.folder = new Folder();
        DriveItem subFolder = graphClient.drive().items(newFolder.id).children().buildRequest().post(preEntry);

        Thread.sleep(5000);

        // update
System.out.println("[update] webhook");
        calendar.roll(Calendar.DATE, true);
        subscription.expirationDateTime = calendar;
        subscription = graphClient.subscriptions(subscription.id).buildRequest().patch(subscription);

        // delete
System.out.println("[delete] webhook");
        graphClient.subscriptions(subscription.id).buildRequest().delete();

System.out.println("rmdir " + subFolder.name);
        graphClient.drive().items(subFolder.id).buildRequest().delete();
System.out.println("rmdir " + newFolder.name);
        graphClient.drive().items(newFolder.id).buildRequest().delete();
    }
}

/* */
