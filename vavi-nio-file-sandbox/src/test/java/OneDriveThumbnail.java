/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.condition.EnabledIf;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import vavi.awt.image.resample.FfmpegResampleOp;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static java.nio.file.FileVisitResult.CONTINUE;


/**
 * OneDriveThumbnail.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/03/13 umjammer initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
public class OneDriveThumbnail {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "onedrive.thumbnail.start")
    String start;

    static byte[] duke;

    static String string;
    /** isbn is ok also */
    static String asin;

    /**
     * @param args dir
     */
    public static void main(String[] args) throws Exception {

        OneDriveThumbnail app = new OneDriveThumbnail();
        PropsEntity.Util.bind(app);

        String email = System.getenv("MICROSOFT_TEST_ACCOUNT");
Debug.println("email: " + email);

        URI uri = URI.create("onedrive:///?id=" + email);
        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());

//        app.start = args[0];
        string = "一般小説";
        asin = "4048523112";

//        Files.createDirectories(Paths.get("tmp"));
        duke = Files.readAllBytes(Paths.get(OneDriveThumbnail.class.getResource("/duke.jpg").toURI()));

        Path dir = fs.getPath(app.start);
        Files.walkFileTree(dir, new MyFileVisitor());

        fs.close();
    }

    static class MyFileVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            if (attr.isRegularFile()) {
                try {
                    if (filter1(file)) {
                        func2(file);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return CONTINUE;
        }
    }

    // filters

    static final String ext = ".zip";
    static final Pattern pattern = Pattern.compile("^\\(一般小説\\)\\s\\[(.+?)\\]\\s(.+?)\\" + ext + "$");

    /** {@link #pattern}, {{@link #ext} */
    static boolean filter1(Path file) {
        String filename = file.getFileName().toString();
//System.err.println(filename);
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find() && filename.contains(string)) {
System.out.println(matcher.group(1) + " - " + matcher.group(2));
            return true;
        } else {
            return false;
        }
    }

    // functions

    /** get thumbnail of the file and save it to local */
    static void func1(Path file) throws IOException {
        byte[] bytes = (byte[]) Files.getAttribute(file, "user:thumbnail");
        if (bytes != null && bytes.length != 0) {
            String name = file.getFileName().toString().replace(".zip", ".jpg");
System.err.println("output: " + name);
            Path path = Paths.get("tmp", name);
            Files.write(path, bytes);
        } else {
System.err.println("skip: " + file);
        }
    }


    static final boolean DRY_RUN = false;
    static final boolean OVERWRITE = true;

//    static final Pattern jpg = Pattern.compile("^.+?\\.jpg$");
    static final Pattern jpg = Pattern.compile("^(.+\\/)*.+\\.jpe?g$");

    /** extract self and set first jpg as a thumbnail */
    static void func2(Path file) throws IOException {
        // check existence
        byte[] bytes = (byte[]) Files.getAttribute(file, "user:thumbnail");
        if (!OVERWRITE && bytes != null && bytes.length != 0) {
System.err.println("skip: " + file);
            return;
        }

        // exec
        List<String> names = new ArrayList<>();
        ZipEntry entry;
        boolean sjis = false;
        try {
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(file)));
            while ((entry = zis.getNextEntry()) != null) {
                Matcher m = jpg.matcher(entry.getName());
                if (m.matches()) {
                    names.add(entry.getName());
                }
            }
            zis.close();
        } catch (IllegalArgumentException e) {
Debug.println(e);
            sjis = true;
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(file)), Charset.forName("MS932"));
            while ((entry = zis.getNextEntry()) != null) {
                Matcher m = jpg.matcher(entry.getName());
                if (m.matches()) {
                    names.add(entry.getName());
                }
            }
            zis.close();
        }

        // determine cover
        Collections.sort(names, (a, b) -> {
            if (a.contains("表紙") && !b.contains("表紙")) {
                return -1;
            } else if (!a.contains("表紙") && b.contains("表紙")) {
                return 1;
            } else if (a.contains("cover") && !b.contains("cover")) {
                return -1;
            } else if (!a.contains("cover") && b.contains("cover")) {
                return 1;
            } else {
                return a.compareTo(b);
            }
        });
        if (names.size() == 0) {
ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(file)), Charset.forName(sjis ? "MS932" : "UTF-8"));
while ((entry = zis.getNextEntry()) != null) {
 System.err.println(entry.getName());
}
zis.close();
Debug.print(Level.WARNING, "no images in: " + file);
            return;
        }
        String name = names.get(0);

        // extract image
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(file)), Charset.forName(sjis ? "MS932" : "UTF-8"));

        BufferedImage image = null;
        while ((entry = zis.getNextEntry()) != null) {
            if (name.equals(entry.getName())) {
                image = ImageIO.read(zis);
                break;
            }
        }
        zis.close();

        // resize image
        double sx = 600d / image.getWidth();
        BufferedImage thumbnail = new FfmpegResampleOp(sx, sx).filter(image, null);
