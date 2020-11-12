/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;

import vavi.util.Debug;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;


/**
 * WatchService.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/01 umjammer initial version <br>
 */
public class Main8 {

    public static void main(String[] args) throws Exception {
        WatchService watcher = FileSystems.getDefault().newWatchService();

        Watchable path = Paths.get("./tmp");
        path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        while (true) {
            WatchKey watchKey = watcher.take();

            for (WatchEvent<?> event : watchKey.pollEvents()) {
                Kind<?> kind = event.kind();
                Object context = event.context();
Debug.println("kind: " + kind + ", context: " + context);
            }

            if (!watchKey.reset()) {
                System.out.println("WatchKey has reset");
                return;
            }
        }
    }
}