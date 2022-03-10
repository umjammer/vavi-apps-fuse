/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.watch.webhook;

import java.io.IOException;
import java.util.function.Consumer;


/**
 * NotificationProvider.
 * <p>
 * for example, ngrok provider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/07 umjammer initial version <br>
 */
public interface NotificationProvider {

    <T> Notification<T> getNotification(Consumer<T> callback, Object... args) throws IOException;
}

/* */
