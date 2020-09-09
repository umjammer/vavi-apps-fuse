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
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vavi.util.CharNormalizerJa;

import static java.nio.file.FileVisitResult.CONTINUE;

import net.java.sen.StringTagger;
import net.java.sen.Token;


/**
 * classification
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/14 umjammer initial version <br>
 */
public final class Classification2 {

    private static final String[] table = {
        "あいうえお",
        "かきくけこがぎぐげご",
        "さしすせそざじずぜそ",
        "たちつてとだぢづでど",
        "なにぬねの",
        "はひふへほばびぶべぼぱぴぷぺぽ",
        "まみむめも",
        "やゆよ",
        "らりるれろ",
        "わをん",
    };

    /**
     * @param args 0: dir
     */
    public static void main(String[] args) throws IOException {
        String cwd = args[0];
        boolean dryRun = true;

        Path root = Paths.get(cwd);
        FileSearcher fileSearcher = new FileSearcher(root);
        Files.walkFileTree(root, fileSearcher);
        Pattern pattern = Pattern.compile("\\[(.+?)\\]");
        fileSearcher.result().forEach(path -> {
            try {
                for (String s : table) {
                    Path dir = root.resolve(s.substring(0, 1));
                    if (!Files.exists(dir)) {
//System.err.println("mkdir " + dir);
                        if (!dryRun) {
                            Files.createDirectory(dir);
                        }
                    }
                }

                String reading;
                if (Files.isDirectory(path)) {
                    reading = toKana(path.getFileName().toString());
                } else {
                    Matcher matcher = pattern.matcher(path.getFileName().toString());
                    if (matcher.find()) {
                        reading = toKana(matcher.group(1));
                    } else {
                        throw new IllegalStateException(path.toString());
                    }
                }

                char kana = CharNormalizerJa.ToHiragana.normalize(reading).charAt(0);
//System.err.println(reading + ", " + kana);

                char c = Arrays.stream(table)
                    .filter(s -> s.indexOf(kana) >= 0)
                    .map(s -> s.charAt(0))
                    .findFirst().get();

                Path dir = root.resolve(String.valueOf(c));
                System.err.println("mv " + path.getFileName() + " " + dir.getFileName());
                if (!dryRun) {
                    Files.move(path, dir.resolve(path.getFileName()));
                }
            } catch (NoSuchElementException f) {
                System.err.println(f.getMessage() + ": " + path);
            } catch (IOException f) {
                System.err.println("ERROR: " + f);
                throw new IllegalStateException(f);
            }
        });
    }

    private static String toKana(String text) throws IOException{
        StringBuilder sb = new StringBuilder();
        StringTagger tagger = StringTagger.getInstance();
        Token[] token = tagger.analyze(text);
        if (token != null) {
            for (int i = 0; i < token.length; i++) {
//System.err.println(token[i].toString() + "\t("
//           + token[i].getBasicString() + ")" + "\t" + token[i].getPos()
//           + "(" + token[i].start() + "," + token[i].end() + ","
//           + token[i].length() + ")\t" + token[i].getReading() + "\t"
//           + token[i].getPronunciation());
                if (token[i].getReading() != null && !token[i].getPos().startsWith("記号")) {
                    sb.append(token[i].getReading());
                }
            }
        }
//System.err.println(sb);
        return sb.length() == 0 ? text : sb.toString();
    }

    static class FileSearcher extends SimpleFileVisitor<Path> {

        private List<Path> list = new ArrayList<>();

        Path root;

        Pattern patternF = Pattern.compile("\\[(.+?)\\]");
        Pattern patternD = Pattern.compile("[あかさたなはまやらわ]");

        FileSearcher(Path root) {
            this.root = root;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
                if (file.getParent().equals(root)) {
                    if (patternF.matcher(file.getFileName().toString()).find()) {
//                        System.err.println(file);
                        list.add(file);
                    }
                }
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            if (dir.getParent().equals(root)) {
                if (!patternD.matcher(dir.getFileName().toString()).matches()) {
//                    System.err.println(dir);
                    list.add(dir);
                }
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }

        List<Path> result() {
            return list;
        }
    }
}
