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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.microsoft.MicrosoftLocalAppCredential;
import vavi.nio.file.Util;
import vavi.nio.file.onedrive.OneDriveFileSystemProvider;
import vavi.util.properties.annotation.PropsEntity;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * onedrive classification
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/03/14 umjammer initial version <br>
 */
public final class Classification3 {

    /**
     * @param args 0: email, 1: dir
     */
    public static void main(final String... args) throws IOException {
        String email = args[0];
        String cwd = args[1];
        boolean dryRun = false;

        URI uri = URI.create("onedrive:///?id=" + email);

        OAuth2AppCredential appCredential = new MicrosoftLocalAppCredential();
        PropsEntity.Util.bind(appCredential);

        Map<String, Object> env = new HashMap<>();
        env.put(OneDriveFileSystemProvider.ENV_APP_CREDENTIAL, appCredential);

        FileSystem onedrivefs = FileSystems.newFileSystem(uri, env);

        Path root = onedrivefs.getPath(cwd);
        FileSearcher fileSearcher = new FileSearcher();
        Files.walkFileTree(root, fileSearcher);
        Pattern pattern = Pattern.compile("\\[(.+?)\\]");
        fileSearcher.result().stream()
            .filter(path -> pattern.matcher(path.getFileName().toString()).find())
            .forEach(path -> {
                try {
                    Matcher matcher = pattern.matcher(Util.toFilenameString(path));
                    String author;
                    if (matcher.find()) {
                        author = matcher.group(1);
                    } else {
                        throw new IllegalStateException("not match: " + path);
                    }

                    Path dir = path.getParent().resolve(author);
                    if (Files.exists(dir)) {
                        System.err.println("mv " + path + " " + dir);
                        if (!dryRun) {
                            Files.move(path, dir.resolve(path.getFileName()));
                        }
                    }
                } catch (IOException f) {
                    System.err.println(f);
                }
            });

        System.exit(0);
    }

    static class FileSearcher extends SimpleFileVisitor<Path> {

        private List<Path> list = new ArrayList<>();

        Pattern pattern = Pattern.compile("[あかさたなはまやらわ]");

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
                if (pattern.matcher(file.getParent().getFileName().toString()).matches()) {
//                    System.err.println(file);
                    list.add(file);
                }
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
            return list;
        }
    }
}
