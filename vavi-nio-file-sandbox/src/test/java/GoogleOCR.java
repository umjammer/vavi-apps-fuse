/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import vavi.nio.file.googledrive.GoogleDriveCopyOption;
import vavi.nio.file.googledrive.GoogleDriveOpenOption;
import vavi.util.archive.Entry;
import vavi.util.archive.zip.JdkZipArchive;


/**
 * google drive ocr
 *
 * TODO Google Vision API
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/03/03 umjammer initial version <br>
 */
public final class GoogleOCR {

    /**
     * @param args 0: email, 1: zip file, 2: extract to dir, 3: extracted dir, 4: output ocr dir
     */
    public static void main(String[] args) {
        int exitCode = 0;
        try {
            GoogleOCR app = new GoogleOCR();
            FileSystem fs = app.prepare(args);
System.err.println("fs: " + fs);
            app.extract(fs, args);
            app.ocr(fs, args);
            app.gather(fs, args);
        } catch (Exception e) {
            e.printStackTrace();
            exitCode = -1;
        } finally {
//Thread.getAllStackTraces().keySet().forEach(System.err::println);
            System.exit(exitCode);
        }
    }

    /**
     * @param args 0: email
     */
    FileSystem prepare(final String... args) throws IOException {
        String email = args[0];

        URI uri = URI.create("googledrive:///?id=" + email);

        return FileSystems.newFileSystem(uri, Collections.emptyMap());
    }

    /**
     * @param args 1: zip file, 2: extract to dir
     */
    void extract(FileSystem fs, final String... args) throws IOException {
        String filename = args[1];
        String dirname = args[2];
//        boolean dryRun = false;

        Path dir = fs.getPath(dirname);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path file = fs.getPath(filename);
        Path tmp = Paths.get("/tmp", file.getFileName().toString());
        if (!Files.exists(tmp)) {
            Files.copy(file, tmp);
System.err.println("copy: " + file + " to " + tmp);
        }
        JdkZipArchive archive = new JdkZipArchive(tmp.toFile());
        for (Entry entry : archive.entries()) {
            Path path = dir.resolve(entry.getName());
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (!Files.exists(path)) {
                if (entry.isDirectory()) {
                    Files.createDirectory(path);
                } else {
                    Files.copy(archive.getInputStream(entry), path);
                }
System.err.println("extract: " + path);
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            } else {
System.err.println("skip: " + path);
            }
        }
        Files.delete(tmp);
System.err.println("rm: " + tmp);
    }

    /**
     * @param args 3: extracted dir, 4: output ocr dir
     */
    void ocr(FileSystem fs, final String... args) throws IOException {
        String extractedDirname = args[3];
        // specify other directory avoid ConcurrentModificationException Files#list().forEach()
        String ocrDirname = args[4];
        Files.list(fs.getPath(extractedDirname)).forEach(p -> {
            try {
                String fn = p.getFileName().toString();
                if (fn.indexOf(".jpg") > 0) {
                    // gdocs doesn't have extension
                    String ocrName = fn.substring(0, fn.lastIndexOf('.')) + "_ocr"/* + fn.substring(fn.lastIndexOf('.')) */;
                    Path ocr = fs.getPath(ocrDirname).resolve(ocrName);
                    if (!Files.exists(ocr)) {
                        Path x = Files.copy(p, ocr, GoogleDriveCopyOption.EXPORT_AS_GDOCS);
System.err.println("ocr: " + x);
                        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    } else {
System.err.println("skip ocr: " + fn);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    /**
     * @param args 4: output ocr dir
     */
    void gather(FileSystem fs, final String... args) throws IOException {
        String ocrDirname = args[4];
        Files.list(fs.getPath(ocrDirname)).sorted().forEach(p -> {
            try {
                ZipInputStream zis = new ZipInputStream(Files.newInputStream(p, GoogleDriveOpenOption.EXPORT_WITH_GDOCS_DOCX));
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                     if (entry.getName().equals("word/document.xml")) {
                         Scanner scanner = new Scanner(zis);
                         while (scanner.hasNextLine()) {
                             String line = scanner.nextLine();
                             line = line.replaceAll("</w:p>", "\n");
                             line = line.replaceAll("<[^>]{1,}>", "");
                             line = line.replaceAll("[^[\\p{Print}]\\n]{1,}", ""); // TODO check
                             System.out.println(line);
                         }
                     }
                     zis.closeEntry();
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
