
package vavi.nio.file.dropbox;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.github.fge.fs.dropbox.DropBoxFileSystemProvider;

import vavi.util.properties.annotation.PropsEntity;

import static vavi.nio.file.Base.testMoveFolder;


/**
 * dropbox move folder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/21 umjammer initial version <br>
 */
@PropsEntity(url = "file://${HOME}/.vavifuse/dropbox/{0}")
public final class Main3 {

    @Test
    void test01() throws Exception {
        String email = System.getenv("DROPBOX_TEST_ACCOUNT");

        URI uri = URI.create("dropbox:///?id=" + email);
        FileSystem fs = new DropBoxFileSystemProvider().newFileSystem(uri, Collections.EMPTY_MAP);

        testMoveFolder(fs);
    }
}
