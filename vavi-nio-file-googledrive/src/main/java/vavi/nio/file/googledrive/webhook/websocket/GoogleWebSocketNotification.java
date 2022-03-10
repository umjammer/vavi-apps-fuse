/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive.webhook.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

import javax.websocket.ClientEndpoint;
import javax.websocket.Session;

import com.google.api.client.googleapis.notifications.UnparsedNotification;

import vavi.nio.file.googledrive.webhook.websocket.GoogleJsonCodec.GoogleJsonDecoder;
import vavi.nio.file.googledrive.webhook.websocket.GoogleJsonCodec.GoogleJsonEncoder;
import vavi.nio.file.watch.webhook.websocket.BasicAuthorizationConfigurator;
import vavi.nio.file.watch.webhook.websocket.WebSocketNotification;


/**
 * GoogleWebSocketNotification.
 * <p>
 * environment variables
 * <ul>
 * <li> VAVI_APPS_WEBHOOK_WEBSOCKET_BASE_URL
 * <li> VAVI_APPS_WEBHOOK_WEBSOCKET_GOOGLE_PATH
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/06 umjammer initial version <br>
 */
@ClientEndpoint(decoders = GoogleJsonDecoder.class,
                encoders = GoogleJsonEncoder.class,
                configurator = BasicAuthorizationConfigurator.class)
public class GoogleWebSocketNotification extends WebSocketNotification<UnparsedNotification> {

    private static final String websocketBaseUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_BASE_URL");
    private static final String websocketPath = System.getenv("VAVI_APPS_WEBHOOK_WEBSOCKET_GOOGLE_PATH");

    private static final URI uri = URI.create(websocketBaseUrl + websocketPath);

    private Consumer<UnparsedNotification> callback;

    /**
     * @param args 0: uuid for channel id
     */
    public GoogleWebSocketNotification(Consumer<UnparsedNotification> callback, Object... args) throws IOException {
        super(uri, args);
        this.callback = callback;
    }

    @Override
    public void onOpenImpl(Session session) throws IOException {
        session.getBasicRemote().sendText(String.join(" ", "GOOGLE_DRIVE_CHANGE", args[0].toString()));
    }

    @Override
    protected void onNotifyMessageImpl(UnparsedNotification notification) throws IOException {
        callback.accept(notification);
    }

    @Override
    protected void onCloseImpl(Session session) throws IOException {
    }
}

/* */
