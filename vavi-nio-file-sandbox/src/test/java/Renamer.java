/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * renamer
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/14 umjammer initial version <br>
 */
public final class Renamer {

    /**
     * @param args 0: dir, 1: regex, 2: replacement
     */
    public static void main(String[] args) throws IOException {
        String cwd = args[0];
        String regex = args[1];
        String replacement = args[2];

        Path root = Paths.get(cwd);
        FileRenamer.Replacer replacer = new RegexReplacer(regex, replacement);
        FileRenamer fileRenamer = new FileRenamer(replacer);
        Files.walkFileTree(root, fileRenamer);
        fileRenamer.exec(false);
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

        public interface Replacer {
            boolean find(String source);
            String replace(String source);
        }

        private final Replacer replacer;

        FileRenamer(Replacer replacer) {
            this.replacer = replacer;
        }

        private static class Pair {
            Pair(Path source, Path target) {
                this.source = source;
                this.target = target;
            }
            Path source;
            Path target;
        }

        private final List<Pair> list = new ArrayList<>();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isSymbolicLink()) {
//                System.err.format("Symbolic link: %s ", file);
            } else if (attr.isRegularFile()) {
                String name = file.getFileName().toString();
                String newName = replacer.replace(name);
                if (replacer.find(name)) {
                    System.out.format("mv '%s' '%s'\n", newName, name);  // for reverting
                    list.add(new Pair(file, file.resolveSibling(newName)));
                }
            } else {
//                System.err.format("Other        : %s ", file);
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
//            System.err.format("Directory    : %s%n", dir);
            return CONTINUE;
        }

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
