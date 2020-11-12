/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.google.api.client.googleapis.notifications.UnparsedNotification;

import vavi.nio.file.googledrive.WebHookTest.AuthorizationConfigurator;
import vavi.nio.file.googledrive.webhook.websocket.GoogleJsonCodec.GoogleJsonDecoder;
import vavi.nio.file.googledrive.webhook.websocket.GoogleJsonCodec.GoogleJsonEncoder;
import vavi.util.Debug;


/**
 * WebSocketTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/01 umjammer initial version <br>
 */
public class WebSocketTest {

    static String websocketBaseUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_BASE_URL");
    static String websocketPath = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_GOOGLE_PATH");

    static CountDownLatch cdl = new CountDownLatch(1);

    @ClientEndpoint(decoders = GoogleJsonDecoder.class,
                    encoders = GoogleJsonEncoder.class,
                    configurator = AuthorizationConfigurator.class)
    public static class NotificationClient {

        @OnOpen
        public void onOpen(Session session) {
Debug.println("OPEN: " + session);
        }

        @OnMessage
        public void onMessage(UnparsedNotification notification) {
Debug.println(notification);
        }

        @OnError
        public void onError(Throwable t) {
t.printStackTrace();
        }

        @OnClose
        public void onClose(Session session) {
Debug.println("CLOSE");
            cdl.countDown();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
//        URI uri = URI.create("ws://localhost:5000" + websocketPath);
        URI uri = URI.create(websocketBaseUrl + websocketPath);
        Session session = container.connectToServer(new NotificationClient(), uri);

Debug.println("Stop");
        cdl.await();

        session.close();
Debug.println("Done");

// TODO https://stackoverflow.com/a/46472909/6102938
if(container != null && container instanceof org.eclipse.jetty.util.component.LifeCycle) { 
 try {
Debug.println("Stopping Jetty's WebSocket Client");
  ((org.eclipse.jetty.util.component.LifeCycle) container).stop();
 } catch (Exception e) {
  throw new IllegalStateException(e);
 }
}
    }
}

/* */
