/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vavix.util.screenscrape.annotation.JsonPathParser;
import vavix.util.screenscrape.annotation.PlainInputHandler;
import vavix.util.screenscrape.annotation.Target;
import vavix.util.screenscrape.annotation.WebScraper;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * cloud drive description adder
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/09/08 umjammer initial version <br>
 * @see "https://manablog.org/google-books-apis/"
 */
public final class Descriptor {

    /**
     * @param args 0: email, 1: dir
     */
    public static void main(String[] args) throws IOException {
        String email = args[0];
        String cwd = args[1];

        URI uri = URI.create("onedrive:///?id=" + email);

        try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.EMPTY_MAP)) {

            Path root = fs.getPath(cwd);
            FileSearcher fileSearcher = new FileSearcher();
            Files.walkFileTree(root, fileSearcher);

//            String query = args[2];
//            Result result = query(query);
//System.err.println(result);

        }
//Thread.getAllStackTraces().keySet().forEach(System.err::println);
    }

    @WebScraper(url = "https://www.googleapis.com/books/v1/volumes?q={0}",
            parser = JsonPathParser.class,
            input = PlainInputHandler.class,
            value = "$..items",
            isDebug = false,
            isCollection = false)
    public static class Result {
        @Target(value = "$.volumeInfo.title")
        String title;
        @Target(value = "$.volumeInfo.authors", optional = true)
        List<String> authors;
        @Target(value = "$.volumeInfo.publishedDate", optional = true)
        String publishedDate;
        @Target(value = "$.volumeInfo.description", optional = true)
        String description;
        @Target(value = "$.volumeInfo.industryIdentifiers[0].identifier", optional = true)
        String isbn10;
        @Target(value = "$.volumeInfo.industryIdentifiers[1].identifier", optional = true)
        String isbn13;
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Result [title=");
            builder.append(title);
            builder.append(", authors=");
            builder.append(authors);
            builder.append(", publishedDate=");
            builder.append(publishedDate);
            builder.append(", description=");
            builder.append(description);
            builder.append(", isbn10=");
            builder.append(isbn10);
            builder.append(", isbn13=");
            builder.append(isbn13);
            builder.append("]");
            return builder.toString();
        }
        public String toFormatedString() {
            StringBuilder builder = new StringBuilder();
            builder.append(authors == null ? "[]" : authors);
            builder.append(" ");
            builder.append(title);
            builder.append("\n");
            builder.append(publishedDate == null ? "" : publishedDate);
            builder.append("\n");
            if (isbn10 != null) {
                builder.append(isbn10);
                builder.append("\n");
            }
            if (isbn13 != null) {
                builder.append(isbn13);
                builder.append("\n");
            }
            builder.append("\n");
            builder.append(description == null ? "" : description);
            return builder.toString();
        }
    }

    static Result query(String query) throws IOException {
         return WebScraper.Util.scrape(Result.class, query).get(0);
    }

    static class FileSearcher extends SimpleFileVisitor<Path> {

        static final Pattern pattern = Pattern.compile("^\\(一般小説\\)\\s\\[(.+?)\\]\\s(.+?)(\\(.+?\\)){0,1}\\..+$");

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
                String filename = file.getFileName().toString();
//System.err.println(filename);
                Matcher matcher = pattern.matcher(filename);
                if (matcher.find()) {
//System.out.println(matcher.group(1) + " - " + matcher.group(2));
                    try {
                        byte[] bytes = (byte[]) Files.getAttribute(file, "user:description");
                        if (bytes == null || bytes.length == 0) {

                            Result result = query(matcher.group(1) + " " + matcher.group(2));
                            String description = result.toFormatedString();
System.out.println(description + "\n\n");
                            Files.setAttribute(file, "user:description", description.getBytes());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
System.err.println(file);
exc.printStackTrace();
            return CONTINUE;
        }
    }
}