System.err.println(entry.getName() + ": " + thumbnail.getWidth() + "x" + thumbnail.getHeight());

        // set image
        if (DRY_RUN) {
            Path path = Paths.get("tmp", file.getFileName().toString().replace(ext, ".jpg"));
            ImageIO.write(thumbnail, "JPG", Files.newOutputStream(path));
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "JPG", baos);
            bytes = baos.toByteArray();
            Files.setAttribute(file, "user:thumbnail", bytes);
        }
    }

    /**
     * set amazon thumbnail asin from self meta data
     * GoogleDrive.app needed
     */
    static void func3(Path file) throws Exception {
        // check existence
        byte[] bytes = (byte[]) Files.getAttribute(file, "user:thumbnail");
        if (!OVERWRITE && bytes != null && bytes.length != 0) {
System.err.println("skip: " + file);
            return;
        }

        // exec
        // convert path from google drive fs to default fs
        // because "zipfs" dosn't accept googledrive as sub scheme
        Path gd = Paths.get("/Volumes/GoogleDrive/My Drive", file.toString());
        URI uri = URI.create("jar:" + gd.toUri().toString());
Debug.println("uri: " + uri);

        FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
Debug.println("fs: " + fs.getClass().getName());


        Path conatiner = fs.getPath("META-INF/container.xml");
Debug.println(conatiner + ": " + Files.exists(conatiner));

        XPath xpath = XPathFactory.newInstance().newXPath();

        InputSource is = new InputSource(Files.newInputStream(conatiner));

        NodeList elements = (NodeList) xpath.evaluate("//*[local-name() = 'rootfile']", is, XPathConstants.NODESET);
        Element element = (Element) elements.item(0);
Debug.println("element: " + element.getTagName());
        String fullPath = element.getAttribute("full-path");
Debug.println("full-path: " + fullPath);

        Path content = fs.getPath(fullPath);
        is = new InputSource(Files.newInputStream(content));

        is = new InputSource(Files.newInputStream(content));

        elements = (NodeList) xpath.evaluate("//*[local-name() = 'identifier'][@*[local-name() = 'scheme']]", is, XPathConstants.NODESET);
        element = (Element) elements.item(0);
Debug.println("element: " + element.getTagName());
        String asin = element.getTextContent();
Debug.println("asin: " + asin);
        String url = GoogleDriveThumbnail.amazon(asin);
Debug.println("url: " + url);


        InputStream in = new BufferedInputStream(new URL(url).openStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8024];
        int l = 0;
        while ((l = in.read(buffer)) != -1) {
            baos.write(buffer, 0, l);
        }
        bytes = baos.toByteArray();
Debug.println("image: " + bytes.length);
        if (bytes.length < 100) {
            throw new IllegalStateException("no image in amazon? for: " + asin);
        }


        if (DRY_RUN) {
            Path path = Paths.get("tmp", file.getFileName().toString().replace(ext, ".jpg"));
            Files.write(path, bytes);
        } else {
            Files.setAttribute(file, "user:thumbnail", bytes);
        }
    }

    /** set amazon thumbnail specify asin directly */
    static void func4(Path file) throws Exception {
        // check existence
        byte[] bytes = (byte[]) Files.getAttribute(file, "user:thumbnail");
        if (!OVERWRITE && bytes != null && bytes.length != 0) {
System.err.println("skip: " + file);
            return;
        }

        // exec
Debug.println("asin: " + asin);
        String url = GoogleDriveThumbnail.amazon(asin);
Debug.println("url: " + url);


        InputStream in = new BufferedInputStream(new URL(url).openStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8024];
        int l = 0;
        while ((l = in.read(buffer)) != -1) {
            baos.write(buffer, 0, l);
        }
        bytes = baos.toByteArray();
Debug.println("image: " + bytes.length);
        if (bytes.length < 100) {
            throw new IllegalStateException("no image in amazon? for: " + asin);
        }


        if (DRY_RUN) {
            Path path = Paths.get("tmp", file.getFileName().toString().replace(ext, ".jpg"));
            Files.write(path, bytes);
        } else {
            Files.setAttribute(file, "user:thumbnail", bytes);
        }
    }

    /** set local thumbnail same file name */
    static void func5(Path file) throws Exception {
        // check existence
        byte[] bytes = (byte[]) Files.getAttribute(file, "user:thumbnail");
        if (!OVERWRITE && bytes != null && bytes.length != 0) {
System.err.println("skip: " + file);
            return;
        }

        // exec
        Path path = Paths.get("tmp", file.getFileName().toString().replace(ext, ".jpg"));
Debug.println("jpeg: " + path);

        InputStream in = new BufferedInputStream(Files.newInputStream(path));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8024];
        int l = 0;
        while ((l = in.read(buffer)) != -1) {
            baos.write(buffer, 0, l);
        }
        bytes = baos.toByteArray();
Debug.println("image: " + bytes.length);
        if (bytes.length < 100) {
            throw new IllegalStateException("no image in amazon? for: " + asin);
        }

        Files.setAttribute(file, "user:thumbnail", bytes);
    }
}

/* */
