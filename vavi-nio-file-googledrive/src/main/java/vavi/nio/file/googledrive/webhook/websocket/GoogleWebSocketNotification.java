/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive.webhook.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private static final String COMMAND = "GOOGLE_DRIVE_CHANGE";

    /** channel ids to be notified, those are registered to the server again when reconnected */
    private static final Set<String> channelIds = ConcurrentHashMap.newKeySet();

    /** opened sessions */
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    private final Consumer<UnparsedNotification> callback;

    /**
     * registers a channel id to the server.
     * a notification instance is shared by watch services, so use this for channels other than the first one.
     */
    public static void addChannel(String channelId) throws IOException {
        if (channelIds.add(channelId)) {
            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(String.join(" ", COMMAND, channelId));
                }
            }
        }
    }

    /** the server has no unregister command, just forget it locally */
    public static void removeChannel(String channelId) {
        channelIds.remove(channelId);
    }

    /**
     * @param args 0: uuid for channel id
     */
    public GoogleWebSocketNotification(Consumer<UnparsedNotification> callback, Object... args) throws IOException {
        super(uri, args);
        this.callback = callback;
    }

    @Override
    public void onOpenImpl(Session session) throws IOException {
        sessions.add(session);
        channelIds.add(args[0].toString());
        for (String channelId : channelIds) {
            session.getBasicRemote().sendText(String.join(" ", COMMAND, channelId));
        }
    }

    @Override
    protected void onNotifyMessageImpl(UnparsedNotification notification) throws IOException {
        callback.accept(notification);
    }

    @Override
    protected void onCloseImpl(Session session) throws IOException {
        sessions.remove(session);
    }
}
