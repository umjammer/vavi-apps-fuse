package vavi.nio.file.onedrive.test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

import com.github.fge.filesystem.provider.FileSystemRepository;

import co.paralleluniverse.javafs.JavaFS;


public class Main {
    
    public static void main(final String... args) throws IOException {
        String email = args[1];

        /* 
         * Create the necessary elements to create a filesystem.
         * Note: the URI _must_ have a scheme of "dropbox", and
         * _must_ be hierarchical.
         */
        final URI uri = URI.create("onedrive://foo/");
        final Map<String, String> env = new HashMap<>();
        env.put("email", email);

        /*
         * Create the FileSystemProvider; this will be more simple once
         * the filesystem is registered to the JRE, but right now you
         * have to do like that, sorry...
         */
        final FileSystemRepository repository = new OneDriveFileSystemRepository();
        final FileSystemProvider provider = new OneDriveFileSystemProvider(repository);

        final FileSystem fs = provider.newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "onedrive_fs" + "@" + System.currentTimeMillis());
            
        JavaFS.mount(fs, Paths.get(args[0]), false, true, options);
    }
}