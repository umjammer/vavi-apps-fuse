/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.condition.EnabledIf;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavi.xml.util.XPathDebugger;

import net.sf.saxon.dom.DOMNodeList;
import net.sf.saxon.dom.NodeOverNodeInfo;
import net.sf.saxon.om.NodeInfo;


/**
 * EpubManipulator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/28 umjammer initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
public class EpubManipulator {

    @Property(name = "epub.file")
    String file;

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    /**
     * @param args dir
     */
    public static void main(String[] args) throws Exception {
        EpubManipulator app = new EpubManipulator();
        PropsEntity.Util.bind(app);
        app.exec();
    }

    void exec() throws Exception {
//      Path src = Paths.get(Test1.class.getResource("/test.zip").toURI());

      Path dir = Paths.get("tmp");
//      Files.createDirectories(dir);

      Path target = dir.resolve(file);
//      Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
      URI uri = URI.create("jar:" + target.toUri());
Debug.println("uri: " + uri);

      FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
Debug.println("fs: " + fs.getClass().getName());

//        jsoup(fs);
//        jdom2(fs);
        saxon(fs);

        fs.close();
    }

    /**
     * @see "https://www.ipentec.com/document/internet-get-amazon-product-image"
     */
    static String amazon(String asin) {
        int countryCode = 9;
        String imageType = "LZZZZZZZ";
        return String.format("http://images-jp.amazon.com/images/P/%s.%02d.%s.jpg", asin, countryCode, imageType);
    }

    /**
     * TODO jsoup doesn't handle attribute namespace
     * @see "https://github.com/jhy/jsoup/pull/1682"
     */
    static void jsoup(FileSystem fs) throws IOException {

        Path conatiner = fs.getPath("META-INF/container.xml");
Debug.println(conatiner + ": " + Files.exists(conatiner));

        org.jsoup.nodes.Document document = Jsoup.parse(Files.newInputStream(conatiner), "utf8", conatiner.toUri().toString());
Debug.println("conatiner: " + document);
        Elements elements = document.select("container > rootfiles > rootfile[full-path]");
        org.jsoup.nodes.Element element = elements.first();
        String fullPath = element.attr("full-path");
Debug.println("full-path: " + fullPath);

        Path content = fs.getPath(fullPath);
        document = Jsoup.parse(Files.newInputStream(content), "utf8", conatiner.toUri().toString());
Debug.println("content: " + document);

        elements = document.select("dc|identifier[opfU00003Ascheme]");
Debug.println("elements: " + elements);
        element = elements.first();
        String asin = element.ownText();
Debug.println("asin: " + asin);
        String url = amazon(asin);
Debug.println("url: " + url);

        Path out = fs.getPath("vavi", asin + ".jpg");
        Files.copy(new URL(url).openStream(), out, StandardCopyOption.REPLACE_EXISTING);
Debug.println("out: " + Files.size(out));
    }

    /** */
    static void jdom2(FileSystem fs) throws IOException, JDOMException {

        Path conatiner = fs.getPath("META-INF/container.xml");
Debug.println(conatiner + ": " + Files.exists(conatiner));

        SAXBuilder builder = new SAXBuilder();
        org.jdom2.xpath.XPathFactory factory = org.jdom2.xpath.XPathFactory.instance();

XPathDebugger.getEntryList(new InputSource(Files.newInputStream(conatiner))).forEach(System.err::println);

        org.jdom2.Document document = builder.build(Files.newInputStream(conatiner));
Debug.println("conatiner: " + new XMLOutputter().outputString(document));

        org.jdom2.xpath.XPathExpression<org.jdom2.Element> expression =
                factory.compile("//*[local-name() = 'rootfile']", Filters.element());
        org.jdom2.Element element = expression.evaluateFirst(document);
Debug.println("element: " + element);
        String fullPath = element.getAttributeValue("full-path");
Debug.println("full-path: " + fullPath);

        Path content = fs.getPath(fullPath);
        document = builder.build(Files.newInputStream(content));
Debug.println("content: " + document);

        expression = factory.compile("//*[local-name() = 'identifier'][@*[local-name() = 'scheme']]", Filters.element());
        element = expression.evaluateFirst(document);
Debug.println("element: " + element);
        String asin = element.getText();
Debug.println("asin: " + asin);
        String url = amazon(asin);
Debug.println("url: " + url);

        Path out = fs.getPath("vavi", asin + ".jpg");
        Files.copy(new URL(url).openStream(), out, StandardCopyOption.REPLACE_EXISTING);
Debug.println("out: " + Files.size(out));
    }

    /** */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static void saxon(FileSystem fs) throws Exception {

        Path conatiner = fs.getPath("META-INF/container.xml");
Debug.println(conatiner + ": " + Files.exists(conatiner));

        System.setProperty(XPathFactory.DEFAULT_PROPERTY_NAME + ":" + XPathFactory.DEFAULT_OBJECT_MODEL_URI,
                           "net.sf.saxon.xpath.XPathFactoryImpl");
        XPath xpath = XPathFactory.newInstance().newXPath();

        InputSource is = new InputSource(Files.newInputStream(conatiner));
//XPathDebugger.getEntryList(is).forEach(System.err::println);

        List<NodeInfo> elements = (List) xpath.evaluate("//*[local-name() = 'rootfile']", is, XPathConstants.NODESET);
        Element element = (Element) NodeOverNodeInfo.wrap(elements.get(0));
Debug.println("element: " + element.getTagName());
        String fullPath = element.getAttribute("full-path");
Debug.println("full-path: " + fullPath);

        Path content = fs.getPath(fullPath);
        is = new InputSource(Files.newInputStream(content));
//Debug.println("content: " + content);

//        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "net.sf.saxon.dom.DocumentBuilderFactoryImpl");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(new InputSource(Files.newInputStream(content)));

        DOMNodeList elements2 = (DOMNodeList) xpath.evaluate("//*[local-name() = 'identifier'][@*[local-name() = 'scheme']]", document, XPathConstants.NODESET);
        element = (Element) elements2.item(0);
Debug.println("element: " + element.getTagName());
        String asin = element.getTextContent();
Debug.println("asin: " + asin);
        String url = amazon(asin);
Debug.println("url: " + url);

        Path outDir = fs.getPath("vavi");
        if (!Files.exists(outDir)) {
            Files.createDirectory(outDir);
        }
        Path out = outDir.resolve(asin + ".jpg");
        Files.copy(new URL(url).openStream(), out, StandardCopyOption.REPLACE_EXISTING);
Debug.println("out: " + Files.size(out));

        elements2 = (DOMNodeList) xpath.evaluate("//*[local-name() = 'manifest']/*[local-name() = 'item'][@*[local-name() = 'id']='cover']", document, XPathConstants.NODESET);
        element = (Element) elements2.item(0);
Debug.println("href:B: " + element.getAttribute("href"));
        element.setAttribute("href", out.toString());
Debug.println("href:A: " + element.getAttribute("href"));

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        DOMSource source = new DOMSource(document);
        OutputStream os = Files.newOutputStream(content, StandardOpenOption.TRUNCATE_EXISTING);
        StreamResult result = new StreamResult(os);
        transformer.transform(source, result);
        os.flush(); // needed
        os.close(); // needed
StreamResult result2 = new StreamResult(System.out);
transformer.transform(source, result2);

Debug.println("content: " + Files.size(content));
    }
}

/* */
