
package vavi.nio.file.dropbox;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * dropbox nio walk.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/21 umjammer initial version <br>
 */
@PropsEntity(url = "file://${HOME}/.vavifuse/dropbox/{0}")
public final class Main3 {

    @Property(name = "dropbox.accessToken")
    private String accessToken;

    public static void main(final String... args) throws IOException {
        String email = args[0];

        Main3 app = new Main3();
        PropsEntity.Util.bind(app, email);

        // Create the necessary elements to create a filesystem.
        // Note: the URI _must_ have a scheme of "dropbox", and
        // _must_ be hierarchical.
        URI uri = URI.create("dropbox:///");

        final Map<String, String> env = new HashMap<>();
        env.put("accessToken", app.accessToken);

        // Create the filesystem...
        try (FileSystem dropboxfs = FileSystems.newFileSystem(uri, env)) {

            // And use it! You should of course adapt this code...
            // Equivalent to FileSystems.getDefault().getPath(...)
            final Path src = Paths.get(System.getProperty("user.home") + "/tmp/2" , "java7.java");
            // Here we create a path for our DropBox fs...
            final Path dst = dropboxfs.getPath("/java7.java");
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

//            Path root = dropboxfs.getRootDirectories().iterator().next();
//            Files.walkFileTree(root, new PrintFiles());
        }
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
