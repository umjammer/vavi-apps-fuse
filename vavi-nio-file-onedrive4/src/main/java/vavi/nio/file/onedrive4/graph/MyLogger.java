/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive4.graph;

import java.util.logging.Logger;

import com.microsoft.graph.logger.ILogger;
import com.microsoft.graph.logger.LoggerLevel;


/**
 * The logger for the service client
 */
public class MyLogger implements ILogger {

    /**
     * The logging level
     */
    private LoggerLevel level = LoggerLevel.DEBUG;

    private final static Logger LOGGER = Logger.getLogger(MyLogger.class.getName());

    /**
     * Sets the logging level of this logger
     * 
     * @param level the level to log at
     */
    public void setLoggingLevel(final LoggerLevel level) {
        LOGGER.info("Setting logging level to " + level);
        this.level = level;
    }

    /**
     * Gets the logging level of this logger
     * 
     * @return the level the logger is set to
     */
    public LoggerLevel getLoggingLevel() {
        return level;
    }

    /**
     * Creates the tag automatically
     * 
     * @return the tag for the current method Sourced from
     *         https://gist.github.com/eefret/a9c7ac052854a10a8936
     */
    private String getTag() {
        try {
            final StringBuilder sb = new StringBuilder();
            final int callerStackDepth = 4;
            final String className = Thread.currentThread().getStackTrace()[callerStackDepth].getClassName();
            sb.append(className.substring(className.lastIndexOf('.') + 1));
            sb.append("[");
            sb.append(Thread.currentThread().getStackTrace()[callerStackDepth].getMethodName());
            sb.append("] - ");
            sb.append(Thread.currentThread().getStackTrace()[callerStackDepth].getLineNumber());
            return sb.toString();
        } catch (final Exception ex) {
            LOGGER.warning(ex.getMessage());
        }
        return null;
    }

    /**
     * Logs a debug message
     * 
     * @param message the message
     */
    @Override
    public void logDebug(final String message) {
        if (this.level == LoggerLevel.DEBUG)
            for (final String line : message.split("\n")) {
                LOGGER.info(line);
            }
    }

    /**
     * Logs an error message with throwable
     * 
     * @param message the message
     * @param throwable the throwable
     */
    @Override
    public void logError(final String message, final Throwable throwable) {
        switch (level) {
        case DEBUG:
        case ERROR:
        default:
            for (final String line : message.split("\n")) {
                LOGGER.severe(getTag() + line);
            }
            LOGGER.severe("Throwable detail: " + throwable);
        }
    }
}
