/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.oauth2.amazon;

import java.awt.Dimension;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLInputElement;

import vavi.net.auth.oauth2.Authenticator;
import vavi.net.http.HttpServer;
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
 * AmazonAuthenticator.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/08/08 umjammer initial version <br>
 */
@PropsEntity(url = "file://${HOME}/.vavifuse/credentials.properties")
public class AmazonAuthenticator implements Authenticator {

    /** */
    private final String email;
    @Property(name = "amazon.password.{0}")
    private transient String password;
    /** */
    private final String clientId;
    /** */
    private final String scope;
    /** */
    private final String redirectUrl;
    /** */
    private transient String code;

    /** */
    public AmazonAuthenticator(String email, String clientId, String scope, String redirectUrl) throws IOException {
        this.email = email;
        this.clientId = clientId;
        this.scope = scope;
        this.redirectUrl = redirectUrl;

        PropsEntity.Util.bind(this, email);
    }

    /** */
    private CountDownLatch latch = new CountDownLatch(1);
    /** */
    private volatile Exception exception;

    /* @see Authenticator#get(java.lang.String) */
    @Override
    public String get(String url) throws IOException {

        exception = null;

        URL redirectUrl = new URL(this.redirectUrl);
        String host = redirectUrl.getHost();
        int port = redirectUrl.getPort();
        HttpServer httpServer = new HttpServer(host, port);
        httpServer.start();

        SwingUtilities.invokeLater(() -> { openUI(url); });

        try { latch.await(); } catch (InterruptedException e) { throw new IllegalStateException(e); }

        closeUI();

        httpServer.stop();

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
                        if (location.indexOf("code=") > 0) {
                            code = location.substring(location.indexOf("code=") + 5);
System.err.println("code: " + code);
                        } else {
                            exception = new IllegalArgumentException(location.substring(location.indexOf("error_description=") + 18));
                        }
                        latch.countDown();
                    }
                }
            }
        });
        try {
            webEngine.load(String.format(url, clientId, URLEncoder.encode(scope, "UTF-8"), URLEncoder.encode(redirectUrl, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
            assert false;
        }
    }
}

/* */
