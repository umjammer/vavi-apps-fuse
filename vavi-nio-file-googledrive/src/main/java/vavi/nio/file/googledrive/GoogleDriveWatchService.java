/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.util.UUID;

import com.google.api.client.googleapis.notifications.UnparsedNotification;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.StartPageToken;

import vavi.nio.file.watch.webhook.WebHookBaseWatchService;
import vavi.util.Debug;

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

    private static final String WEBHOOK_NOTIFICATION_PROVIDER =
            System.getProperty("vavi.nio.file.watch.webhook.NotificationProvider.googledrive", ".googledrive.webhook.websocket");

    private static final String VAVI_APPS_WEBHOOK_SECRET = System.getenv("VAVI_APPS_WEBHOOK_SECRET");
    private static final String webhooktUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBHOOK_GOOGLE_URL");

    private Drive drive;
    private Channel channel = null;

    private String savedStartPageToken;

    /** */
    public GoogleDriveWatchService(Drive drive) throws IOException {
        this.drive = drive;

        UUID uuid = UUID.randomUUID();

        setupNotification(this, WEBHOOK_NOTIFICATION_PROVIDER, uuid);

        StartPageToken response = drive.changes().getStartPageToken().execute();
        savedStartPageToken = response.getStartPageToken();
Debug.println("GOOGLE: start token: " + savedStartPageToken);

        Channel content = new Channel()
                .setId(uuid.toString())
                .setType("web_hook")
                .setToken(VAVI_APPS_WEBHOOK_SECRET)
                .setAddress(webhooktUrl);

        channel = drive.changes().watch(savedStartPageToken, content).execute();
Debug.println("GOOGLE: channel: " + channel);
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
Debug.println(">> notification: " + notification.getResourceState());

        if (!channel.getId().equals(notification.getChannelId())) {
Debug.println(">> notification is not for this channel: " + notification.getChannelId());

//try { // *** STOP ANOTHER CHANNEL ***
// Channel c = new Channel();
// c.setId(notification.getChannelId());
// c.setResourceId(notification.getResourceId());
// drive.channels().stop(c).execute();
// Debug.println(">> notification: stop another channel: " + c.getId());
//} catch (IOException e) {
// e.printStackTrace();
//}

            return;
        }

        switch (notification.getResourceState()) {
        case "sync":
Debug.println(">> synched");
            break;
        case "change":
            // Begin with our last saved start token for this user or the
            // current token from getStartPageToken()
            String pageToken = savedStartPageToken;
            while (pageToken != null) {
                ChangeList changes = drive.changes().list(pageToken).execute();
                for (Change change : changes.getChanges()) {
                    // Process change
Debug.println(">> " + (change.getFile() == null ? "id" : isFolder(change.getFile()) ? "folder" : "file") +
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
Debug.println(">> unhandled state: " + notification.getResourceState());
            break;
        }

Debug.println(">> notification: done");
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            super.close();

            drive.channels().stop(channel).execute();
Debug.println("GOOGLE: channel deleted: " + channel);
        }
    }

    /** TODO duplicated */
    private static boolean isFolder(File file) {
        return GoogleDriveFileSystemDriver.MIME_TYPE_DIR.equals(file.getMimeType());
    }
}
