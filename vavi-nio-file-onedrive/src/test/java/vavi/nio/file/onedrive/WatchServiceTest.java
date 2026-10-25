/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests for {@link OneDriveWatchService}.
 * <p>
 * needs the webhook server (vavi-apps-webhook) and the environment variables
 * described at {@link OneDriveWatchService}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2026/10/25 umjammer initial version <br>
 */
@Disabled("needs the webhook server")
public class WatchServiceTest {

    /** notification via webhook takes a while */
    static final long TIMEOUT_SECONDS = 60;

    static FileSystem fs;

    static Path basePath;

    WatchService watcher;

    @BeforeAll
    static void beforeAll() throws IOException {
        String email = System.getenv("MICROSOFT_TEST_ACCOUNT");

        URI uri = URI.create("onedrive1:///?id=" + email);
        fs = new OneDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        basePath = Files.createTempDirectory(fs.getRootDirectories().iterator().next(), "VAVIFUSE-TEST-WATCHSERVICE-");
    }

    @AfterAll
    static void afterAll() throws IOException {
        Files.delete(basePath);
        fs.close();
    }

    @BeforeEach
    void setUp() throws IOException {
        watcher = fs.newWatchService();
    }

    @AfterEach
    void tearDown() throws IOException {
        watcher.close();
    }

    @Test
    void testNewWatcher() {
        assertTrue(watcher instanceof OneDriveWatchService);
    }

    @Test
    void testCreate() throws Exception {
        WatchKey key = basePath.register(watcher, ENTRY_MODIFY);
        assertTrue(key.isValid());

        Path file = basePath.resolve("created.txt");
        Files.write(file, "created".getBytes());
        try {
            WatchKey signalled = watcher.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertNotNull(signalled);
            assertSame(key, signalled);
            assertTrue(signalled.reset());
        } finally {
            Files.delete(file);
        }
    }

    @Test
    void testDelete() throws Exception {
        Path file = basePath.resolve("deleted.txt");
        Files.write(file, "deleted".getBytes());

        WatchKey key = basePath.register(watcher, ENTRY_DELETE);

        Files.delete(file);

        WatchKey signalled = watcher.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull(signalled);
        assertSame(key, signalled);
    }

    @Test
    void testTwoWatchers() throws Exception {
        // a notification is shared by watch services, every service should be notified
        try (WatchService watcher2 = fs.newWatchService()) {
            WatchKey key1 = basePath.register(watcher, ENTRY_MODIFY);
            WatchKey key2 = basePath.register(watcher2, ENTRY_MODIFY);

            Path file = basePath.resolve("two.txt");
            Files.write(file, "two".getBytes());
            try {
                assertSame(key1, watcher.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                assertSame(key2, watcher2.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } finally {
                Files.delete(file);
            }
        }
    }

    @Test
    void testClose() throws Exception {
        WatchKey key = basePath.register(watcher, ENTRY_MODIFY);
        assertTrue(key.isValid());

        watcher.close();

        assertFalse(key.isValid());
    }
}
