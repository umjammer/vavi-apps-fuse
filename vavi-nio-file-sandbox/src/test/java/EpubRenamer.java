/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavi.util.serdes.Element;
import vavi.util.serdes.JSoupBeanBinder;
import vavi.util.serdes.SaxonXPathBeanBinder;
import vavi.util.serdes.Serdes;


/**
 * EpubRenamer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-11-07 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class EpubRenamer {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "epubDir")
    String epubDir = "src/test/resources";

    @BeforeEach
    void setup() throws IOException {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    void test() throws Exception {
        main(new String[] {epubDir});
    }

    /**
     * @param args 0: dir
     */
    public static void main(String[] args) throws Exception {
        EpubRenamer app = new EpubRenamer();
        app.exec(args[0]);
    }

    /**
     * @param topDir top dir
     */
    void exec(String topDir) throws IOException {
        Path dir = Paths.get(topDir);
        Files.walk(dir).filter(p -> p.getFileName().toString().endsWith(".epub")).forEach(epub -> {
            try {
                rename(epub);
            } catch (Throwable e) {
                System.err.println(e);
            }
        });
    }

    @Serdes(beanBinder = SaxonXPathBeanBinder.class)
    static class EpubContainer {
        @Element(value = "//*[local-name()='rootfile']/@full-path")
        String fillPath;
    }

    @Serdes(beanBinder = SaxonXPathBeanBinder.class)
    static class EpubPackage {
        @Element(value = "//*[local-name()='title']/text()")
        String title;
        @Element(value = "//*[local-name()='creator']/text()")
        String creator;
    }

    private void rename(Path epub) throws IOException {
        URI uri = URI.create("archive:" + epub.toUri());
Debug.println(Level.FINE, "open fs: " + uri);
        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
        Path root = fs.getRootDirectories().iterator().next();

        Path containerPath = root.resolve("META-INF/container.xml");
Debug.println(Level.FINE, "containerPath: " + containerPath + ", " + Files.exists(containerPath));
        EpubContainer container = new EpubContainer();
        Serdes.Util.deserialize(Files.newInputStream(containerPath), container);
Debug.println(Level.FINE, "container.fillPath: " + container.fillPath);

        Path packagePath = root.resolve(container.fillPath);
Debug.println(Level.FINE, "packagePath: " + packagePath + ", " + Files.exists(packagePath));
        EpubPackage package_ = new EpubPackage();
        Serdes.Util.deserialize(Files.newInputStream(packagePath), package_);

        String normalizedName = String.format("(一般小説) [%s] %s.epub", package_.creator, package_.title);
Debug.println("normalizedName: " + normalizedName);
        Files.move(epub, epub.getParent().resolve(normalizedName));

        fs.close();
    }
}
