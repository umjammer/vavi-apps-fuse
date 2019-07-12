package vavi.nio.file.box;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.fge.filesystem.box.provider.BoxFileSystemProvider;

import vavi.net.auth.oauth2.BasicAppCredential;
import vavi.net.auth.oauth2.box.BoxLocalAppCredential;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.nio.file.Base.testAll;

import co.paralleluniverse.javafs.JavaFS;


/**
 * Main. (box)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/11 umjammer initial version <br>
 */
public class Main {

    /**
     * @param args 0: mount point, 1: email
     */
    public static void main(final String... args) throws IOException {
        String email = args[1];

        BasicAppCredential appCredential = new BoxLocalAppCredential();
        PropsEntity.Util.bind(appCredential);

        // Create the necessary elements to create a filesystem.
        // Note: the URI _must_ have a scheme of "box", and
        // _must_ be hierarchical.
        URI uri = URI.create("box:///?id=" + email);

        final Map<String, Object> env = new HashMap<>();
        env.put(BoxFileSystemProvider.ENV_CREDENTIAL, appCredential);
        env.put("ignoreAppleDouble", true);

        final FileSystem fs = new BoxFileSystemProvider().newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "box_fs" + "@" + System.currentTimeMillis());

        JavaFS.mount(fs, Paths.get(args[0]), true, true, options);
    }

    @Test
    void test01() throws Exception {
        String email = "umjammer@gmail.com";

        URI uri = URI.create("box:///?id=" + email);

        BasicAppCredential appCredential = new BoxLocalAppCredential();
        PropsEntity.Util.bind(appCredential);

        Map<String, Object> env = new HashMap<>();
        env.put(BoxFileSystemProvider.ENV_CREDENTIAL, appCredential);

        testAll(new BoxFileSystemProvider().newFileSystem(uri, env));
    }
}