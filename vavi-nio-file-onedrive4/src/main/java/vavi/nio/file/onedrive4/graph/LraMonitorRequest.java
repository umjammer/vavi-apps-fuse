/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;

import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.http.HttpMethod;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;


/**
 * The LRA monitor request.
 */
public class LraMonitorRequest {

    /**
     * The base request.
     */
    private final BaseRequest baseRequest;

    /**
     * Construct the LraMonitorRequest
     * Note: This request does not require authentication, since the URL is short-lived and unique to the original caller.
     * @param requestUrl The upload URL.
     * @param client The Graph client.
     */
    public LraMonitorRequest(final String requestUrl, IGraphServiceClient client) {
        IGraphServiceClient clientWithoutAuth = GraphServiceClient.builder()
                .authenticationProvider(request -> {
                })
                .logger(client.getLogger())
                .buildClient();
        this.baseRequest = new BaseRequest(requestUrl, clientWithoutAuth, null, LraMonitorResult.class) {
        };
        this.baseRequest.setHttpMethod(HttpMethod.GET);
    }

    /**
     * Monitor a LRA.
     *
     * @param responseHandler The handler to handle the HTTP response.
     * @return The monitor result.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" }) // TODO
    public LraMonitorResult monitor(final LraMonitorResponseHandler responseHandler) {
        return this.baseRequest.getClient()
                .getHttpProvider()
                .send(baseRequest,
                      LraMonitorResult.class,
                      null,
                        responseHandler);
    }
}
