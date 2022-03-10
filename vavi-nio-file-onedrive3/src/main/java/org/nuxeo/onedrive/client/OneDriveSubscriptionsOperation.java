/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.nuxeo.onedrive.client;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.eclipsesource.json.JsonObject;


/**
 * OneDriveSubscriptionsOperation.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/05 umjammer initial version <br>
 */
public class OneDriveSubscriptionsOperation {
    private final JsonObject jsonObject = new JsonObject();

    public void subscribe(String notificationUrl, String clientState) {
        jsonObject.add("changeType", "updated");
        jsonObject.add("notificationUrl", notificationUrl);
        jsonObject.add("resource", "me/drive/root");
        jsonObject.add("expirationDateTime", getExpireTime());
        jsonObject.add("clientState", clientState);
    }

    JsonObject build() {
        return jsonObject;
    }

    static String getExpireTime() {
        long time = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(new Date(time));
    }
}
