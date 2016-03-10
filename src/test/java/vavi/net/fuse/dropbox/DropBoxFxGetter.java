/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.dropbox;

import java.awt.Dimension;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLInputElement;

import vavi.net.fuse.Getter;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;


/**
 * DropBoxFxGetter. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/02 umjammer initial version <br>
 */
@PropsEntity(url = "file://${HOME}/.vavifuse/credentials.properties")
public class DropBoxFxGetter implements Getter {

    private final String email;
    @Property(name = "dropbox.password.{0}")
    private transient String password;
    private transient String code;
    
    public DropBoxFxGetter(String email) {
        this.email = email;

        try {
            PropsEntity.Util.bind(this, email);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private CountDownLatch latch = new CountDownLatch(1);
    private volatile Exception exception;

    /* @see Getter#get(java.lang.String) */
    @Override
    public String get(String url) throws IOException {
System.err.println(url);

        exception = null;

        System.setProperty("http.proxyHost","127.0.0.1"); // TODO
        System.setProperty("http.proxyPort","8888");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initAndShowGUI(url);
            }
        });
        
        try {
            System.err.println("wait until sign in...");
            latch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        frame.setVisible(false);
        frame.dispose();
        
        System.setProperty("http.proxyHost","");
        System.setProperty("http.proxyPort","");
        
        if (exception != null) {
            throw new IllegalStateException(exception);
        }

        return code;
    }

    private JFrame frame;

    /** Create a JFrame with a JButton and a JFXPanel containing the WebView. */
    private void initAndShowGUI(String url) {
        // This method is invoked on Swing thread
        frame = new JFrame("Don't touch me.");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(null); // do the layout manually

        final JFXPanel fxPanel = new JFXPanel();

        frame.add(fxPanel);
        frame.setVisible(true);

        fxPanel.setSize(new Dimension(480, 640));

        frame.getContentPane().setPreferredSize(new Dimension(480, 640));
        frame.pack();

        Platform.runLater(new Runnable() { // this will run initFX as JavaFX-Thread
            @Override
            public void run() {
                initFX(fxPanel, url);
            }
        });
    }

    /** Creates a WebView and fires up */
    private void initFX(JFXPanel fxPanel, String url) {
        Group group = new Group();
        Scene scene = new Scene(group);
        fxPanel.setScene(scene);

        WebView webView = new WebView();

        group.getChildren().add(webView);
        webView.setMinSize(480, 640);
        webView.setMaxSize(480, 640);

        // Obtain the webEngine to navigate
        WebEngine webEngine = webView.getEngine();
        webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
            volatile boolean login = false;
            public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
                if (newState == State.SUCCEEDED) {
                    String location = webEngine.getLocation();
                    System.err.println("location: " + location);

                    if (location.startsWith(url)) {
                        
                        if (!login) { 
                            Document doc = webEngine.getDocument();
System.err.println(webEngine.executeScript("document.documentElement.outerHTML"));
        
                            try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(System.err); }

                            ((HTMLInputElement) doc.getElementById("pyxl4325")).setValue(email);
System.err.println("set email: " + email);
                            
                            ((HTMLInputElement) doc.getElementById("pyxl4328")).setValue(password);
System.err.println("set passwd: " + password);

//                            ((HTMLInputElement) doc.getElementById("????")).click();
//System.err.println("signin");
                            
                            login = true;

                        } else {
                            exception = new IllegalArgumentException("wrong email or password");
                            latch.countDown();
                        }
                    } else if (location.startsWith("https://www.dropbox.com/1/oauth2/authorize?")) {
//System.err.println(webEngine.executeScript("document.documentElement.outerHTML"));
//                        Document doc = webEngine.getDocument();
//                        ((HTMLButtonElement) doc.getElementById("submit_approve_access")).click();
System.err.println("accept");
                    } else if (location.startsWith("https://www.dropbox.com/1/oauth2/authorize_submit")) {
System.err.println(webEngine.executeScript("document.documentElement.outerHTML"));

                        Document doc = webEngine.getDocument();
                        NodeList inputs = doc.getElementsByTagName("INPUT");
                        for (int i = 0; i < inputs.getLength(); i++) {
                            Node input = inputs.item(i);
System.err.println("input: " + ((Element) input).getAttribute("type")); // == text
                        }
                        code = ((HTMLInputElement) doc.getElementById("code")).getAttribute("data-token");
System.err.println("code: " + code);
                        latch.countDown();
                    }
                }

System.err.println(ov + ", " + newState + ", " + oldState);
            }
        });
        webEngine.load(url);
    }
}

/* */
