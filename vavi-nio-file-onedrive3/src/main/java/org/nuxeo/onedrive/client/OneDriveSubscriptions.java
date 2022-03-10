/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.nuxeo.onedrive.client;

import java.io.IOException;
import java.net.URL;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;


/**
 * OneDriveSubscriptions.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/05 umjammer initial version <br>
 */
public class OneDriveSubscriptions extends OneDriveResource {
    private OneDriveSubscriptions(OneDriveAPI api) {
        super(api);
    }

    public Metadata getMetadata(OneDriveExpand... expands) throws IOException {
        QueryStringBuilder query = new QueryStringBuilder().set("expand", expands);
        final URL url = getMetadataUrl().build(getApi().getBaseURL(), query, getItemIdentifier());
        OneDriveJsonRequest request = new OneDriveJsonRequest(url, "POST");
        OneDriveJsonResponse response = request.sendRequest(getApi().getExecutor());
        JsonObject jsonObject = response.getContent();
        response.close();
        return new OneDriveSubscriptions.Metadata(jsonObject);
    }

    public URLTemplate getMetadataUrl() {
        return new URLTemplate(getPath());
    }

    public String getPath() {
        return "/subscriptions";
    }

    @Override
    public String getFullyQualifiedPath() {
        return getPath();
    }

    public class Metadata extends OneDriveResource.Metadata {
        public String id;
        public String resource;
        public String changeType;
        public String clientState;
        public String notificationUrl;
        public String expirationDateTime;
        public String applicationId;
        public String creatorId;

        public Metadata(final JsonObject json) {
            super(json);
        }

        @Override
        public OneDriveResource getResource() {
            return OneDriveSubscriptions.this;
        }

        @Override
        protected void parseMember(JsonObject.Member member) {
            super.parseMember(member);
            try {
                JsonValue value = member.getValue();
                String memberName = member.getName();
                if ("id".equals(memberName)) {
                    id = value.asString();
                } else if ("resource".equals(memberName)) {
                    resource = value.asString();
                } else if ("changeType".equals(memberName)) {
                    changeType = value.asString();
                } else if ("clientState".equals(memberName)) {
                    clientState = value.asString();
                } else if ("notificationUrl".equals(memberName)) {
                    notificationUrl = value.asString();
                } else if ("expirationDateTime".equals(memberName)) {
                    expirationDateTime = value.asString();
                } else if ("applicationId".equals(memberName)) {
                    applicationId = value.asString();
                } else if ("creatorId".equals(memberName)) {
                    creatorId = value.asString();
                }
            } catch (ParseException e) {
                throw new OneDriveRuntimeException(new OneDriveAPIException(e.getMessage(), e));
            }
        }

        public String getResource_() {
            return resource;
        }

        public String getChangeType() {
            return changeType;
        }

        public String getClientState() {
            return clientState;
        }

        public String getNotificationUrl() {
            return notificationUrl;
        }

        public String getExpirationDateTime() {
            return expirationDateTime;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public String getCreatorId() {
            return creatorId;
        }

        @Override
        public String getId() {
            return id;
        }
    }
}
