/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;

import java.io.IOException;
import java.security.InvalidParameterException;

import com.microsoft.graph.http.BaseRequest;
import com.microsoft.graph.http.HttpMethod;
import com.microsoft.graph.http.HttpResponseCode;
import com.microsoft.graph.models.extensions.DriveItem;
import com.microsoft.graph.models.extensions.IGraphServiceClient;

import vavi.util.Debug;


/**
 * ThumbnailUpload service provider
 */
public class ThumbnailUploadProvider {

    /**
     * The client
     */
    private final IGraphServiceClient client;

    /**
     * The upload URL
     */
    private final String uploadUrl;

    /**
     * Creates the ChunkedUploadProvider
     *
     * @param item   the upload item
     * @param client the Graph client
     */
    public ThumbnailUploadProvider(DriveItem item, IGraphServiceClient client) {
        if (item == null) {
            throw new InvalidParameterException("item is null.");
        }

        if (client == null) {
            throw new InvalidParameterException("OneDrive client is null.");
        }

        this.client = client;
        this.uploadUrl = client.getServiceRoot() + "/drive/items/" + item.id + "/thumbnails/0/source/content";
Debug.println("url: " + uploadUrl);
    }

    /**
     * Uploads a thumbnail.
     *
     * @param image  the thumbnail bytes
     * @throws IOException the IO exception that occurred during upload
     */
    public void upload(byte[] image) throws IOException {

        BaseRequest request = new BaseRequest(uploadUrl, client, null, Integer.class) {{
            setHttpMethod(HttpMethod.PUT);
        }};
        int result = client.getHttpProvider().send(
                request,
                Integer.class,
                image,
                new ThumbnailUploadResponseHandler());

        if (result != HttpResponseCode.HTTP_OK) {
            throw new IOException(String.valueOf(result));
        }
    }
}
