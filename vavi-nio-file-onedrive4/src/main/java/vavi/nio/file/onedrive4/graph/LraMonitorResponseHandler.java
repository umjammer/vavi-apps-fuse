/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;

import java.io.BufferedInputStream;
import java.io.InputStream;

import com.microsoft.graph.http.DefaultHttpProvider;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.http.HttpResponseCode;
import com.microsoft.graph.http.IConnection;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.http.IStatefulResponseHandler;
import com.microsoft.graph.logger.ILogger;
import com.microsoft.graph.serializer.ISerializer;

/**
 * Handles the stateful response from the OneDrive LRA session
 *
 * @param <MonitorType> the expected LRA item
 */
public class LraMonitorResponseHandler<MonitorType>
        implements IStatefulResponseHandler<LraMonitorResult, MonitorType> {

    /**
     * Do nothing before getting the response
     *
     * @param connection the connection
     */
    @Override
    public void configConnection(final IConnection connection) {
        return;
    }

    /**
     * Generate the LRA monitor response result
     *
     * @param request the HTTP request
     * @param connection the HTTP connection
     * @param serializer the serializer
     * @param logger the system logger
     * @return the LRA monitor result, which could be either an LRA monitor or error
     * @throws Exception an exception occurs if the request was unable to complete for any reason
     */
    @Override
    public LraMonitorResult generateResult(
            final IHttpRequest request,
            final IConnection connection,
            final ISerializer serializer,
            final ILogger logger) throws Exception {
        InputStream in = null;

        try {
            if (connection.getResponseCode() == HttpResponseCode.HTTP_ACCEPTED) {
logger.logDebug("LRA has been accepted by the server.");
                final LraSession session = new LraSession();
                session.monitorUrl = connection.getResponseHeaders().get("Location").get(0);
                return new LraMonitorResult(session);
            } else if (connection.getResponseCode() == HttpResponseCode.HTTP_SEE_OTHER) {
logger.logDebug("see other url.");
                String seeOtherUrl = connection.getResponseHeaders().get("Location").get(0);
                return new LraMonitorResult(seeOtherUrl);
            } else if (connection.getResponseCode() == HttpResponseCode.HTTP_CREATED
                    || connection.getResponseCode() == HttpResponseCode.HTTP_OK) {
logger.logDebug("LRA session is completed, drive item returned.");
                in = new BufferedInputStream(connection.getInputStream());
                String rawJson = DefaultHttpProvider.streamToString(in);
                MonitorObject monitoredItem = serializer.deserializeObject(rawJson, MonitorObject.class);

                return new LraMonitorResult(monitoredItem);
            } else if (connection.getResponseCode() >= HttpResponseCode.HTTP_CLIENT_ERROR) {
logger.logDebug("LRA error during monitor, see detail on result error");
                return new LraMonitorResult(
                        GraphServiceException.createFromConnection(request, null, serializer,
                                connection, logger));
            } else {
logger.logDebug("unhandled response code: " + connection.getResponseCode());
                return null;
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
