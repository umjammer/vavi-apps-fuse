/*
 * Copyright (c) 2021 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.stream.StreamSupport;

import vavi.nio.file.googledrive.GoogleDriveFileSystemProvider;
import vavi.util.Debug;

import static com.rainerhahnekamp.sneakythrow.Sneaky.sneaked;
import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * apply something to filtered folders
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2021/10/30 umjammer initial version <br>
 */
public class ShowEmptyFolder {

    public static void main(String[] args) throws IOException {
        ShowEmptyFolder app = new ShowEmptyFolder();
        app.exec(args);
    }

    void exec(String[] args) throws IOException {
        String email = System.getenv("GOOGLE_TEST_ACCOUNT");

        URI uri = URI.create("googledrive:///?id=" + email);
        FileSystem fs = new GoogleDriveFileSystemProvider().newFileSystem(uri, Collections.emptyMap());

        Files.walkFileTree(fs.getPath("/Music/midi"), new MyFileVisitor());

        fs.close();
Debug.println("Done");
    }

    class MyFileVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            try {
                if (filter2(dir)) {
                    func2(dir);
                }
            } catch (IOException e) {
                System.err.println("ERROR: " + dir + ", " + e.getMessage());
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }
    }

    boolean filter1(Path path) throws IOException {
        return Files.list(path).count() == 0;
    }

    boolean filter2(Path path) throws IOException {
        long c1 = Files.list(path).count();

        DirectoryStream<Path> iterable = Files.newDirectoryStream(path, ".DS_Store");
        long c3 = StreamSupport.stream(iterable.spliterator(), false).count();
        iterable.close();

        if (c1 == 0) {
            System.out.println("DIR0: " + path);
            return true;
        } else if (c1 == c3) {
            System.out.println("DIR1X " + path);
            return true;
        } else {
            return false;
        }
    }

    void func1(Path path) throws IOException {
        System.out.println("DIR0: " + path);
    }

    void func2(Path path) throws IOException {
        Files.list(path).forEach(sneaked(Files::delete));
        Files.delete(path);
    }
}