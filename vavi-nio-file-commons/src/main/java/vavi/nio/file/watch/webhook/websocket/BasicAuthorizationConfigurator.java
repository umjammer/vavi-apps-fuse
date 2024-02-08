/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.watch.webhook.websocket;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;


/**
 * BasicAuthorizationConfigurator.
 * <p>
 * environment variables
 * <ul>
 * <li>VAVI_APPS_WEBHOOK_USERNAME
 * <li>VAVI_APPS_WEBHOOK_PASSWORD
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/07 umjammer initial version <br>
 */
public class BasicAuthorizationConfigurator extends ClientEndpointConfig.Configurator {

    private static final String username = System.getenv("VAVI_APPS_WEBHOOK_USERNAME");
    private static final String password = System.getenv("VAVI_APPS_WEBHOOK_PASSWORD");

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        headers.put("Authorization", Collections.singletonList("Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes())));
    }
}

/* */
