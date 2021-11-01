/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

import com.microsoft.graph.authentication.BaseAuthenticationProvider;
import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.http.HttpMethod;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.http.IStatefulResponseHandler;
import com.microsoft.graph.httpcore.HttpClients;
import com.microsoft.graph.requests.GraphServiceClient;

import okhttp3.OkHttpClient;


/**
 * The LRA monitor request.
 */
public class LraMonitorRequest {

    /**
     * The base request.
     */
    private final BaseRequest<?> baseRequest;

    /**
     * Construct the LraMonitorRequest
     * Note: This request does not require authentication, since the URL is short-lived and unique to the original caller.
     * @param requestUrl The upload URL.
     * @param client The Graph client.
     */
    public LraMonitorRequest(final String requestUrl, GraphServiceClient<?> client) {
    	BaseAuthenticationProvider authenticationProvider = new BaseAuthenticationProvider() {
            @Override
            public CompletableFuture<String> getAuthorizationTokenAsync(final URL requestUrl) {
                return CompletableFuture.completedFuture((String)null);
            }
        };
        OkHttpClient httpClient = HttpClients.createDefault(authenticationProvider);
        GraphServiceClient<?> clientWithoutAuth = GraphServiceClient.builder()
                .httpClient(httpClient)
                .logger(client.getLogger())
                .buildClient();
        this.baseRequest = new BaseRequest<LraMonitorResult>(requestUrl, clientWithoutAuth, null, LraMonitorResult.class) {
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
                .send((IHttpRequest) baseRequest,
                      LraMonitorResult.class,
                      null,
                      (IStatefulResponseHandler) responseHandler);
    }
}
