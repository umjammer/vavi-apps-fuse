/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package vavi.nio.file.watch;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vavi.nio.file.googledrive.GoogleDriveFileSystemProvider;
import vavi.util.Debug;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static vavi.nio.file.Base.removeTree;


/**
 * Unit test for WatchService that exercises all methods in various scenarios.
 *
 * @bug 4313887 6838333 7017446 8011537 8042470
 */
@Disabled
public class WatchServiceTest {

    void checkKey(WatchKey key, Path dir) {
        if (!key.isValid())
            throw new RuntimeException("Key is not valid");
        if (key.watchable() != dir)
            throw new RuntimeException("Unexpected watchable");
    }

    void takeExpectedKey(WatchService watcher, WatchKey expected) {
        Debug.println("take events...");
        WatchKey key;
        try {
            key = watcher.take();
        } catch (InterruptedException x) {
            // not expected
            throw new RuntimeException(x);
        }
        if (key != expected)
            throw new RuntimeException("removed unexpected key");
    }

    void checkExpectedEvent(Iterable<WatchEvent<?>> events,
                                   WatchEvent.Kind<?> expectedKind,
                                   Object expectedContext) {
        WatchEvent<?> event = events.iterator().next();
        Debug.printf("got event: type=%s, count=%d, context=%s\n",
            event.kind(), event.count(), event.context());
        if (event.kind() != expectedKind)
            throw new RuntimeException("unexpected event");
        if (!expectedContext.equals(event.context()))
            throw new RuntimeException("unexpected context");
    }

    static FileSystem fs;

    /**
     * Simple test of each of the standard events
     */
    @Test
    void testEvents() throws IOException {
        Debug.println("-- Standard Events --");

        Path name = fs.getPath("foo");

        try (WatchService watcher = fs.newWatchService()) {
            // --- ENTRY_CREATE ---

            // register for event
            Debug.printf("register %s for ENTRY_CREATE\n", dir);
            WatchKey myKey = dir.register(watcher, ENTRY_CREATE);
            checkKey(myKey, dir);

            // create file
            Path file = dir.resolve("foo");
            Debug.printf("create %s\n", file);
            Files.createFile(file);

            // remove key and check that we got the ENTRY_CREATE event
            takeExpectedKey(watcher, myKey);
            checkExpectedEvent(myKey.pollEvents(), ENTRY_CREATE, name);

            Debug.println("reset key");
            if (!myKey.reset())
                throw new RuntimeException("key has been cancalled");

            Debug.println("OKAY");

            // --- ENTRY_DELETE ---

            Debug.printf("register %s for ENTRY_DELETE\n", dir);
            WatchKey deleteKey = dir.register(watcher, ENTRY_DELETE);
            if (deleteKey != myKey)
                throw new RuntimeException("register did not return existing key");
            checkKey(deleteKey, dir);

            Debug.printf("delete %s\n", file);
            Files.delete(file);
            takeExpectedKey(watcher, myKey);
            checkExpectedEvent(myKey.pollEvents(), ENTRY_DELETE, name);

            Debug.println("reset key");
            if (!myKey.reset())
                throw new RuntimeException("key has been cancalled");

            Debug.println("OKAY");

            // create the file for the next test
            Files.createFile(file);

            // --- ENTRY_MODIFY ---

            Debug.printf("register %s for ENTRY_MODIFY\n", dir);
            WatchKey newKey = dir.register(watcher, ENTRY_MODIFY);
            if (newKey != myKey)
                throw new RuntimeException("register did not return existing key");
            checkKey(newKey, dir);

            Debug.printf("update: %s\n", file);
            try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.APPEND)) {
                out.write("I am a small file".getBytes(StandardCharsets.UTF_8));
            }

            // remove key and check that we got the ENTRY_MODIFY event
            takeExpectedKey(watcher, myKey);
            checkExpectedEvent(myKey.pollEvents(), ENTRY_MODIFY, name);
            Debug.println("OKAY");

