/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;


import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;

import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.UploadSession;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.tasks.IProgressCallback;
import com.microsoft.graph.tasks.LargeFileUploadResult;
import com.microsoft.graph.tasks.LargeFileUploadTask;


/**
 * ChunkedUpload service provider
 *
 * @param <UploadType> the upload item type
 */
public class ChunkedUploadProvider<UploadType> {

    /**
     * The default retry times for a simple chunk upload if failure happened
     */
    private static final int MAXIMUM_RETRY_TIMES = 3;

    /**
     * The client
     */
    private final GraphServiceClient<?> client;

    /**
     * The upload session URL
     */
    private final String uploadUrl;

    /**
     * The stream size
     */
    private final int streamSize;

    /**
     * The upload response handler
     */
    private LargeFileUploadTask<DriveItem> chunkedUploadProvider;

    /**
     * The counter for how many bytes have been read from input stream
     */
    private int readSoFar;

    /**
     * Creates the ChunkedUploadProvider
     *
     * @param uploadSession   the initial upload session
     * @param client          the Graph client
     * @param streamSize      the stream size
     * @param uploadTypeClass the upload type class
     */
    public ChunkedUploadProvider(final UploadSession uploadSession,
                                 final GraphServiceClient<?> client,
                                 final int streamSize,
                                 final Class<UploadType> uploadTypeClass) {
        if (uploadSession == null) {
            throw new InvalidParameterException("Upload session is null.");
        }

        if (client == null) {
            throw new InvalidParameterException("OneDrive client is null.");
        }

        if (streamSize <= 0) {
            throw new InvalidParameterException("Stream size should larger than 0.");
        }

        this.client = client;
        this.readSoFar = 0;
        this.streamSize = streamSize;
        this.uploadUrl = uploadSession.uploadUrl;

        LargeFileUploadTask<DriveItem> chunkedUploadProvider = new LargeFileUploadTask<DriveItem>(
				uploadSession,
				client,
				null,
				streamSize,
				DriveItem.class);
    }

    /**
     * Uploads content to remote upload session based on the input stream
     *
     * @param options  the upload options
     * @param callback the progress callback invoked during uploading
     * @param configs  the optional configurations for the upload options. [0] should be the maxRetry for upload retry.
     * @throws IOException the IO exception that occurred during upload
     */
    public OutputStream upload(final List<Option> options,
                       final IProgressCallback callback,
                       final int... configs)
            throws IOException {

        final int maxRetry = (configs.length > 1) ? configs[0] : MAXIMUM_RETRY_TIMES;

        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new UnsupportedOperationException();
            }
            @Override
            public void write(byte[] b, int ofs, int len) throws IOException {
                byte[] buffer = Arrays.copyOfRange(b, ofs, ofs + len);
                LargeFileUploadResult<DriveItem> result = chunkedUploadProvider.upload(0, null, callback);

//                if (result.uploadCompleted()) {
//                    callback.progress(streamSize, streamSize);
//                    callback.success(result.getItem());
//                } else if (result.chunkCompleted()) {
//                    callback.progress(readSoFar, streamSize);
//                } else if (result.hasError()) {
//                    throw new IOException(result.getError());
//                }

                readSoFar += len;
            }
        };
    }

    /**
     * Uploads content to remote upload session based on the input stream
     *
     * @param callback the progress callback invoked during uploading
     * @param configs  the optional configurations for the upload options. [0] should be the customized chunk
     *                 size and [1] should be the maxRetry for upload retry.
     * @throws IOException the IO exception that occurred during upload
     */
    public OutputStream upload(final IProgressCallback callback, final int... configs) throws IOException {
        return upload(null, callback, configs);
    }
}
