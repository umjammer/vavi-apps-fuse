/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.api.client.googleapis.notifications.UnparsedNotification;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.StartPageToken;

import vavi.nio.file.googledrive.webhook.websocket.GoogleWebSocketNotification;
import vavi.nio.file.watch.webhook.WebHookBaseWatchService;

import static java.lang.System.getLogger;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;


/**
 * GoogleDriveWatchService.
 * <p>
 * notification source is {@link #channel}.
 * <p>
 * system properties
 * <ul>
 * <li> vavi.nio.file.watch.webhook.NotificationProvider.googledrive
 * </ul>
 * </p>
 * <p>
 * environment variables
 * <ul>
 * <li> VAVI_APPS_WEBHOOK_SECRET
 * <li> VAVI_APPS_WEBHOOK_WEBHOOK_GOOGLE_URL
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/06 umjammer initial version <br>
 */
public class GoogleDriveWatchService extends WebHookBaseWatchService<UnparsedNotification> {

    private static final Logger logger = getLogger(GoogleDriveWatchService.class.getName());

    private static final String WEBHOOK_NOTIFICATION_PROVIDER =
            System.getProperty("vavi.nio.file.watch.webhook.NotificationProvider.googledrive", ".googledrive.webhook.websocket");

    private static final String VAVI_APPS_WEBHOOK_SECRET = System.getenv("VAVI_APPS_WEBHOOK_SECRET");
    private static final String webhooktUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBHOOK_GOOGLE_URL");

    /** channel lifetime in hours (max is 1 week for changes), renewed at the half of it */
    private static final int CHANNEL_EXPIRATION_HOURS = 24;

    /**
     * a notification is delivered only to the first instance's callback (see {@link WebHookBaseWatchService#setupNotification}),
     * so dispatch it to the instance by channel id.
     */
    private static final Map<String, GoogleDriveWatchService> services = new ConcurrentHashMap<>();

    private final Drive drive;
    private Channel channel = null;

    private String savedStartPageToken;

