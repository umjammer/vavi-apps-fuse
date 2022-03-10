/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * onedrive classification
 * <p>
 * if an author has three more novels, create author folder and move those into there.
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/14 umjammer initial version <br>
 */
public final class Classification {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod",
                           "com\\.microsoft\\.graph\\.logger\\.DefaultLogger#logDebug");
    }

    /**
     * @param args 0: email, 1: dir
     */
    public static void main(String[] args) throws Exception {
        Classification app = new Classification(args[0]);
        app.exec(args[1]);
    }

    boolean dryRun = true;

    FileSystem fs;

    Classification(String email) throws IOException {

        URI uri = URI.create("onedrive:///?id=" + email);

        fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
    }

    void exec(String cwd) throws IOException {
        Path root = fs.getPath(cwd);
        Files.walkFileTree(root, new MyFileVisitor1());
System.err.println("\ndone counting");
        exec2();
System.err.println("done");
    }

    class MyFileVisitor1 extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
System.err.print(".");
                if (filter1(file)) {
                    func1(file);
                    targets.add(file);
                }
            }
            return CONTINUE;
        }
    }

    static final Pattern pattern1 = Pattern.compile("[あかさたなはまやらわ]");

    // novels parent is one of ka sa ta na ... only
    boolean filter1(Path file) {
        return pattern1.matcher(file.getParent().getFileName().toString()).matches();
    }

    static final Pattern pattern2 = Pattern.compile("\\[(.+?)\\]");

    Map<String, Integer> counter = new HashMap<>();

    List<Path> targets = new ArrayList<>();

    /** counter */
    void func1(Path file) {
        Matcher matcher = pattern2.matcher(file.getFileName().toString());
        if (matcher.find()) {
            String author = matcher.group(1);
            if (counter.get(author) == null) {
                counter.put(author, 1);
            } else {
                counter.put(author, counter.get(author) + 1);
            }
        }
    }

    void exec2() {
        targets.stream()
            .forEach(file -> {
                Matcher matcher = pattern2.matcher(file.getFileName().toString());
                if (matcher.find()) {
                    func2(file, matcher.group(1));
                }
            });
    }

    /** */
    void func2(Path file, String author) {
        try {
            if (counter.get(author) >= 3) {
System.err.println("author " + author);
                Path dir = file.getParent().resolve(author);
                if (!Files.exists(dir)) {
System.err.println("mkdir " + dir);
                    if (!dryRun) {
                            Files.createDirectory(dir);
                    }
                }

System.err.println("mv " + file + " " + dir);
                if (!dryRun) {
                    Files.move(file, dir.resolve(file.getFileName()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
