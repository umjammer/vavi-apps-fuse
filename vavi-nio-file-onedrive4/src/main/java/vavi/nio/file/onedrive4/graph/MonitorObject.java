/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.microsoft.graph.serializer.AdditionalDataManager;
import com.microsoft.graph.serializer.IJsonBackedObject;
import com.microsoft.graph.serializer.ISerializer;

import static java.lang.System.getLogger;


/**
 * MonitorObject.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/08 umjammer initial version <br>
 */
public class MonitorObject implements IJsonBackedObject  {

    private static final Logger logger = getLogger(MonitorObject.class.getName());

    @SerializedName("operation")
    @Expose
    String operation;
    @SerializedName("percentageComplete")
    @Expose
    float percentageComplete;
    @SerializedName("resourceId")
    @Expose
    String resourceId;
    @SerializedName("status")
    @Expose
    String status;

    @Override
    public void setRawObject(ISerializer serializer, JsonObject json) {
logger.log(Level.DEBUG, json);
    }

    private final transient AdditionalDataManager additionalDataManager = new AdditionalDataManager(this);

    @Override
    public final AdditionalDataManager additionalDataManager() {
        return additionalDataManager;
    }
}
