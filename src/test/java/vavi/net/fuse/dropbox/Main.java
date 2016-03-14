package vavi.net.fuse.dropbox;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

import com.github.fge.filesystem.provider.FileSystemRepository;
import com.github.fge.fs.dropbox.provider.DropBoxFileSystemProvider;
import com.github.fge.fs.dropbox.provider.DropBoxFileSystemRepository;

import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import co.paralleluniverse.javafs.JavaFS;

@PropsEntity(url = "file://${HOME}/.vavifuse/dropbox/{0}")
public class Main {
    
    @Property(name = "dropbox.accessToken")
    private String accessToken;

    public static void main(final String... args) throws IOException {
        String email = args[1];

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

        final FileSystem fs = provider.newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "dropbox_fs" + "@" + System.currentTimeMillis());
            
        JavaFS.mount(fs, Paths.get(args[0]), false, true, options);
    }
}