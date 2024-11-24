/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.nuxeo.onedrive.client.OneDriveAPI;

import vavi.nio.file.watch.webhook.WebHookBaseWatchService;

import static java.lang.System.getLogger;


/**
 * OneDriveWatchService.
 * <p>
 * system properties
 * <ul>
 * <li> vavi.nio.file.watch.webhook.NotificationProvider.onedrive3
 * </ul>
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/23 umjammer initial version <br>
 */
public class OneDriveWatchService extends WebHookBaseWatchService<String> {

    private static final Logger logger = getLogger(OneDriveWatchService.class.getName());

    private static final String WEBHOOK_NOTIFICATION_PROVIDER =
            System.getProperty("vavi.nio.file.watch.webhook.NotificationProvider.onedrive3", ".onedrive3.webhook.websocket");

//    private OneDriveAPI client;

//    private String savedStartPageToken;

    /** */
    public OneDriveWatchService(OneDriveAPI client) throws IOException {
//        this.client = client;

        setupNotification(this, WEBHOOK_NOTIFICATION_PROVIDER);
    }

    @Override
    protected void onNotifyMessage(String notification) throws IOException {
logger.log(Level.DEBUG, ">> notification: done");
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            super.close();
        }
    }
}
