/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.nuxeo.onedrive.client;

import java.net.URL;
import java.util.Iterator;
import java.util.Objects;

import com.eclipsesource.json.JsonObject;


/**
 * OneDriveSubscriptionsIterator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/05 umjammer initial version <br>
 */
public class OneDriveSubscriptionsIterator implements Iterator<OneDriveSubscriptions.Metadata> {

    private final OneDriveAPI api;

    private final JsonObjectIterator jsonObjectIterator;

    public OneDriveSubscriptionsIterator(OneDriveAPI api, URL url) {
        this.api = Objects.requireNonNull(api);
        this.jsonObjectIterator = new JsonObjectIterator(api, url) {

            @Override
            protected void onResponse(JsonObject response) {
                OneDriveSubscriptionsIterator.this.onResponse(response);
            }

        };
    }

    @Override
    public boolean hasNext() throws OneDriveRuntimeException {
        return jsonObjectIterator.hasNext();
    }

    @Override
    public OneDriveSubscriptions.Metadata next() throws OneDriveRuntimeException {
        return null; //OneDriveSubscriptions.parseJson(api, jsonObjectIterator.next());
    }

    /**
     * @since 1.1
     */
    protected void onResponse(JsonObject response) {
        // Hook method
    }

}
