
package vavi.nio.file.box;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import com.github.fge.filesystem.box.BoxFileSystemProvider;

import vavi.net.auth.oauth2.OAuth2AppCredential;
import vavi.net.auth.oauth2.box.BoxLocalAppCredential;
import vavi.util.properties.annotation.PropsEntity;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * Main3.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/11 umjammer initial version <br>
 */
public final class Main3 {

    public static void main(final String... args) throws IOException {
        String email = args[0];

        OAuth2AppCredential appCredential = new BoxLocalAppCredential();
        PropsEntity.Util.bind(appCredential);

        /*
         * Create the necessary elements to create a filesystem.
         * Note: the URI _must_ have a scheme of "box", and
         * _must_ be hierarchical.
         */
        final URI uri = URI.create("box:///?id=" + email);

        final Map<String, Object> env = new HashMap<>();
        env.put(BoxFileSystemProvider.ENV_APP_CREDENTIAL, appCredential);

        // Create the filesystem...
        try (final FileSystem boxfs = new BoxFileSystemProvider().newFileSystem(uri, env)) {

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
