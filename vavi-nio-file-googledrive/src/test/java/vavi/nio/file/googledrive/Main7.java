/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.googledrive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import vavi.nio.file.googledrive.GoogleDriveUserDefinedFileAttributesProvider.RevisionsUtil;
import vavi.util.Debug;

import static java.nio.file.FileVisitResult.CONTINUE;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * GoogleDrive attribute user:revision.
 *
 * TODO upload a file as new revision
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2021/10/30 umjammer initial version <br>
 */
public class Main7 {

    @Test
    void test01() throws Exception {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        FileSystem fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        Path src = Paths.get(Main7.class.getResource("/Hello.java").toURI());
        Path dst = fs.getPath("/tmp/vavi.nio.file.googledrive.Main7-" + System.currentTimeMillis());

        // revision 1
        Files.copy(src, dst);

        Thread.sleep(100);

        // revision 2
        // copy options don't be passed to the upload method of file system driver...
        // and OutputStream#close() is important
        try (OutputStream os = Files.newOutputStream(dst, GoogleDriveOpenOption.IMPORT_AS_NEW_REVISION)) {
            Files.copy(src, os);
        }

        byte[] in = (byte[]) Files.getAttribute(dst, "user:revisions");
        String[] revisions = RevisionsUtil.split(in);
        assertEquals(2, revisions.length);
System.err.println(dst);
Arrays.stream(revisions).forEach(System.err::println);

        // revision latest
        byte[] out = RevisionsUtil.getLatestOnly(in);
        Files.setAttribute(dst, "user:revisions", out);

        in = (byte[]) Files.getAttribute(dst, "user:revisions");
        revisions = RevisionsUtil.split(in);
        assertEquals(1, revisions.length);
System.err.println("---- latest ---");
Arrays.stream(revisions).forEach(System.err::println);

        // clean up
        Files.delete(dst);

        fs.close();
    }

    //

    public static void main(String[] args) throws IOException {
        t1(args);
    }

    /** list */
    static void t2(String[] args) throws IOException {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        FileSystem fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        Files.list(fs.getPath("/tmp")).forEach(p -> {
            try {
                if (!Files.isDirectory(p)) {
                    System.err.println(p + "\n" + new String((byte[]) Files.getAttribute(p, "user:revisions")));
                }
            } catch (IOException e) {
                System.err.println(e);
            }
        });

        fs.close();
    }

    /** remove all remaining latest */
    static void t3(String[] args) throws IOException {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        FileSystem fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());
        Path path = fs.getPath("/tmp/heroku-blog-duke.png");

System.err.println("--- before ---");
        byte[] in = (byte[]) Files.getAttribute(path, "user:revisions");
        byte[] out = RevisionsUtil.getLatestOnly(in);
System.err.println("--- write ---");
System.err.println(new String(out));
        Files.setAttribute(path, "user:revisions", out);

System.err.println("--- after ---");
        in = (byte[]) Files.getAttribute(path, "user:revisions");
        String[] as = RevisionsUtil.split(in);
Arrays.stream(as).forEach(System.err::println);

        fs.close();
    }

    static class MyFileVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
                try {
                    int size = RevisionsUtil.size(Files.getAttribute(file, "user:revisions"));
                    if (size > 1) {
                        System.err.println(file + ": " + size);

System.err.println("--- before ---");
                        byte[] in = (byte[]) Files.getAttribute(file, "user:revisions");
                        byte[] out = RevisionsUtil.getLatestOnly(in);
System.err.println("--- write ---");
System.err.println(new String(out));
                        Files.setAttribute(file, "user:revisions", out);

System.err.println("--- after ---");
                        in = (byte[]) Files.getAttribute(file, "user:revisions");
                        String[] as = RevisionsUtil.split(in);
Arrays.stream(as).forEach(System.err::println);
System.err.println("\n");
                    }
                } catch (IOException e) {
                    System.err.println(e);
                }
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }
    }

    /** remove all remaining latest, tree */
    static void t1(String[] args) throws IOException {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        FileSystem fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        Files.walkFileTree(fs.getPath("/Books/Comics/"), new MyFileVisitor());

        fs.close();
Debug.println("Done");
    }
}