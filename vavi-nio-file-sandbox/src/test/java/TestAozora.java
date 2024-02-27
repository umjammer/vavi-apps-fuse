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

import org.klab.commons.csv.CsvColumn;
import org.klab.commons.csv.CsvEntity;
import org.klab.commons.csv.impl.FileCsvFactory;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * TestAozora.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/20 umjammer initial version <br>
 */
public class TestAozora {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String cwd = args[0];
        boolean dryRun = true;

        List<AozoraDatabase> result = CsvEntity.Util.read(AozoraDatabase.class);
//System.out.println("csv: " + result.size());
//result.forEach(System.err::println);

        Path root = Paths.get(cwd);
        MyFileVisitor fileSearcher = new MyFileVisitor();
        Files.walkFileTree(root, fileSearcher);
        fileSearcher.result().stream().filter(path -> {
            final String name = path.getFileName().toString();
//          return list.stream().anyMatch(l -> name.indexOf(l.getAuthor()) > 0 && name.indexOf(l.title) > 0);
            return result.stream().anyMatch(l -> name.indexOf(l.getAuthor()) > 0);
        }).forEach(path -> {
            try {
                System.out.println("rm '" + path + "'");
                if (!dryRun) {
                    Files.delete(path);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @CsvEntity(url = "tmp/list_person_all_extended.csv", io = FileCsvFactory.class, encoding = "Windows-31J")
    public static class AozoraDatabase {
        @CsvColumn(sequence = 16)
        String familyName;
        @CsvColumn(sequence = 17)
        String firstName;
        @CsvColumn(sequence = 2)
        String title;
        public String toString() {
            return "[" + familyName + firstName + "] " + title;
        }
        public String getAuthor() {
            return familyName + firstName;
        }
    }

    static class MyFileVisitor extends SimpleFileVisitor<Path> {

        private final List<Path> list = new ArrayList<>();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
                list.add(file);
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

        public List<Path> result() {
//System.out.println("files: " + list.size());
            return list;
        }
    }
}

/* */
