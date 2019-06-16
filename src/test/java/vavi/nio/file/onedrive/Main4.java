/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.nio.file.onedrive;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.github.fge.filesystem.provider.FileSystemRepository;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * onedrive renamer
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/14 umjammer initial version <br>
 */
public final class Main4 {

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

        try (FileSystem onedrivefs = provider.newFileSystem(uri, env)) {

            Path root = onedrivefs.getRootDirectories().iterator().next();
            FileRenamer.Replacer replacer = new RegexReplacer("\\ \\ ", " ");
            FileRenamer fileRenamer = new FileRenamer(replacer);
            Files.walkFileTree(root, fileRenamer);
            fileRenamer.exec(true);
        }

        System.exit(0);
    }

    static class RegexReplacer implements FileRenamer.Replacer {

        String regex;
        String replacement;

        Pattern pattern;

        RegexReplacer(String regex, String replacement) {
            this.regex = regex;
            this.replacement = replacement;
            pattern = Pattern.compile(regex);
        }

        @Override
        public boolean find(String source) {
            return pattern.matcher(source).find();
        }

        @Override
        public String replace(String source) {
            return source.replaceAll(regex, replacement);
        }
    }

    static class FileRenamer extends SimpleFileVisitor<Path> {

        interface Replacer {
            boolean find(String source);
            String replace(String source);
        }

        Replacer replacer;

        FileRenamer(Replacer replacer) {
            this.replacer = replacer;
        }

        class Pair {
            Pair(Path source, Path target) {
                this.source = source;
                this.target = target;
            }
            Path source;
            Path target;
        }

        List<Pair> list = new ArrayList<>();

        // Print information about
        // each type of file.
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isSymbolicLink()) {
                System.err.format("Symbolic link: %s ", file);
            } else if (attr.isRegularFile()) {
                String name = file.getFileName().toString();
                String newName = replacer.replace(name);
                if (replacer.find(name)) {
                    System.out.format("mv '%s' '%s'\n", name, newName);
                    list.add(new Pair(file, file.resolveSibling(newName)));
                }
            } else {
                System.err.format("Other        : %s ", file);
            }
            return CONTINUE;
        }

        // Print each directory visited.
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            System.err.format("Directory    : %s%n", dir);
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

        public void exec(boolean isDryRun) {
            list.forEach(pair -> {
                try {
                    if (!isDryRun) {
                        Files.move(pair.source, pair.target);
                    }
                    System.err.print(".");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            System.err.println("\nDone");
        }
    }
}