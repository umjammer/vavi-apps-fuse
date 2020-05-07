package vavi.nio.file.box;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.fge.filesystem.box.provider.BoxFileSystemProvider;

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

        final Map<String, Object> env = new HashMap<>();
        env.put("ignoreAppleDouble", true);

        URI uri = URI.create("box:///?id=" + email);

        final FileSystem fs = new BoxFileSystemProvider().newFileSystem(uri, env);

        Map<String, String> options = new HashMap<>();
        options.put("fsname", "box_fs" + "@" + System.currentTimeMillis());

        JavaFS.mount(fs, Paths.get(args[0]), true, true, options);
    }

    @Test
    void test01() throws Exception {
        String email = System.getenv("BOX_TEST_ACCOUNT");

        URI uri = URI.create("box:///?id=" + email);

        testAll(new BoxFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP));
    }
}