            // done
            Files.delete(file);
        }
    }

    /**
     * Check that a cancelled key will never be queued
     */
    @Test
    void testCancel() throws IOException {
        Debug.println("-- Cancel --");

        try (WatchService watcher = fs.newWatchService()) {

            Debug.printf("register %s for events\n", dir);
            WatchKey myKey = dir.register(watcher, ENTRY_CREATE);
            checkKey(myKey, dir);

            Debug.println("cancel key");
            myKey.cancel();

            // create a file in the directory
            Path file = dir.resolve("mars");
            Debug.printf("create: %s\n", file);
            Files.createFile(file);

            // poll for keys - there will be none
            Debug.println("poll...");
            try {
                WatchKey key = watcher.poll(3000, TimeUnit.MILLISECONDS);
                if (key != null)
                    throw new RuntimeException("key should not be queued");
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }

            // done
            Files.delete(file);

            Debug.println("OKAY");
        }
    }

    /**
     * Check that deleting a registered directory causes the key to be
     * cancelled and queued.
     */
    @Test
    void testAutomaticCancel() throws IOException {
        Debug.println("-- Automatic Cancel --");

        Path subdir = Files.createDirectory(dir.resolve("bar"));

        try (WatchService watcher = fs.newWatchService()) {

            Debug.printf("register %s for events\n", subdir);
            WatchKey myKey = subdir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            Debug.printf("delete: %s\n", subdir);
            Files.delete(subdir);
            takeExpectedKey(watcher, myKey);

            Debug.println("reset key");
            if (myKey.reset())
                throw new RuntimeException("Key was not cancelled");
            if (myKey.isValid())
                throw new RuntimeException("Key is still valid");

            Debug.println("OKAY");
        }
    }

    /**
     * Asynchronous close of watcher causes blocked threads to wakeup
     */
    @Test
    void testWakeup() throws IOException {
        Debug.println("-- Wakeup Tests --");
        final WatchService watcher = fs.newWatchService();
        Runnable r = () -> {
            try {
                Thread.sleep(5000);
                Debug.println("close WatchService...");
                watcher.close();
            } catch (InterruptedException | IOException x) {
                Debug.printStackTrace(x);
            }
        };

        // start thread to close watch service after delay
        new Thread(r).start();

        try {
            Debug.println("take...");
            watcher.take();
            throw new RuntimeException("ClosedWatchServiceException not thrown");
        } catch (InterruptedException x) {
            throw new RuntimeException(x);
        } catch (ClosedWatchServiceException  x) {
            Debug.println("ClosedWatchServiceException thrown");
        }

        Debug.println("OKAY");
    }

    /**
     * Simple test to check exceptions and other cases
     */
    @Test
    void testExceptions() throws IOException {
        Debug.println("-- Exceptions and other simple tests --");

        WatchService watcher = fs.newWatchService();

        try (watcher) {

            // Poll tests

            WatchKey key;
            Debug.println("poll...");
            key = watcher.poll();
            if (key != null)
                throw new RuntimeException("no keys registered");

            Debug.println("poll with timeout...");
            try {
                long start = System.nanoTime();
                key = watcher.poll(3000, TimeUnit.MILLISECONDS);
                if (key != null)
                    throw new RuntimeException("no keys registered");
                long waited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                if (waited < 2900)
                    throw new RuntimeException("poll was too short");
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }

            // IllegalArgumentException
            Debug.println("IllegalArgumentException tests...");
            assertThrows(IllegalArgumentException.class,
                    () -> dir.register(watcher /*empty event list*/),
                    "IllegalArgumentException not thrown");
            assertThrows(IllegalArgumentException.class,
                    // OVERFLOW is ignored so this is equivalent to the empty set
                    () -> dir.register(watcher, OVERFLOW),
                    "IllegalArgumentException not thrown");
            assertThrows(IllegalArgumentException.class,
                    // OVERFLOW is ignored even if specified multiple times
                    () -> dir.register(watcher, OVERFLOW, OVERFLOW),
                    "IllegalArgumentException not thrown");

            // UnsupportedOperationException
            assertThrows(UnsupportedOperationException.class,
                    () -> dir.register(watcher, new WatchEvent.Kind<>() {
                        @Override public String name() { return "custom"; }
                        @Override public Class<Object> type() { return Object.class; }
                    }),
                    "UnsupportedOperationException not thrown");
            assertThrows(UnsupportedOperationException.class,
                    () -> dir.register(watcher, new WatchEvent.Kind<?>[]{ ENTRY_CREATE }, () -> "custom"),
                    "UnsupportedOperationException not thrown");

            // NullPointerException
            Debug.println("NullPointerException tests...");
            assertThrows(NullPointerException.class,
                    () -> dir.register(null, ENTRY_CREATE),
                    "NullPointerException not thrown");
            assertThrows(NullPointerException.class,
                    () -> dir.register(watcher, new WatchEvent.Kind<?>[]{ null }),
                    "NullPointerException not thrown");
            assertThrows(NullPointerException.class,
                    () -> dir.register(watcher, new WatchEvent.Kind<?>[]{ ENTRY_CREATE }, (WatchEvent.Modifier) null),
                    "NullPointerException not thrown");
        }

        // -- ClosedWatchServiceException --

        Debug.println("ClosedWatchServiceException tests...");

        assertThrows(ClosedWatchServiceException.class, watcher::poll,
                "ClosedWatchServiceException not thrown");

        // assume that poll throws exception immediately
        long start = System.nanoTime();
        assertThrows(ClosedWatchServiceException.class, () -> {
            try {
                watcher.poll(10000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
        }, "ClosedWatchServiceException not thrown");
        long waited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (waited > 5000)
            throw new RuntimeException("poll was too long");

        assertThrows(ClosedWatchServiceException.class, () -> {
            try {
                watcher.take();
            } catch (InterruptedException x) {
                throw new RuntimeException(x);
            }
        }, "ClosedWatchServiceException not thrown");

        assertThrows(ClosedWatchServiceException.class, () -> dir.register(watcher, ENTRY_CREATE), "ClosedWatchServiceException not thrown");

        Debug.println("OKAY");
    }

    /**
     * Test that directory can be registered with more than one watch service
     * and that events don't interfere with each other
     */
    @Test
    void testTwoWatchers() throws IOException {
        Debug.println("-- Two watchers test --");

        try (WatchService watcher1 = fs.newWatchService();
             WatchService watcher2 = fs.newWatchService()) {

            Path name1 = fs.getPath("gus1");
            Path name2 = fs.getPath("gus2");

            // create gus1
            Path file1 = dir.resolve(name1);
            Debug.printf("create %s\n", file1);
            Files.createFile(file1);

            // register with both watch services (different events)
            Debug.println("register for different events");
            WatchKey key1 = dir.register(watcher1, ENTRY_CREATE);
            WatchKey key2 = dir.register(watcher2, ENTRY_DELETE);

            if (key1 == key2)
                throw new RuntimeException("keys should be different");

            // create gus2
            Path file2 = dir.resolve(name2);
            Debug.printf("create %s\n", file2);
            Files.createFile(file2);

            // check that key1 got ENTRY_CREATE
            takeExpectedKey(watcher1, key1);
            checkExpectedEvent(key1.pollEvents(), ENTRY_CREATE, name2);

            // check that key2 got zero events
            WatchKey key = watcher2.poll();
            if (key != null)
                throw new RuntimeException("key not expected");

            // delete gus1
            Files.delete(file1);

            // check that key2 got ENTRY_DELETE
            takeExpectedKey(watcher2, key2);
            checkExpectedEvent(key2.pollEvents(), ENTRY_DELETE, name1);

            // check that key1 got zero events
            key = watcher1.poll();
            if (key != null)
                throw new RuntimeException("key not expected");

            // reset for next test
            key1.reset();
            key2.reset();

            // change registration with watcher2 so that they are both
            // registered for the same event
            Debug.println("register for same event");
            key2 = dir.register(watcher2, ENTRY_CREATE);

            // create file and key2 should be queued
            Debug.printf("create %s\n", file1);
            Files.createFile(file1);
            takeExpectedKey(watcher2, key2);
            checkExpectedEvent(key2.pollEvents(), ENTRY_CREATE, name1);

            Debug.println("OKAY");
        }
    }

    /**
     * Test that thread interruped status is preserved upon a call
     * to register()
     */
    @Test
    void testThreadInterrupt() throws IOException {
        Debug.println("-- Thread interrupted status test --");

        Thread curr = Thread.currentThread();
        try (WatchService watcher = fs.newWatchService()) {
            Debug.println("interrupting current thread");
            curr.interrupt();
            dir.register(watcher, ENTRY_CREATE);
            if (!curr.isInterrupted())
                throw new RuntimeException("thread should remain interrupted");
            Debug.println("current thread is still interrupted");
            Debug.println("OKAY");
        } finally {
            Thread.interrupted();
        }
    }

    static Path dir;

    @BeforeAll
    static void before() throws IOException {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        dir = Files.createTempDirectory(fs.getRootDirectories().iterator().next(), "VAVIFUSE-TEST-WATCHSERVICE");
    }

    @AfterAll
    static void after() throws Exception {
        removeTree(dir);
    }
}
