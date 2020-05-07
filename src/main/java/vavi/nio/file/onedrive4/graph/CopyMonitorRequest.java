/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;

import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.http.HttpMethod;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.http.IStatefulResponseHandler;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;


/**
 * The copy monitor request.
 */
public class CopyMonitorRequest {

    /**
     * The base request.
     */
    private final BaseRequest baseRequest;

    /**
     * Construct the CopyMonitorRequest
     * Note: This request does not require authentication, since the URL is short-lived and unique to the original caller.
     * @param requestUrl The upload URL.
     * @param client The Graph client.
     */
    public CopyMonitorRequest(final String requestUrl, IGraphServiceClient client) {
        IGraphServiceClient clientWithoutAuth = GraphServiceClient.builder()
                .authenticationProvider(new IAuthenticationProvider() {
                    @Override
                    public void authenticateRequest(IHttpRequest request) {
                    }
                })
                .buildClient();
        this.baseRequest = new BaseRequest(requestUrl, clientWithoutAuth, null, CopyMonitorResult.class) {
        };
        this.baseRequest.setHttpMethod(HttpMethod.GET);
    }

    /**
     * Monitor a copy.
     *
     * @param responseHandler The handler to handle the HTTP response.
     * @return The monitor result.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" }) // TODO
    public CopyMonitorResult monitor(final CopyMonitorResponseHandler responseHandler) {
        return this.baseRequest.getClient()
                .getHttpProvider()
                .send((IHttpRequest) baseRequest,
                      CopyMonitorResult.class,
                      null,
                      (IStatefulResponseHandler) responseHandler);
    }
}
