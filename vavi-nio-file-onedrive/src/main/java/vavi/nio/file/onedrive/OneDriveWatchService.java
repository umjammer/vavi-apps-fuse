/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import vavi.nio.file.watch.webhook.WebHookBaseWatchService;

import de.tuberlin.onedrivesdk.OneDriveException;
import de.tuberlin.onedrivesdk.OneDriveSDK;
import de.tuberlin.onedrivesdk.common.Subscription;
import de.tuberlin.onedrivesdk.networking.OneResponse;
import de.tuberlin.onedrivesdk.networking.PreparedRequestMethod;

import static java.lang.System.getLogger;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;


/**
 * OneDriveWatchService.
 * <p>
 * notification source is {@link #subscription}, changes are retrieved by the delta api.
 * <p>
 * system properties
 * <ul>
 * <li> vavi.nio.file.watch.webhook.NotificationProvider.onedrive
 * </ul>
 * </p>
 * <p>
 * environment variables
 * <ul>
 * <li> VAVI_APPS_WEBHOOK_SECRET
 * <li> VAVI_APPS_WEBHOOK_WEBHOOK_ONEDRIVE_URL
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/22 umjammer initial version <br>
 * @see "https://tsmatz.wordpress.com/2016/04/22/onedrive-api-webhook-and-synchronization-delta/"
 */
public class OneDriveWatchService extends WebHookBaseWatchService<String> {

    private static final Logger logger = getLogger(OneDriveWatchService.class.getName());

    private static final String WEBHOOK_NOTIFICATION_PROVIDER =
            System.getProperty("vavi.nio.file.watch.webhook.NotificationProvider.onedrive", ".onedrive.webhook.websocket");

    private static final String VAVI_APPS_WEBHOOK_SECRET = System.getenv("VAVI_APPS_WEBHOOK_SECRET");
    private static final String webhooktUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBHOOK_ONEDRIVE_URL");

    /**
     * a relative path is appended "?access_token=..." by the sdk, so use complete urls for urls which have a query.
     * @see "de.tuberlin.onedrivesdk.common.ConcreteOneDriveSDK#baseUrl"
     */
    private static final String BASE_URL = "https://api.onedrive.com/v1.0/";

    /** the sdk subscribes for 24 hours, renew at the half of it */
    private static final int SUBSCRIPTION_RENEWAL_HOURS = 12;

    /**
     * a notification is delivered only to the first instance's callback (see {@link WebHookBaseWatchService#setupNotification}),
     * so dispatch it to the instance by subscription id.
     */
    private static final Map<String, OneDriveWatchService> services = new ConcurrentHashMap<>();

    private final OneDriveSDK client;

    private Subscription subscription;

    /** the delta link includes the token */
    private String savedDeltaLink;

    private final ScheduledExecutorService renewer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "onedrive-subscription-renewer");
        thread.setDaemon(true);
        return thread;
    });

    /** */
    public OneDriveWatchService(OneDriveSDK client) throws IOException {
        this.client = client;

        setupNotification(this, WEBHOOK_NOTIFICATION_PROVIDER);

        // "latest" returns no items but the delta link for the current state
        String url = BASE_URL + "drive/root/delta?token=latest";
        while (savedDeltaLink == null) {
            JsonObject page = getJson(url);
            savedDeltaLink = getString(page, "@odata.deltaLink");
            url = getString(page, "@odata.nextLink");
            if (savedDeltaLink == null && url == null) {
                throw new OneDriveException("no delta link: " + page);
            }
        }
logger.log(Level.DEBUG, "ONEDRIVE: start delta: " + savedDeltaLink);

        subscription = client.subscribe(webhooktUrl, VAVI_APPS_WEBHOOK_SECRET);
logger.log(Level.DEBUG, "ONEDRIVE: subscription: " + subscription.getId());

        services.put(subscription.getId(), this);

        renewer.scheduleAtFixedRate(this::renewSubscription,
                SUBSCRIPTION_RENEWAL_HOURS, SUBSCRIPTION_RENEWAL_HOURS, TimeUnit.HOURS);
    }

    /** */
    private void renewSubscription() {
        try {
            subscription = subscription.update();
logger.log(Level.DEBUG, "ONEDRIVE: subscription renewed: " + subscription.getId());
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    /** */
    private JsonObject getJson(String url) throws IOException {
        OneResponse response = client.makeRequest(url, PreparedRequestMethod.GET, null);
        if (!response.wasSuccess()) {
            throw new OneDriveException("Request error: " + response.getStatusCode() + " " + response.getBodyAsString());
        }
        return JsonParser.parseString(response.getBodyAsString()).getAsJsonObject();
    }

    /** @return null when the member does not exist */
    private static String getString(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    /**
     * <pre>
     * {
     *   "value": [
     *     {
     *       "subscriptionId": "...",
     *       "clientState": "$VAVI_APPS_WEBHOOK_SECRET",
     *       "expirationDateTime": "...",
     *       "resource": "/me/drive/root",
     *       "tenantId": "",
     *       "userId": "..."
     *     }
     *   ]
     * }
     * </pre>
     */
    @Override
    protected void onNotifyMessage(String notification) throws IOException {
logger.log(Level.DEBUG, ">> notification: " + notification);

        JsonObject root = JsonParser.parseString(notification).getAsJsonObject();
        if (!root.has("value") || !root.get("value").isJsonArray()) {
logger.log(Level.DEBUG, ">> notification: unknown format");
            return;
        }
        for (JsonElement element : root.getAsJsonArray("value")) {
            JsonObject value = element.getAsJsonObject();
            String subscriptionId = getString(value, "subscriptionId");
            String clientState = getString(value, "clientState");

            OneDriveWatchService service = subscriptionId != null ? services.get(subscriptionId) : null;
            if (service == null) {
logger.log(Level.DEBUG, ">> notification is not for this subscription: " + subscriptionId);
                continue;
            }
            if (VAVI_APPS_WEBHOOK_SECRET != null && !VAVI_APPS_WEBHOOK_SECRET.equals(clientState)) {
logger.log(Level.WARNING, ">> notification: client state mismatch: " + subscriptionId);
                continue;
            }

            service.processChanges();
        }

logger.log(Level.DEBUG, ">> notification: done");
    }

    /** retrieves changes since the last saved delta link */
    private synchronized void processChanges() throws IOException {
        if (!isOpen()) {
            return;
        }
        String link = savedDeltaLink;
        while (link != null) {
            JsonObject page = getJson(link);
            if (page.has("value") && page.get("value").isJsonArray()) {
                for (JsonElement element : page.getAsJsonArray("value")) {
                    JsonObject item = element.getAsJsonObject();
                    String id = getString(item, "id");
                    boolean deleted = item.has("deleted");
logger.log(Level.DEBUG, ">> " + (item.has("folder") ? "folder" : "file") + "[" + (getString(item, "name") != null ? getString(item, "name") : id) + "] " + (deleted ? "deleted" : "updated?"));

                    listener.accept(id, deleted ? ENTRY_DELETE : ENTRY_MODIFY);
                }
            }
            String deltaLink = getString(page, "@odata.deltaLink");
            if (deltaLink != null) {
                // last page, save this link for the next notification
                savedDeltaLink = deltaLink;
            }
            link = getString(page, "@odata.nextLink");
        }
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            super.close();

            renewer.shutdownNow();
            services.remove(subscription.getId());
            try {
                subscription.delete();
logger.log(Level.DEBUG, "ONEDRIVE: subscription deleted: " + subscription.getId());
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
    }
}
