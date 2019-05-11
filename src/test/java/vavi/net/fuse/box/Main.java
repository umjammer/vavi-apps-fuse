package vavi.net.fuse.box;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;

import com.box.sdk.BoxAPIConnection;
import com.github.fge.filesystem.box.provider.BoxFileSystemProvider;

import vavi.net.fuse.Getter;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import co.paralleluniverse.javafs.JavaFS;


/**
 * <pre>
 * HOWTO
 *
 * * get developer token
 *  https://app.box.com/developers/services/edit/216798
 * * edit properties file
 *  $ vi ~/.vavifuse/box/developer
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/11/11 umjammer initial version <br>
 */
@PropsEntity(url = "file://${HOME}/.vavifuse/box/{0}")
public class Main {

    @Property(name = "box.accessToken")
    private String accessToken;

    @PropsEntity(url = "file://${HOME}/.vavifuse/box.properties")
    public static class BoxAuth {
        @Property(name = "box.clientId")
        private transient String clientId;
        @Property(name = "box.clientSecret")
        private transient String clientSecret;
    }

    @PropsEntity(url = "file://${HOME}/.vavifuse/credentials.properties")
    public static class BoxUser {
        @Property(name = "box.password.{0}")
        private transient String password;
    }

    /**
     * @param args 0: mount point, 1: email
     */
    public static void main(final String... args) throws IOException {
        String email = args[1];

        Main app = new Main();
        try {
            PropsEntity.Util.bind(app, email);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
//            Getter getter = new BoxFxGetter(email);
//            app.accessToken = getter.get("https://account.box.com/api/oauth2/authorize");
            BoxAuth auth = new BoxAuth();
            PropsEntity.Util.bind(auth, email);
            BoxAPIConnection api = new BoxAPIConnection(auth.clientId, auth.clientSecret, "zlqxcyc51blrq9rkzzy0ji3bbz4odx0i");
        }

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

        final FileSystem fs = provider.newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "box_fs" + "@" + System.currentTimeMillis());

        JavaFS.mount(fs, Paths.get(args[0]), false, true, options);
    }
}