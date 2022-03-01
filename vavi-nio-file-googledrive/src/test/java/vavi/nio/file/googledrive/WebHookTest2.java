/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.notifications.UnparsedNotification;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.drive.model.StartPageToken;

import vavi.net.auth.WithTotpUserCredential;
import vavi.net.auth.oauth2.google.GoogleOAuth2AppCredential;
import vavi.net.auth.oauth2.google.GoogleLocalOAuth2AppCredential;
import vavi.net.auth.oauth2.google.GoogleOAuth2;
import vavi.net.auth.web.google.GoogleLocalUserCredential;
import vavi.nio.file.watch.webhook.Notification;
import vavi.util.Debug;


/**
 * WebHookTest2. google drive, using now construction libraries.
 *
 * @depends "file://${HOME}.vavifuse/googledrive/?"
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
public class WebHookTest2 {

    static final String VAVI_APPS_WEBHOOK_SECRET = System.getenv("VAVI_APPS_WEBHOOK_SECRET");
    static String webhooktUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBHOOK_GOOGLE_URL");

    static CountDownLatch countDownLatch = new CountDownLatch(1);

    static class Service {
        Drive driveService;
        String savedStartPageToken;
        Channel channel = null;
        Notification<UnparsedNotification> notification;

        Service() throws IOException {
            WithTotpUserCredential userCredential = new GoogleLocalUserCredential(email);
            GoogleOAuth2AppCredential appCredential = new GoogleLocalOAuth2AppCredential("googledrive");

            Credential credential = new GoogleOAuth2(appCredential).authorize(userCredential);
            driveService = new Drive.Builder(GoogleOAuth2.getHttpTransport(), GoogleOAuth2.getJsonFactory(), credential)
                    .setHttpRequestInitializer(new HttpRequestInitializer() {
                        @Override
                        public void initialize(HttpRequest httpRequest) throws IOException {
                            credential.initialize(httpRequest);
                            httpRequest.setConnectTimeout(30 * 1000);
                            httpRequest.setReadTimeout(30 * 1000);
                        }
                    })
                    .setApplicationName(appCredential.getClientId())
                    .build();
        }

        void start() throws IOException {
            UUID uuid = UUID.randomUUID();

            notification = Notification.getNotification("googledrive.webhook.websocket", notification -> {
                try {
                    process(notification);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }, uuid);
Debug.println("notification: " + notification);

            StartPageToken response = driveService.changes().getStartPageToken().execute();
            savedStartPageToken = response.getStartPageToken();
Debug.println("Start token: " + savedStartPageToken);

            Channel content = new Channel()
                    .setId(uuid.toString())
                    .setType("web_hook")
                    .setToken(VAVI_APPS_WEBHOOK_SECRET)
                    .setAddress(webhooktUrl);

            channel = driveService.changes().watch(savedStartPageToken, content).execute();
Debug.println("channel: " + channel);
        }

        void stop() throws IOException {
            if (channel != null) {
                driveService.channels().stop(channel).execute();
Debug.println("channel deleted: " + channel);
                channel = null;
            }
            notification.close();
        }

        private void process(UnparsedNotification notification) throws IOException {
Debug.println(">> notification: " + notification.getResourceState());
            if (notification.getResourceState().equals("sync")) {
                countDownLatch.countDown();
Debug.println(">> synched");
            } else {
                // Begin with our last saved start token for this user or the
                // current token from getStartPageToken()
                String pageToken = savedStartPageToken;
                while (pageToken != null) {
                    ChangeList changes = driveService.changes().list(pageToken).execute();
                    for (Change change : changes.getChanges()) {
                        // Process change
Debug.println(">> change: " + change);
                    }
                    if (changes.getNewStartPageToken() != null) {
                        // Last page, save this token for the next polling interval
                        savedStartPageToken = changes.getNewStartPageToken();
                    }
                    pageToken = changes.getNextPageToken();
                }
            }
        }
    }

    static String email = System.getenv("GOOGLE_TEST_ACCOUNT");

    /**
     * @param args 0: email
     *
     * https://developers.google.com/drive/api/v3/reference/changes/watch
     * https://stackoverflow.com/a/43793313/6102938
     *  TODO needs domain authorize
     */
    public static void main(String[] args) throws Exception {
        WebHookTest2 app = new WebHookTest2();
        app.test();
    }

    void test() throws Exception {
Debug.println("Start");
        Service service = new Service();
        try {
            service.start();

            URI uri = URI.create("googledrive:///?id=" + email);
            FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

            countDownLatch.await();

            Path tmpDir = fs.getPath("tmp");
            if (!Files.exists(tmpDir)) {
System.out.println("rmdir " + tmpDir);
                Files.createDirectory(tmpDir);
            }
            Path remote = tmpDir.resolve("Test+Watch");
            if (Files.exists(remote)) {
System.out.println("rm " + remote);
                Files.delete(remote);
            }
            Path source = Paths.get("src/test/resources", "Hello.java");
System.out.println("cp " + source + " " + remote);
            Files.copy(source, remote);

System.out.println("rm " + remote);
            Files.delete(remote);

            Thread.sleep(10000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            service.stop();
        }
Debug.println("Done");
//Thread.getAllStackTraces().keySet().forEach(System.err::println);
    }
}
