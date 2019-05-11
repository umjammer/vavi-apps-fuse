
package vavi.net.fuse.box;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

import com.github.fge.filesystem.box.provider.BoxFileSystemProvider;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static java.nio.file.FileVisitResult.CONTINUE;


@PropsEntity(url = "file://${HOME}/.vavifuse/box/{0}")
public final class Main3 {

    @Property(name = "box.accessToken")
    private String accessToken;

    public static void main(final String... args) throws IOException {
        String email = args[0];

        Main3 app = new Main3();
        PropsEntity.Util.bind(app, email);

        /*
         * Create the necessary elements to create a filesystem.
         * Note: the URI _must_ have a scheme of "box", and
         * _must_ be hierarchical.
         */
        final URI uri = URI.create("box://foo/");
        final Map<String, String> env = new HashMap<>();
        env.put("accessToken", app.accessToken);

        /*
         * Create the FileSystemProvider; this will be more simple once
         * the filesystem is registered to the JRE, but right now you
         * have to do like that, sorry...
         */
        final FileSystemProvider provider = new BoxFileSystemProvider();

        try (/* Create the filesystem... */
            final FileSystem boxfs = provider.newFileSystem(uri, env)) {

            /* And use it! You should of course adapt this code... */
            // Equivalent to FileSystems.getDefault().getPath(...)
//            final Path src = Paths.get(System.getProperty("user.home") + "/tmp/2" , "java7.java");
            // Here we create a path for our Box fs...
//            final Path dst = boxfs.getPath("/java7.java");
            // Here we copy the file from our local fs to box!
//            Files.copy(src, dst);

            Path root = boxfs.getRootDirectories().iterator().next();
            Files.walkFileTree(root, new PrintFiles());
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
