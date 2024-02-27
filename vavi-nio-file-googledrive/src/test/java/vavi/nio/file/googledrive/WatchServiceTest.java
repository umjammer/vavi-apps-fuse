/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.fge.filesystem.watch.AbstractWatchService;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests for {@link GoogleDriveWatchService}.
 *
 * @author Colin Decker
 */
@Disabled
public class WatchServiceTest {

    private FileSystem fs;

    private AbstractWatchService watcher;

    @BeforeEach
    public void setUp() throws IOException {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        watcher = (AbstractWatchService) fs.newWatchService();
    }

    @AfterEach
    public void tearDown() throws IOException {
        watcher.close();
        fs.close();
        watcher = null;
        fs = null;
    }

    @Test
    public void testNewWatcher() {
//      assertTrue(watcher.isOpen());
//    assertFalse(watcher.isPolling());
    }

    @Test
    public void testRegister() throws IOException {
        WatchKey key = watcher.register(createDirectory(), ImmutableList.of(ENTRY_CREATE));
        assertTrue(key.isValid());

//    assertTrue(watcher.isPolling());
    }

    @Test
    public void testRegister_fileDoesNotExist() throws IOException {
        assertThrows(NoSuchFileException.class, () -> watcher.register(fs.getPath("/a/b/c"), ImmutableList.of(ENTRY_CREATE)));
    }

    @Test
    public void testRegister_fileIsNotDirectory() throws IOException {
        Path path = fs.getPath("/a.txt");
        Files.createFile(path);
        assertThrows(NoSuchFileException.class, () -> watcher.register(path, ImmutableList.of(ENTRY_CREATE)));
    }

    @Test
    public void testCancellingLastKeyStopsPolling() throws IOException {
        WatchKey key = watcher.register(createDirectory(), ImmutableList.of(ENTRY_CREATE));
        key.cancel();
        assertFalse(key.isValid());

//    assertFalse(watcher.isPolling());

        WatchKey key2 = watcher.register(createDirectory(), ImmutableList.of(ENTRY_CREATE));
        WatchKey key3 = watcher.register(createDirectory(), ImmutableList.of(ENTRY_DELETE));

//    assertTrue(watcher.isPolling());

        key2.cancel();

//    assertTrue(watcher.isPolling());

        key3.cancel();

//    assertFalse(watcher.isPolling());
    }

    @Test
    public void testCloseCancelsAllKeysAndStopsPolling() throws IOException {
        WatchKey key1 = watcher.register(createDirectory(), ImmutableList.of(ENTRY_CREATE));
        WatchKey key2 = watcher.register(createDirectory(), ImmutableList.of(ENTRY_DELETE));

        assertTrue(key1.isValid());
        assertTrue(key2.isValid());
//    assertTrue(watcher.isPolling());

        watcher.close();

        assertFalse(key1.isValid());
        assertFalse(key2.isValid());
//    assertFalse(watcher.isPolling());
    }

    @Test
    @Timeout(30000)
    public void testWatchForOneEventType() throws IOException, InterruptedException {
        Path path = createDirectory();
        watcher.register(path, ImmutableList.of(ENTRY_CREATE));

        Files.createFile(path.resolve("foo"));

        assertWatcherHasEvents(HackedAbstractWatchService.createWatchEvent(ENTRY_CREATE, 1, fs.getPath("foo")));

        Files.createFile(path.resolve("bar"));
        Files.createFile(path.resolve("baz"));

        assertWatcherHasEvents(HackedAbstractWatchService.createWatchEvent(ENTRY_CREATE, 1, fs.getPath("bar")),
                               HackedAbstractWatchService.createWatchEvent(ENTRY_CREATE, 1, fs.getPath("baz")));
    }

