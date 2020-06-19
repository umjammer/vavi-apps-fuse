package vavi.nio.file.box;

import java.net.URI;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.github.fge.filesystem.box.BoxFileSystemProvider;

import static vavi.nio.file.Base.testAll;


/**
 * Main. (box)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/11 umjammer initial version <br>
 */
public class Main {

    @Test
    void test01() throws Exception {
        String email = System.getenv("BOX_TEST_ACCOUNT");

        URI uri = URI.create("box:///?id=" + email);

        testAll(new BoxFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP));
    }
}