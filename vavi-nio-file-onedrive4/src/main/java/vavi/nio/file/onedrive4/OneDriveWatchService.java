/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.Subscription;
import com.microsoft.graph.requests.extensions.IDriveItemDeltaCollectionPage;
import com.microsoft.graph.requests.extensions.IDriveItemDeltaCollectionRequestBuilder;

import vavi.nio.file.watch.webhook.WebHookBaseWatchService;

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
 * <li> vavi.nio.file.watch.webhook.NotificationProvider.onedrive4
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
            System.getProperty("vavi.nio.file.watch.webhook.NotificationProvider.onedrive4", ".onedrive4.webhook.websocket");

    private static final String VAVI_APPS_WEBHOOK_SECRET = System.getenv("VAVI_APPS_WEBHOOK_SECRET");
    private static final String webhooktUrl = System.getenv("VAVI_APPS_WEBHOOK_WEBHOOK_MICROSOFT_URL");

    /** subscription lifetime in hours, renewed at the half of it */
    private static final int SUBSCRIPTION_EXPIRATION_HOURS = 24;

    /**
     * a notification is delivered only to the first instance's callback (see {@link WebHookBaseWatchService#setupNotification}),
     * so dispatch it to the instance by subscription id.
     */
    private static final Map<String, OneDriveWatchService> services = new ConcurrentHashMap<>();

    private final IGraphServiceClient client;

    private Subscription subscription;

    private String savedDeltaToken;

    private final ScheduledExecutorService renewer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "onedrive4-subscription-renewer");
        thread.setDaemon(true);
        return thread;
    });

    /** */
    public OneDriveWatchService(IGraphServiceClient client) throws IOException {
        this.client = client;

        setupNotification(this, WEBHOOK_NOTIFICATION_PROVIDER);

        try {
            // "latest" returns no items but the delta link for the current state
            savedDeltaToken = getDeltaToken(client.drive().root().delta("latest").buildRequest().get().deltaLink());
logger.log(Level.DEBUG, "MICROSOFT: start token: " + savedDeltaToken);

            Subscription preSubscription = new Subscription();
            preSubscription.changeType = "updated"; // root is only supported 'updated'
            preSubscription.notificationUrl = webhooktUrl;
            preSubscription.resource = "me/drive/root";
            preSubscription.expirationDateTime = getExpirationDateTime();
            preSubscription.clientState = VAVI_APPS_WEBHOOK_SECRET;

            subscription = client.subscriptions().buildRequest().post(preSubscription);
logger.log(Level.DEBUG, "MICROSOFT: subscription: " + subscription.id);
        } catch (ClientException e) {
            throw new IOException(e);
        }

        services.put(subscription.id, this);

        renewer.scheduleAtFixedRate(this::renewSubscription,
                SUBSCRIPTION_EXPIRATION_HOURS / 2, SUBSCRIPTION_EXPIRATION_HOURS / 2, TimeUnit.HOURS);
    }

    /** */
    private static Calendar getExpirationDateTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, SUBSCRIPTION_EXPIRATION_HOURS);
        return calendar;
    }

    /** */
    private void renewSubscription() {
        try {
            Subscription preSubscription = new Subscription();
            preSubscription.expirationDateTime = getExpirationDateTime();
            subscription = client.subscriptions(subscription.id).buildRequest().patch(preSubscription);
logger.log(Level.DEBUG, "MICROSOFT: subscription renewed: " + subscription.id + ", " + subscription.expirationDateTime.getTime());
        } catch (ClientException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    /** @return "token" parameter value of the delta link */
    private static String getDeltaToken(String deltaLink) {
        if (deltaLink == null) {
            return null;
        }
        String query = URI.create(deltaLink).getRawQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return param.substring("token=".length());
                }
            }
        }
        // e.g. ".../delta(token='xxx')"
        int p = deltaLink.indexOf("token='");
        if (p != -1) {
            return deltaLink.substring(p + "token='".length(), deltaLink.indexOf('\'', p + "token='".length()));
        }
        throw new IllegalArgumentException("no token: " + deltaLink);
    }

    /**
     * <pre>
     * {
     *   "value": [
     *     {
     *       "subscriptionId": "...",
     *       "clientState": "$VAVI_APPS_WEBHOOK_SECRET",
     *       "changeType": "updated",
     *       "resource": "me/drive/root",
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

        JsonObject root = JsonParser.parseString(notification).getAsJsonObject();
        if (!root.has("value")) {
logger.log(Level.DEBUG, ">> notification: unknown format");
            return;
        }
        for (JsonElement element : root.getAsJsonArray("value")) {
            JsonObject value = element.getAsJsonObject();
            String subscriptionId = value.has("subscriptionId") ? value.get("subscriptionId").getAsString() : null;
            String clientState = value.has("clientState") ? value.get("clientState").getAsString() : null;

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

    /** retrieves changes since the last saved delta token */
    private synchronized void processChanges() throws IOException {
        if (!isOpen()) {
            return;
        }
        try {
            IDriveItemDeltaCollectionRequestBuilder builder = client.drive().root().delta(savedDeltaToken);
            while (builder != null) {
                IDriveItemDeltaCollectionPage page = builder.buildRequest().get();
                for (DriveItem item : page.getCurrentPage()) {
logger.log(Level.DEBUG, ">> " + (item.folder != null ? "folder" : "file") + "[" + (item.name != null ? item.name : item.id) + "] " + (item.deleted != null ? "deleted" : "updated?"));

                    listener.accept(item.id, item.deleted != null ? ENTRY_DELETE : ENTRY_MODIFY);
                }
                if (page.deltaLink() != null) {
                    // last page, save this token for the next notification
                    savedDeltaToken = getDeltaToken(page.deltaLink());
                }
                builder = page.getNextPage();
            }
        } catch (ClientException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (isOpen()) {
            super.close();

            renewer.shutdownNow();
            services.remove(subscription.id);
            try {
                client.subscriptions(subscription.id).buildRequest().delete();
logger.log(Level.DEBUG, "MICROSOFT: subscription deleted: " + subscription.id);
            } catch (ClientException e) {
                throw new IOException(e);
            }
        }
    }
}