    private final ScheduledExecutorService renewer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "googledrive-channel-renewer");
        thread.setDaemon(true);
        return thread;
    });

    /** */
    public GoogleDriveWatchService(Drive drive) throws IOException {
        this.drive = drive;

        UUID uuid = UUID.randomUUID();

        setupNotification(this, WEBHOOK_NOTIFICATION_PROVIDER, uuid);

        StartPageToken response = drive.changes().getStartPageToken().execute();
        savedStartPageToken = response.getStartPageToken();
logger.log(Level.DEBUG, "GOOGLE: start token: " + savedStartPageToken);

        channel = watch(uuid.toString());

        renewer.scheduleAtFixedRate(this::renewChannel,
                CHANNEL_EXPIRATION_HOURS / 2, CHANNEL_EXPIRATION_HOURS / 2, TimeUnit.HOURS);
    }

    /** creates a channel and registers it */
    private Channel watch(String channelId) throws IOException {
        // the notification is shared by instances, so register channels other than the first one
        GoogleWebSocketNotification.addChannel(channelId);

        Channel content = new Channel()
                .setId(channelId)
                .setType("web_hook")
                .setToken(VAVI_APPS_WEBHOOK_SECRET)
                .setAddress(webhooktUrl)
                .setExpiration(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(CHANNEL_EXPIRATION_HOURS));

        services.put(channelId, this);
        try {
            Channel channel = drive.changes().watch(savedStartPageToken, content).execute();
logger.log(Level.DEBUG, "GOOGLE: channel: " + channel);
            return channel;
        } catch (IOException e) {
            services.remove(channelId);
            GoogleWebSocketNotification.removeChannel(channelId);
            throw e;
        }
    }

    /** stops and unregisters the channel */
    private void stop(Channel channel) throws IOException {
        services.remove(channel.getId());
        GoogleWebSocketNotification.removeChannel(channel.getId());
        drive.channels().stop(channel).execute();
logger.log(Level.DEBUG, "GOOGLE: channel deleted: " + channel);
    }

    /** a channel cannot be extended, so create a new one then stop the old one */
    private synchronized void renewChannel() {
        try {
            Channel oldChannel = channel;
            channel = watch(UUID.randomUUID().toString());
            stop(oldChannel);
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    /**
        UnparsedNotification{
          messageNumber=9727,
          resourceState=change,
          resourceId=r7_j-XM_WRzwHe8Lns_ECZzzXS8,
          resourceUri=https://www.googleapis.com/drive/v3/changes?includeCorpusRemovals=false&includeItemsFromAllDrives=false&includeRemoved=true&includeTeamDriveItems=false&pageSize=100&pageToken=905064&restrictToMyDrive=false&spaces=drive&supportsAllDrives=false&supportsTeamDrives=false&alt=json,
          channelId=$uuid,
          channelExpiration=Mon, 06 Jul 2020 16:44:43 GMT,
          channelToken=$VAVI_APPS_WEBHOOK_SECRET,
          changed=null,
          contentType=null
        }
     */
    protected void onNotifyMessage(UnparsedNotification notification) throws IOException {
logger.log(Level.DEBUG, ">> notification: " + notification.getResourceState());

        GoogleDriveWatchService service = notification.getChannelId() != null ? services.get(notification.getChannelId()) : null;
        if (service == null) {
logger.log(Level.DEBUG, ">> notification is not for this channel: " + notification.getChannelId());

//try { // *** STOP ANOTHER CHANNEL ***
// Channel c = new Channel();
// c.setId(notification.getChannelId());
// c.setResourceId(notification.getResourceId());
// drive.channels().stop(c).execute();
// logger.log(Level.DEBUG, ">> notification: stop another channel: " + c.getId());
//} catch (IOException e) {
// e.printStackTrace();
//}

            return;
        }
        if (VAVI_APPS_WEBHOOK_SECRET != null && !VAVI_APPS_WEBHOOK_SECRET.equals(notification.getChannelToken())) {
logger.log(Level.WARNING, ">> notification: channel token mismatch: " + notification.getChannelId());
            return;
        }

        service.processNotification(notification);

logger.log(Level.DEBUG, ">> notification: done");
    }

    /** */
    private synchronized void processNotification(UnparsedNotification notification) throws IOException {
        if (!isOpen()) {
            return;
        }

        switch (notification.getResourceState()) {
        case "sync":
logger.log(Level.DEBUG, ">> synched");
            break;
        case "change":
            // Begin with our last saved start token for this user or the
            // current token from getStartPageToken()
            String pageToken = savedStartPageToken;
            while (pageToken != null) {
                ChangeList changes = drive.changes().list(pageToken).execute();
                for (Change change : changes.getChanges()) {
                    // Process change
logger.log(Level.DEBUG, ">> " + (change.getFile() == null ? "id" : isFolder(change.getFile()) ? "folder" : "file") +
              "[" + (change.getFile() != null ? change.getFile().getName() : change.getFileId()) + "] " + (change.getRemoved() ? "deleted" : "updated?"));

                    listener.accept(change.getFileId(), change.getRemoved() ? ENTRY_DELETE : ENTRY_MODIFY);
                }
                if (changes.getNewStartPageToken() != null) {
                    // Last page, save this token for the next polling interval
                    savedStartPageToken = changes.getNewStartPageToken();
                }
                pageToken = changes.getNextPageToken();
            }
            break;
        default:
logger.log(Level.DEBUG, ">> unhandled state: " + notification.getResourceState());
            break;
        }
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            super.close();

            renewer.shutdownNow();
            synchronized (this) {
                stop(channel);
            }
        }
    }

    /** TODO duplicated */
    private static boolean isFolder(File file) {
        return GoogleDriveFileSystemDriver.MIME_TYPE_DIR.equals(file.getMimeType());
    }
}
