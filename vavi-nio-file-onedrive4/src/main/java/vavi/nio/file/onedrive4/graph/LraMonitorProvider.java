/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;

import java.io.IOException;
import java.security.InvalidParameterException;

import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.tasks.IProgressCallback;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * LraMonitorProvider service provider
 *
 * @see "https://docs.microsoft.com/en-us/onedrive/developer/rest-api/concepts/long-running-actions?view=odsp-graph-online"
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/08 umjammer initial version <br>
 */
public class LraMonitorProvider<MonitorType> {

    /**
     * The default retry times for a simple chunk upload if failure happened
     */
    private static final int DEFAULT_TIMEOUT = 8;

    /**
     * The client
     */
    private final GraphServiceClient<?> client;

    /**
     * The LRA session URL
     */
    private String monitorUrl;

    /**
     * The LRA response handler
     */
    private final LraMonitorResponseHandler<MonitorType> responseHandler;

    /**
     * The counter for how many bytes have been read from input stream
     */
    private int percentageComplete;

    /**
     * Creates the CopyMonitorProvider
     *
     * @param lraSession the initial LRA session
     * @param client the Graph client
     * @param lraTypeClass the monitor type class
     */
    public LraMonitorProvider(final LraSession lraSession,
            final GraphServiceClient<?> client,
            final Class<MonitorType> lraTypeClass) {
        if (lraSession == null) {
            throw new InvalidParameterException("LRA session is null.");
        }

        if (client == null) {
            throw new InvalidParameterException("OneDrive client is null.");
        }

        this.client = client;
        this.percentageComplete = 0;
        this.monitorUrl = lraSession.getMonitorURL();
        this.responseHandler = new LraMonitorResponseHandler<>();
    }

    /**
     * monitor content to remote session based on the input stream
     *
     * @param callback the progress callback invoked during uploading
     * @param configs the optional configurations for the monitor options. [0]
     *            should be the customized chunk size.
     * @throws IOException the IO exception that occurred during monitor
     */
    public void monitor(final IProgressCallback callback, final int... configs) throws IOException {

        int timeout = DEFAULT_TIMEOUT;

        if (configs.length > 0) {
            timeout = configs[0];
        }

        // Minimum waiting time between requests: 256ms (1/4 s)
        boolean finished = false;

        while (!finished) {

            try {
                Thread.sleep(1 << timeout);
            } catch (InterruptedException e) {
            }

            LraMonitorRequest request = new LraMonitorRequest(this.monitorUrl, this.client);
            LraMonitorResult result = request.monitor(this.responseHandler);

            if (result.monitorDone()) {
Debug.println(StringUtil.paramString(result));
                MonitorObject monitor = result.getMonitorObject();
                if ("completed".equals(monitor.status)) {
                    finished = true;
                    callback.progress(this.percentageComplete, 100);
                    @SuppressWarnings("unchecked")
                    MonitorType driveItem = (MonitorType) client.drive().items(monitor.resourceId).buildRequest().get();
//                    callback.success(driveItem);
                    break;
                } else {
                    callback.progress(this.percentageComplete, 100);
                }
            } else if (result.urlHasChanged()) {
                this.monitorUrl = result.getSeeOtherUrl();
            } else if (result.hasError()) {
                finished = true;
//                callback.failure(result.getError());
                break;
            }

            timeout = Math.min(timeout + 1, 13);
        }
    }
}
