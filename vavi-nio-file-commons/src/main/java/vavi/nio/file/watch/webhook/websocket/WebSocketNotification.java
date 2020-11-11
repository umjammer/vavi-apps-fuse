/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.watch.webhook.websocket;

import java.io.IOException;
import java.net.URI;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import vavi.util.Debug;


/**
 * WebSocketNotification.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/06 umjammer initial version <br>
 */
public abstract class WebSocketNotification<T> extends BaseWebSocketNotification<T> {

    /** */
    protected WebSocketNotification(URI uri, Object... args) throws IOException {
        super(uri, args);
    }

    @OnOpen
    public final void onOpen(Session session) throws IOException {
Debug.println("WEBSOCKET: onOpen: " + session.getId());
        onOpenImpl(session);
    }

    @OnMessage
    public final void onNotifyMessage(T notification, Session session) throws IOException {
        onNotifyMessageImpl(notification);
    }

    @OnClose
    public final void onClose(Session session) throws IOException {
        onCloseInternal(session);
    }
}

/* */
