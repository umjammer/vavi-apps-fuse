/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive3;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.nuxeo.onedrive.client.OneDriveAPI;
import org.nuxeo.onedrive.client.OneDriveJsonRequest;
import org.nuxeo.onedrive.client.OneDriveJsonResponse;
import org.nuxeo.onedrive.client.OneDriveRequest;
import org.nuxeo.onedrive.client.OneDriveResponse;
import org.nuxeo.onedrive.client.URLTemplate;

import vavi.nio.file.watch.webhook.WebHookBaseWatchService;

import static java.lang.System.getLogger;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;


/**
 * OneDriveWatchService.
 * <p>
 * notification source is {@link #subscriptionId}, changes are retrieved by the delta api.
 * <p>
 * system properties
 * <ul>
 * <li> vavi.nio.file.watch.webhook.NotificationProvider.onedrive3
 * </ul>
 * </p>
 * <p>
 * environment variables
 * <ul>
 * <li> VAVI_APPS_WEBHOOK_SECRET
 * <li> VAVI_APPS_WEBHOOK_WEBHOOK_MICROSOFT_URL
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/23 umjammer initial version <br>
 * @see "https://docs.microsoft.com/graph/api/subscription-post-subscriptions?view=graph-rest-1.0&tabs=http"
 * @see "https://docs.microsoft.com/graph/api/driveitem-delta?view=graph-rest-1.0&tabs=http"
 */
public class OneDriveWatchService extends WebHookBaseWatchService<String> {

    private static final Logger logger = getLogger(OneDriveWatchService.class.getName());

    private static final String WEBHOOK_NOTIFICATION_PROVIDER =
            System.getProperty("vavi.nio.file.watch.webhook.NotificationProvider.onedrive3", ".onedrive3.webhook.websocket");

    private static final String VAVI_APPS_WEBHOOK_SECRET = System.getenv("VAVI_APPS_WEBHOOK_SECRET");
    private static final String webhooktUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBHOOK_MICROSOFT_URL");

    /** subscription lifetime in hours, renewed at the half of it */
    private static final int SUBSCRIPTION_EXPIRATION_HOURS = 24;

    private static final URLTemplate SUBSCRIPTIONS_URL = new URLTemplate("/subscriptions");
    private static final URLTemplate SUBSCRIPTION_URL = new URLTemplate("/subscriptions/%s");
    private static final URLTemplate DELTA_LATEST_URL = new URLTemplate("/drives/%s/root/delta?token=latest");

    /**
     * a notification is delivered only to the first instance's callback (see {@link WebHookBaseWatchService#setupNotification}),
     * so dispatch it to the instance by subscription id.
     */
    private static final Map<String, OneDriveWatchService> services = new ConcurrentHashMap<>();

    private final OneDriveAPI client;

    private final String subscriptionId;

    /** the delta link includes the token */
    private String savedDeltaLink;

