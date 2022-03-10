/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.watch.webhook;

import java.io.Closeable;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;
import java.util.function.Consumer;


/**
 * Notification.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/06 umjammer initial version <br>
 */
public interface Notification<T> extends Closeable {

    static ServiceLoader<NotificationProvider> providers = ServiceLoader.load(NotificationProvider.class);

    /**
     * @param matcher will be compared by {@link String#contains(CharSequence)}
     */
    static <T> Notification<T> getNotification(String matcher, Consumer<T> callback, Object... args) throws IOException {
        for (NotificationProvider provider : providers) {
            if (provider.getClass().getName().contains(matcher)) {
                return provider.getNotification(callback, args);
            }
        }
        throw new NoSuchElementException(matcher);
    }
}

/* */