    @Test
    @Timeout(30000)
    public void testWatchForMultipleEventTypes() throws IOException, InterruptedException {
        Path path = createDirectory();
        watcher.register(path, ImmutableList.of(ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));

        Files.createDirectory(path.resolve("foo"));
        Files.createFile(path.resolve("bar"));

        assertWatcherHasEvents(HackedAbstractWatchService.createWatchEvent(ENTRY_CREATE, 1, fs.getPath("bar")),
                               HackedAbstractWatchService.createWatchEvent(ENTRY_CREATE, 1, fs.getPath("foo")));

        Files.createFile(path.resolve("baz"));
        Files.delete(path.resolve("bar"));
        Files.createFile(path.resolve("foo/bar"));

        assertWatcherHasEvents(HackedAbstractWatchService.createWatchEvent(ENTRY_CREATE, 1, fs.getPath("baz")),
                               HackedAbstractWatchService.createWatchEvent(ENTRY_DELETE, 1, fs.getPath("bar")),
                               HackedAbstractWatchService.createWatchEvent(ENTRY_MODIFY, 1, fs.getPath("foo")));

        Files.delete(path.resolve("foo/bar"));
        ensureTimeToPoll(); // watcher polls, seeing modification, then polls
                            // again, seeing delete
        Files.delete(path.resolve("foo"));

        assertWatcherHasEvents(HackedAbstractWatchService.createWatchEvent(ENTRY_MODIFY, 1, fs.getPath("foo")),
                               HackedAbstractWatchService.createWatchEvent(ENTRY_DELETE, 1, fs.getPath("foo")));

        Files.createDirectories(path.resolve("foo/bar"));

        // polling here may either see just the creation of foo, or may first
        // see the creation of foo
        // and then the creation of foo/bar (modification of foo) since those
        // don't happen atomically
        assertWatcherHasEvents(ImmutableList.of(HackedAbstractWatchService.createWatchEvent(ENTRY_CREATE, 1, fs.getPath("foo"))),
                // or
                ImmutableList.of(HackedAbstractWatchService.createWatchEvent(ENTRY_CREATE, 1, fs.getPath("foo")),
                                 HackedAbstractWatchService.createWatchEvent(ENTRY_MODIFY, 1, fs.getPath("foo"))));

        Files.delete(path.resolve("foo/bar"));
        Files.delete(path.resolve("foo"));

        // polling here may either just see the deletion of foo, or may first
        // see the deletion of bar
        // (modification of foo) and then the deletion of foo
        assertWatcherHasEvents(ImmutableList.of(HackedAbstractWatchService.createWatchEvent(ENTRY_DELETE, 1, fs.getPath("foo"))),
                // or
                ImmutableList.of(HackedAbstractWatchService.createWatchEvent(ENTRY_MODIFY, 1, fs.getPath("foo")),
                                 HackedAbstractWatchService.createWatchEvent(ENTRY_DELETE, 1, fs.getPath("foo"))));
    }

    private void assertWatcherHasEvents(WatchEvent<?>... events) throws InterruptedException {
        assertWatcherHasEvents(Arrays.asList(events), ImmutableList.of());
    }

    private void assertWatcherHasEvents(List<WatchEvent<?>> expected,
                                        List<WatchEvent<?>> alternate) throws InterruptedException {
        ensureTimeToPoll(); // otherwise we could read 1 event but not all the events we're expecting
        WatchKey key = watcher.take();
        List<WatchEvent<?>> keyEvents = key.pollEvents();

        if (keyEvents.size() == expected.size() || alternate.isEmpty()) {
            assertIterableEquals(expected, keyEvents);
        } else {
            assertIterableEquals(alternate, keyEvents);
        }
        key.reset();
    }

    private static void ensureTimeToPoll() {
        Uninterruptibles.sleepUninterruptibly(40, MILLISECONDS);
    }

    private Path createDirectory() throws IOException {
        Path path = fs.getPath("/" + UUID.randomUUID());
        Files.createDirectory(path);
        return path;
    }

    static class HackedAbstractWatchService extends AbstractWatchService {
        public static <T> WatchEvent<T> createWatchEvent(Kind<T> kind, int count, T context) {
            return new BasicWatchEvent<>(kind, count, context);
        }
    }
}
