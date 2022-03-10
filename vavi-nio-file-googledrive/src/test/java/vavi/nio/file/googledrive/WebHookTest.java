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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.websocket.ClientEndpoint;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

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
import vavi.nio.file.googledrive.webhook.websocket.GoogleJsonCodec.GoogleJsonDecoder;
import vavi.nio.file.googledrive.webhook.websocket.GoogleJsonCodec.GoogleJsonEncoder;
import vavi.util.Debug;


/**
 * WebHookTest. google drive
 *
 * @depends "file://${HOME}.vavifuse/googledrive/?"
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/29 umjammer initial version <br>
 */
public class WebHookTest {

    static final String VAVI_APPS_WEBHOOK_SECRET = System.getenv("VAVI_APPS_WEBHOOK_SECRET");

    @ClientEndpoint(decoders = GoogleJsonDecoder.class,
                    encoders = GoogleJsonEncoder.class,
                    configurator = AuthorizationConfigurator.class)
    public static class NotificationClient {
        Service service;
        NotificationClient(Service service) {
            this.service = service;
        }
        @OnOpen
        public void onOpen(Session session) throws IOException {
Debug.println("OPEN: " + session);
            session.getBasicRemote().sendText(String.join(" ", "GOOGLE_DRIVE_CHANGE", service.uuid.toString()));
        }

        @OnMessage
        public void onMessage(UnparsedNotification notification) throws IOException {
Debug.println(notification);
            service.process(notification);
        }

        @OnError
        public void onError(Throwable t) {
t.printStackTrace();
        }

        @OnClose
        public void onClose(Session session) throws IOException {
Debug.println("CLOSE");
            service.stop();
        }
    }

    static String websocketBaseUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_BASE_URL");
    static String websocketPath = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_GOOGLE_PATH");
    static String webhooktUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBHOOK_GOOGLE_URL");
    static String username = System.getenv("VAVI_APPS_WEBHOOK_USERNAME");
    static String password = System.getenv("VAVI_APPS_WEBHOOK_PASSWORD");

    public static class AuthorizationConfigurator extends ClientEndpointConfig.Configurator {
        @Override
        public void beforeRequest(Map<String, List<String>> headers) {
            headers.put("Authorization", Arrays.asList("Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes())));
        }
    };

    static class Service {
        Drive driveService;
        String savedStartPageToken;
        Channel channel = null;
        Session session;
        UUID uuid;

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
            uuid = UUID.randomUUID();

            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                URI uri = URI.create(websocketBaseUrl + websocketPath);
                // TODO using client ssl is so annoying
                // https://github.com/eclipse/jetty.project/issues/155
//                URI uri = URI.create(String.format("ws://localhost:5000" + websocketPath));
                session = container.connectToServer(new NotificationClient(this), uri);
//                session.setMaxIdleTimeout(0);
            } catch (DeploymentException e) {
                throw new IOException(e);
            }

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

            if (session != null) {
                session.close();
                session = null;
            }
        }

        void process(UnparsedNotification notification) throws IOException {
Debug.println("notification: " + notification.getResourceState());
            // Begin with our last saved start token for this user or the
            // current token from getStartPageToken()
            String pageToken = savedStartPageToken;
            while (pageToken != null) {
                ChangeList changes = driveService.changes().list(pageToken).execute();
                for (Change change : changes.getChanges()) {
                    // Process change
Debug.println("Change found for file: " + change);
                }
                if (changes.getNewStartPageToken() != null) {
                    // Last page, save this token for the next polling interval
                    savedStartPageToken = changes.getNewStartPageToken();
                }
                pageToken = changes.getNextPageToken();
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
        WebHookTest app = new WebHookTest();
        app.test();
    }

    void test() throws Exception {
Debug.println("Start");
        Service service = new Service();
        try {
            service.start();

            URI uri = URI.create("googledrive:///?id=" + email);
            FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

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

            Thread.sleep(5000);
        } finally {
            service.stop();
        }
Debug.println("Done");
    }
}