    private final ScheduledExecutorService renewer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "onedrive3-subscription-renewer");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * @param driveId the drive to be watched
     */
    public OneDriveWatchService(OneDriveAPI client, String driveId) throws IOException {
        this.client = client;

        setupNotification(this, WEBHOOK_NOTIFICATION_PROVIDER);

        // "latest" returns no items but the delta link for the current state
        URL url = DELTA_LATEST_URL.build(client.getBaseURL(), driveId);
        while (savedDeltaLink == null) {
            JsonObject page = getJson(url);
            savedDeltaLink = getString(page, "@odata.deltaLink");
            String nextLink = getString(page, "@odata.nextLink");
            if (savedDeltaLink == null && nextLink == null) {
                throw new IOException("no delta link: " + page);
            }
            if (nextLink != null) {
                url = new URL(nextLink);
            }
        }
logger.log(Level.DEBUG, "MICROSOFT: start delta: " + savedDeltaLink);

        JsonObject body = new JsonObject()
                .add("changeType", "updated") // root is only supported 'updated'
                .add("notificationUrl", webhooktUrl)
                .add("resource", "drives/" + driveId + "/root")
                .add("expirationDateTime", getExpirationDateTime())
                .add("clientState", VAVI_APPS_WEBHOOK_SECRET);
        OneDriveJsonRequest request = new OneDriveJsonRequest(SUBSCRIPTIONS_URL.build(client.getBaseURL()), "POST", body);
        try (OneDriveJsonResponse response = request.sendRequest(client.getExecutor())) {
            subscriptionId = response.getContent().get("id").asString();
        }
logger.log(Level.DEBUG, "MICROSOFT: subscription: " + subscriptionId);

        services.put(subscriptionId, this);

        renewer.scheduleAtFixedRate(this::renewSubscription,
                SUBSCRIPTION_EXPIRATION_HOURS / 2, SUBSCRIPTION_EXPIRATION_HOURS / 2, TimeUnit.HOURS);
    }

    /** */
    private static String getExpirationDateTime() {
        return Instant.now().plus(SUBSCRIPTION_EXPIRATION_HOURS, ChronoUnit.HOURS).toString();
    }

    /** */
    private void renewSubscription() {
        JsonObject body = new JsonObject()
                .add("expirationDateTime", getExpirationDateTime());
        OneDriveJsonRequest request = new OneDriveJsonRequest(SUBSCRIPTION_URL.build(client.getBaseURL(), subscriptionId), "PATCH", body);
        try (OneDriveJsonResponse response = request.sendRequest(client.getExecutor())) {
logger.log(Level.DEBUG, "MICROSOFT: subscription renewed: " + subscriptionId + ", " + getString(response.getContent(), "expirationDateTime"));
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    /** */
    private JsonObject getJson(URL url) throws IOException {
        OneDriveJsonRequest request = new OneDriveJsonRequest(url, "GET");
        try (OneDriveJsonResponse response = request.sendRequest(client.getExecutor())) {
            return response.getContent();
        }
    }

    /** @return null when the member does not exist */
    private static String getString(JsonObject object, String name) {
        JsonValue value = object.get(name);
        return value != null && value.isString() ? value.asString() : null;
    }

    /**
     * <pre>
     * {
     *   "value": [
     *     {
     *       "subscriptionId": "...",
     *       "clientState": "$VAVI_APPS_WEBHOOK_SECRET",
     *       "changeType": "updated",
     *       "resource": "drives/.../root",
     *       "subscriptionExpirationDateTime": "...",
     *       "tenantId": "..."
     *     }
     *   ]
     * }
     * </pre>
     */
    @Override
    protected void onNotifyMessage(String notification) throws IOException {
logger.log(Level.DEBUG, ">> notification: " + notification);

        JsonValue values = Json.parse(notification).asObject().get("value");
        if (values == null || !values.isArray()) {
logger.log(Level.DEBUG, ">> notification: unknown format");
            return;
        }
        for (JsonValue element : values.asArray()) {
            JsonObject value = element.asObject();
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
            JsonObject page = getJson(new URL(link));
            JsonValue items = page.get("value");
            if (items != null && items.isArray()) {
                for (JsonValue element : items.asArray()) {
                    JsonObject item = element.asObject();
                    String id = getString(item, "id");
                    boolean deleted = item.get("deleted") != null;
logger.log(Level.DEBUG, ">> " + (item.get("folder") != null ? "folder" : "file") + "[" + (getString(item, "name") != null ? getString(item, "name") : id) + "] " + (deleted ? "deleted" : "updated?"));

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
            services.remove(subscriptionId);
            OneDriveRequest request = new OneDriveRequest(SUBSCRIPTION_URL.build(client.getBaseURL(), subscriptionId), "DELETE");
            try (OneDriveResponse response = request.sendRequest(client.getExecutor())) {
logger.log(Level.DEBUG, "MICROSOFT: subscription deleted: " + subscriptionId + ", " + response.getResponseCode());
            }
        }
    }
}
