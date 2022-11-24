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
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * author directory classification
 * <p>
 * if an author has three more novels, create author folder and move those into there.
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/14 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public final class Classification {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod",
                           "com\\.microsoft\\.graph\\.logger\\.DefaultLogger#logDebug");
    }

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "epubDir")
    String epubDir = "src/test/resources";

    @BeforeEach
    void setup() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    void test() throws Exception {
Debug.println(epubDir);
        Classification app = new Classification();
        app.exec(Paths.get(epubDir));
    }

    /**
     * onedrive
     * @param args 0: email, 1: dir
     */
    public static void main(String[] args) throws Exception {
        String email = args[0];
        String cwd = args[1];

        URI uri = URI.create("onedrive:///?id=" + email);

        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

        Path root = fs.getPath(cwd);

        Classification app = new Classification();
        app.exec(root);

        fs.close();
    }

    boolean dryRun = true;

    /** entry point */
    void exec(Path root) throws IOException {
        Files.walkFileTree(root, new MyFileVisitor1());
System.err.println("\ndone counting");
        exec2();                                 // <-------------- ③ classify
System.err.println("done");
    }

    /** list targets */
    class MyFileVisitor1 extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
System.err.print(".");
                if (filter3(file)) {             // <-------------- ① filtering
                    func1(file);                 // <-------------- ② function
                    targets.add(file);
                }
            }
            return CONTINUE;
        }
    }

    /** for filter1 */
    static final Pattern pattern1 = Pattern.compile("[あかさたなはまやらわ]");

    /** ① novels parent is one of { "a", "ka", "sa", "ta", "na", ... } only */
    boolean filter1(Path file) {
        return pattern1.matcher(file.getParent().getFileName().toString()).matches();
    }

    /** ① epub only */
    boolean filter2(Path file) {
        return file.getFileName().toString().endsWith(".epub");
    }

    /** ① specified dir only */
    boolean filter3(Path file) {
        return file.getParent().toString().equals(epubDir);
    }

    /** extract author */
    static final Pattern pattern2 = Pattern.compile("\\[(.+?)\\]");

    /** author contents counter */
    Map<String, Integer> counter = new HashMap<>();

    /** targets to classify */
    List<Path> targets = new ArrayList<>();

    /** normalize author (remove white spaces) */
    static String normalizeAuthor(String author) {
        return author.replaceAll("\\s", "");
    }

    /** ② counter */
    void func1(Path file) {
        Matcher matcher = pattern2.matcher(file.getFileName().toString());
        if (matcher.find()) {
            String author = normalizeAuthor(matcher.group(1));
            counter.merge(author, 1, Integer::sum);
        }
    }

    /** ③ do classify */
    void exec2() {
        targets.forEach(file -> {
            Matcher matcher = pattern2.matcher(file.getFileName().toString());
            if (matcher.find()) {
                String author = normalizeAuthor(matcher.group(1));
                func2(file, author);
            }
        });
    }

    /** do classify for each */
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
                    Files.move(file, dir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
