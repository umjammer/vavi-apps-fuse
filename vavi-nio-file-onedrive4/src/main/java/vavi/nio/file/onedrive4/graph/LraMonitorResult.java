/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;

import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;

/**
 * Wrapper class for different monitor response from server.
 */
public class LraMonitorResult {
    /**
     * The monitor item response.
     */
    private final MonitorObject monitorObject;

    /**
     * The next session response.
     */
    private final LraSession session;

    /**
     * The error happened during monitor.
     */
    private final ClientException error;

    /** */
    private String seeOtherUtl;

    /**
     * Construct result with item created.
     *
     * @param monitor The created item.
     */
    public LraMonitorResult(MonitorObject monitor) {
        this.monitorObject = monitor;
        this.session = null;
        this.error = null;
        this.seeOtherUtl = null;
    }

    /**
     * Construct result with next session.
     *
     * @param session The next session.
     */
    public LraMonitorResult(LraSession session) {
        this.session = session;
        this.monitorObject = null;
        this.error = null;
        this.seeOtherUtl = null;
    }

    /**
     * Construct result with error.
     *
     * @param error The error occurred during monitor.
     */
    public LraMonitorResult(ClientException error) {
        this.error = error;
        this.monitorObject = null;
        this.session = null;
        this.seeOtherUtl = null;
    }

    /**
     * Construct result with new url.
     *
     * @param seeOtherUtl The new url;
     */
    public LraMonitorResult(String seeOtherUtl) {
        this.seeOtherUtl = seeOtherUtl;
        this.monitorObject = null;
        this.session = null;
        this.error = null;
    }

    /**
     * Construct result ok.
     */
    public LraMonitorResult() {
        this.seeOtherUtl = null;
        this.monitorObject = null;
        this.session = null;
        this.error = null;
    }

    /**
     * Construct result with server exception.
     *
     * @param exception The exception received from server.
     */
    public LraMonitorResult(GraphServiceException exception) {
        this(new ClientException(exception.getMessage(/* verbose */ true), exception));
    }

    /**
     * Checks the whole monitor is completed.
     *
     * @return true if the response is an item.
     */
    public boolean monitorDone() {
        return this.monitorObject != null;
    }

    /**
     * Checks the monitor url has changed.
     *
     * @return true if the url has changed.
     */
    public boolean urlHasChanged() {
        return this.seeOtherUtl != null;
    }

    /**
     * Gets the new monitor url.
     *
     * @return the new url.
     */
    public String getSeeOtherUrl() {
        return this.seeOtherUtl;
    }

    /**
     * Checks if an error happened.
     *
     * @return true if current request has error.
     */
    public boolean hasError() {
        return this.error != null;
    }

    /**
     * Get the monitor item.
     *
     * @return The item.
     */
    public MonitorObject getMonitorObject() {
        return this.monitorObject;
    }

    /**
     * Get the next session.
     *
     * @return The next session for monitoring.
     */
    public LraSession getSession() {
        return this.session;
    }

    /**
     * Get the error.
     *
     * @return The error.
     */
    public ClientException getError() {
        return this.error;
    }
}