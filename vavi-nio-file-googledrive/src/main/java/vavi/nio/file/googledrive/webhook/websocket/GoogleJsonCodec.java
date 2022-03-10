/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive.webhook.websocket;

import com.google.api.client.googleapis.notifications.UnparsedNotification;
import com.google.gson.Gson;

import vavi.nio.file.watch.webhook.websocket.JsonCodec;


/**
 * GoogleJsonCodec.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/02 umjammer initial version <br>
 */
public class GoogleJsonCodec extends JsonCodec {

    public static Gson gsonForGoogleJsonCodec = gson;

    public static class GoogleJsonEncoder extends JsonEncoder<UnparsedNotification> {
    }

    public static class GoogleJsonDecoder extends JsonDecoder<UnparsedNotification> {
        @Override
        protected Class<UnparsedNotification> getType() {
            return UnparsedNotification.class;
        }
    }
}

/* */
