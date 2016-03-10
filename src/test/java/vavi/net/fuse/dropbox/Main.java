
package vavi.net.fuse.dropbox;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

import com.github.fge.filesystem.provider.FileSystemRepository;
import com.github.fge.fs.dropbox.provider.DropBoxFileSystemProvider;
import com.github.fge.fs.dropbox.provider.DropBoxFileSystemRepository;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


@PropsEntity(url = "file://${HOME}/.vavifuse/dropbox/{0}")
public final class Main {
    
    @Property(name = "dropbox.accessToken")
    private String accessToken;

    public static void main(final String... args) throws IOException {
        String email = args[0];

        Main app = new Main();
        PropsEntity.Util.bind(app, email);
        
        /* 
         * Create the necessary elements to create a filesystem.
         * Note: the URI _must_ have a scheme of "dropbox", and
         * _must_ be hierarchical.
         */
        final URI uri = URI.create("dropbox://foo/");
        final Map<String, String> env = new HashMap<>();
        env.put("accessToken", app.accessToken);

        /*
         * Create the FileSystemProvider; this will be more simple once
         * the filesystem is registered to the JRE, but right now you
         * have to do like that, sorry...
         */
        final FileSystemRepository repository = new DropBoxFileSystemRepository();
        final FileSystemProvider provider = new DropBoxFileSystemProvider(repository);

        try (/* Create the filesystem... */
            final FileSystem dropboxfs = provider.newFileSystem(uri, env)) {
            /* And use it! You should of course adapt this code... */
            // Equivalent to FileSystems.getDefault().getPath(...)
            final Path src = Paths.get(System.getProperty("user.home") + "/tmp/2" , "java7.java");
            // Here we create a path for our DropBox fs...
            final Path dst = dropboxfs.getPath("/java7.java");
            // Here we copy the file from our local fs to dropbox!
            Files.copy(src, dst);
        }
    }
}
