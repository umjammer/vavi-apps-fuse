/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

import com.github.fge.filesystem.provider.FileSystemRepository;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * onedrive nio file walk
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/20 umjammer initial version <br>
 */
public final class Main3 {

    public static void main(final String... args) throws IOException {
        String email = args[0];

        /*
         * Create the necessary elements to create a filesystem.
         * Note: the URI _must_ have a scheme of "onedrive", and
         * _must_ be hierarchical.
         */
        final URI uri = URI.create("onedrive://foo/");
        final Map<String, String> env = new HashMap<>();
        env.put("email", email);

        /*
         * Create the FileSystemProvider; this will be more simple once
         * the filesystem is registered to the JRE, but right now you
         * have to do like that, sorry...
         */
        final FileSystemRepository repository = new OneDriveFileSystemRepository();
        final FileSystemProvider provider = new OneDriveFileSystemProvider(repository);

        try (/* Create the filesystem... */
            final FileSystem onedrivefs = provider.newFileSystem(uri, env)) {

            /* And use it! You should of course adapt this code... */
            // Equivalent to FileSystems.getDefault().getPath(...)
            final Path src = Paths.get(System.getProperty("user.home") + "/tmp/2" , "java7.java");
            // Here we create a path for our DropBox fs...
            final Path dst = onedrivefs.getPath("/java7.java");
            // Here we copy the file from our local fs to dropbox!
            try {
System.out.println("$ list");
                Files.list(dst.getParent()).forEach(System.out::println);
System.out.println("$ copy");
                Files.copy(src, dst);
            } catch (FileAlreadyExistsException e) {
//e.printStackTrace(System.out);
System.err.println(e);
System.out.println("$ delete");
                Files.delete(dst);
System.out.println("$ list");
                Files.list(dst.getParent()).forEach(System.out::println);
System.out.println("$ copy");
                Files.copy(src, dst);
            }
System.out.println("$ list");
            Files.list(dst.getParent()).forEach(System.out::println);

//            Path root = onedrivefs.getRootDirectories().iterator().next();
//            Files.walkFileTree(root, new PrintFiles());
        }

        System.exit(0);
    }

    static class PrintFiles extends SimpleFileVisitor<Path> {

        // Print information about
        // each type of file.
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isSymbolicLink()) {
                System.out.format("Symbolic link: %s ", file);
            } else if (attr.isRegularFile()) {
                System.out.format("Regular file : %s ", file);
            } else {
                System.out.format("Other        : %s ", file);
            }
            System.out.println("(" + attr.size() + "bytes)");
            return CONTINUE;
        }

        // Print each directory visited.
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            System.out.format("Directory    : %s%n", dir);
            return CONTINUE;
        }

        // If there is some error accessing
        // the file, let the user know.
        // If you don't override this method
        // and an error occurs, an IOException
        // is thrown.
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }
    }
}
