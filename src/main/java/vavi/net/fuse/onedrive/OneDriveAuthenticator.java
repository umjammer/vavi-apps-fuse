/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.onedrive;

import java.awt.Dimension;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLInputElement;

import vavi.net.fuse.Authenticator;

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
 * OneDriveAuthenticator. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/16 umjammer initial version <br>
 */
public class OneDriveAuthenticator implements Authenticator {

    /** */
    private final String email;
    /** */
    private transient final String password;
    /** */
    private final String redirectUrl;
    /** */
    private transient String code;
    
    /** */
    public OneDriveAuthenticator(String email, String password, String redirectUrl) {
        this.email = email;
        this.password = password;
        this.redirectUrl = redirectUrl;
    }
    
    /** */
    private CountDownLatch latch = new CountDownLatch(1);
    /** */
    private volatile Exception exception;

    /* @see Getter#get(java.lang.String) */
    @Override
    public String get(String url) throws IOException {

        exception = null;
        
        SwingUtilities.invokeLater(() -> { openUI(url); });
        
        try { latch.await(); } catch (InterruptedException e) { throw new IllegalStateException(e); }

        closeUI();
        
        if (exception != null) {
            throw new IllegalStateException(exception);
        }

        return code;
    }

    private JFrame frame;

    /** Create a JFrame with a JButton and a JFXPanel containing the WebView. */
    private void openUI(String url) {
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

        Platform.runLater(() -> { initFX(fxPanel, url); });
    }

    /** */
    private void closeUI() {
        frame.setVisible(false);
        frame.dispose();
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

                    if (location.indexOf(url) > -1) {
                        
                        if (!login) { 
                            System.err.println("set email: " + email);
                            Document doc = webEngine.getDocument();

                            NodeList inputs = doc.getElementsByTagName("INPUT");
    
                            ((HTMLInputElement) inputs.item(0)).setValue(email);
System.err.println("set email: " + email);
                            ((HTMLInputElement) inputs.item(1)).setValue(password);
System.err.println("set passwd: " + password);
                            ((Element) inputs.item(2)).setAttribute("checked", "true");
System.err.println("set checked: " + true);

                            ((HTMLInputElement) inputs.item(3)).click();
System.err.println("submit");
                            
                            login = true;
                        } else {
                            exception = new IllegalArgumentException("wrong email or password");
                            latch.countDown();
                        }
                    } else if (location.indexOf(redirectUrl) > -1) {
                        code = location.substring(location.indexOf("code=") + 5);
System.err.println("code: " + code);
                        latch.countDown();
                    }
                }
            }
        });
        webEngine.load(url);
    }
}

/* */
