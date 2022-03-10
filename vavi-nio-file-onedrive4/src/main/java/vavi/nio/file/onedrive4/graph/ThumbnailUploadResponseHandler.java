/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;

import com.microsoft.graph.http.IConnection;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.http.IStatefulResponseHandler;
import com.microsoft.graph.logger.ILogger;
import com.microsoft.graph.serializer.ISerializer;

import okhttp3.Response; // TODO WTF??? why not be encapsulated in IConnection??? who did this??? crazy!


/**
 * Handles the response from the ThumbnailUpload
 */
public class ThumbnailUploadResponseHandler
        implements IStatefulResponseHandler<Integer, byte[]> {

    /**
     * Do nothing before getting the response
     *
     * @param connection the connection
     */
    @Override
    public void configConnection(final IConnection connection) {
    }

    @Override
    public void configConnection(Response response) {
    }

    @Override
    public Integer generateResult(IHttpRequest request,
                                  Response response,
                                  ISerializer serializer,
                                  ILogger logger) throws Exception {
        // why i need to write twice...
        return response.code();
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
    public Integer generateResult(
            final IHttpRequest request,
            final IConnection connection,
            final ISerializer serializer,
            final ILogger logger) throws Exception {

        return connection.getResponseCode();
    }
}
