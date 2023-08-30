/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Collections;
import java.util.List;

import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Test1.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/27 umjammer initial version <br>
 */
public class Test1 {

    @Test
    public void test() {
        String location = "https://login.live.com/oauth20_authorize.srf?client_id=0000000040184284&scope=wl.offline_access&response_type=code&redirect_uri=https%3A%2F%2Fvast-plateau-97564.herokuapp.com%2Fonedrive_set";
        String url = "https://login.live.com/oauth20_authorize.srf";
        assertTrue(location.indexOf(url) == 0);

        String location2 = "https://vast-plateau-97564.herokuapp.com/onedrive_set?code=M2739c1c0-460c-2ac5-8e94-f9b8fdf3dd5c";
        String redirectUrl = "https://vast-plateau-97564.herokuapp.com/onedrive_set";
        assertTrue(location2.indexOf(redirectUrl) == 0);
    }

    @Test
    public void test2() {
        SecurityManager security = System.getSecurityManager();
        if (security == null) {
Debug.println("no security manager");
            return;
        }
        try {
            security.checkPermission(new RuntimePermission("shutdownHooks"));
        } catch (final SecurityException e) {
            fail();
        }
    }

    @Test
    void test01() {
Debug.printf("file: %1$s, %2$tF %2$tT.%2$tL, %3$d\n", "a", System.currentTimeMillis(), 1);
    }

    @Test
    void test04() {
        String m = "{\"@vavi\":" + 123456L + "}";
        long o = Long.parseLong(m.substring(9, m.length() - 1));
        assertEquals(123456L, o);
    }

    @Test
    void test05() throws Exception {
        Path path = Paths.get("/");
        boolean r = Files.getFileStore(path).supportsFileAttributeView(UserDefinedFileAttributeView.class);
Debug.println("support user attr view: " + r);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
    void test06() throws Exception {
        Path path = Paths.get("/Volumes/GoogleDrive/Books/Comics");
        UserDefinedFileAttributeView userDefinedFAView = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
        if (userDefinedFAView != null) {
            List<String> attributes = userDefinedFAView.list();
attributes.forEach(Debug::println);
        }
    }

    @SuppressWarnings("restriction")
    @Test
    void test07() throws Exception {
        Path src = Paths.get(Test1.class.getResource("/test.zip").toURI());
        Path dir = Paths.get("tmp");
        Files.createDirectories(dir);
        Path target = dir.resolve(src.getFileName());
        Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
        URI uri = URI.create("jar:" + target.toUri());
Debug.println("uri: " + uri);
        assertThrows(FileSystemNotFoundException.class, () -> {
            // first time uri is not instance of ZipPath
            // cause not found
            // @see "https://stackoverflow.com/a/25033217"
            FileSystems.getFileSystem(uri);
        });

        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
Debug.println("fs: " + fs.getClass().getName());
        assertInstanceOf(com.sun.nio.zipfs.ZipFileSystem.class, fs);

        // why second time pass test?
        fs = FileSystems.getFileSystem(uri);
Debug.println("fs: " + fs.getClass().getName());
        assertInstanceOf(com.sun.nio.zipfs.ZipFileSystem.class, fs);
    }

    @Test
    void test08() throws Exception {
        SAXBuilder builder = new SAXBuilder();
        XPathFactory factory = XPathFactory.instance();
        Path p = Paths.get(Test1.class.getResource("/log4j2.xml").toURI());
        org.jdom2.Document document = builder.build(Files.newInputStream(p));
Debug.println("conatiner: " + document);

        XPathExpression<org.jdom2.Element> expression =
                factory.compile("/Configuration", Filters.element());
        org.jdom2.Element element = expression.evaluateFirst(document);
Debug.println("element: " + element);
        assertEquals("Configuration", element.getName());

        expression = factory.compile("/Configuration/Loggers/Root/AppenderRef", Filters.element());
        element = expression.evaluateFirst(document);
Debug.println("element: " + element);
        assertEquals("AppenderRef", element.getName());

        expression = factory.compile("*", Filters.element());
        element = expression.evaluateFirst(document);
Debug.println("element: " + element);
        assertEquals("Configuration", element.getName());
    }
}

/* */
