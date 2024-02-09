/*
 * https://github.com/bbejeck/Java-7/blob/cf0256be2122d9063e21743fb01bd19f07feef69/src/test/java/bbejeck/nio/files/watch/WatchDirectoryTest.java
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.nio.file.Base.removeTree;

/**
 * WatchDirectoryTest.
 *
 * @author bbejeck
 * @version 2/13/12 9:47 PM
 */
@Disabled
public class WatchDirectoryTest {

    static Path dir1Path;

    private WatchService watchService;
    private WatchKey basePathWatchKey;

    static FileSystem fs;

    static Path basePath;

    @BeforeAll
    static void before() throws IOException {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        basePath = Files.createTempDirectory(fs.getRootDirectories().iterator().next(), "VAVIFUSE-TEST-WATCHSERVICE-");
        dir1Path = basePath.resolve("dir1");
    }

    @AfterAll
    static void afterAll() throws Exception {
        Files.delete(basePath);
    }

    @BeforeEach
    public void setUo() throws Exception {
        watchService = fs.newWatchService();
        basePathWatchKey = basePath.register(watchService, ENTRY_CREATE);
    }

    @AfterEach
    public void after() throws Exception {
        watchService.close();
        removeTree(basePath, false);
    }

    @Test
    public void testEventForDirectory() throws Exception {
        generateFile(basePath.resolve("newTextFile.txt"), 10);
        generateFile(basePath.resolve("newTextFileII.txt"), 10);
        generateFile(basePath.resolve("newTextFileIII.txt"), 10);
        WatchKey watchKey = watchService.poll(20, TimeUnit.SECONDS);
        assertNotNull(watchKey);
        assertEquals(basePathWatchKey, watchKey);
        List<WatchEvent<?>> eventList = watchKey.pollEvents();
        assertEquals(3, eventList.size());
        for (WatchEvent<?> event : eventList) {
            assertSame(event.kind(), ENTRY_CREATE);
            assertEquals(1, event.count());
        }
        Path eventPath = (Path) eventList.get(0).context();
        assertTrue(Files.isSameFile(eventPath, Paths.get("newTextFile.txt")));
        Path watchedPath = (Path) watchKey.watchable();
        assertTrue(Files.isSameFile(watchedPath, basePath));
    }

    @Test
    public void testEventForDirectoryWatchKey() throws Exception {
        generateFile(basePath.resolve("newTextFile.txt"), 10);
        List<WatchEvent<?>> eventList = basePathWatchKey.pollEvents();
        while (eventList.size() == 0 ){
            eventList = basePathWatchKey.pollEvents();
            Thread.sleep(10000);
        }
        assertEquals(1, eventList.size());
        for (WatchEvent<?> event : eventList) {
            assertSame(event.kind(), ENTRY_CREATE);
        }
        basePathWatchKey.reset();
        generateFile(basePath.resolve("newTextFileII.txt"), 10);
        generateFile(basePath.resolve("newTextFileIII.txt"), 10);
        while (eventList.size() == 0 ){
            eventList = basePathWatchKey.pollEvents();
            Thread.sleep(10000);
        }
        Path eventPath = (Path) eventList.get(0).context();
        assertTrue(Files.isSameFile(eventPath, Paths.get("newTextFile.txt")));
        Path watchedPath = (Path) basePathWatchKey.watchable();
        assertTrue(Files.isSameFile(watchedPath, basePath));
    }

    @Test
    public void testEventForSubDirectory() throws Exception {
        dir1Path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        generateFile(basePath.resolve("newTextFile.txt"), 10);
        generateFile(dir1Path.resolve("newTextFile.txt"), 10);
        int count = 0;
        while (count < 2) {
            WatchKey watchKey = watchService.poll(20, TimeUnit.SECONDS);
            @SuppressWarnings("unused")
            Path watchedPath = (Path) watchKey.watchable();
            assertNotNull(watchKey);
            List<WatchEvent<?>> eventList = watchKey.pollEvents();
            WatchEvent<?> event = eventList.get(0);
            assertEquals(1, event.count());
            assertSame(event.kind(), ENTRY_CREATE);
            assertTrue(Files.isSameFile((Path) event.context(), Paths.get("newTextFile.txt")));
            count++;
        }
    }

    private static final String LINE_OF_TEXT = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do " +
            "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad " +
            "minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea " +
            "commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit" +
            " esse cillum dolore eu fugiat nulla pariatur. " +
            "Excepteur sint occaecat cupidatat non proident, sunt " +
            "in culpa qui officia deserunt mollit anim id est laborum.";

    public static void generateFile(Path path, int lines) throws Exception {
        try(PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(path, Charset.defaultCharset()))){
            for(int i = 0; i< lines; i++){
                printWriter.println(LINE_OF_TEXT);
            }
        }
    }
}
