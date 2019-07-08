/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.test.googledrive;

import java.awt.Dimension;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.html.HTMLInputElement;

import vavi.net.auth.totp.PinGenerator;
import vavi.test.Getter;
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
 * GoogleDriveFxGetter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/03/01 umjammer initial version <br>
 */
@PropsEntity(url = "file://${HOME}/.vavifuse/credentials.properties")
public class GoogleDriveFxGetter implements Getter {

    private final String email;
    @Property(name = "googledrive.password.{0}")
    private transient String password;
    @Property(name = "googledrive.totpSecret.{0}")
    private transient String totpSecret;
    private transient String code;

    public GoogleDriveFxGetter(String email) {
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

//Thread.getAllStackTraces().keySet().forEach(System.err::println);

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

                    if (location.startsWith("https://accounts.google.com/ServiceLogin")) {

                        if (!login) {
                            Document doc = webEngine.getDocument();

                            try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(System.err); }

                            ((HTMLInputElement) doc.getElementById("Email")).setValue(email);
System.err.println("set email: " + email);

                            try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(System.err); }

                            ((HTMLInputElement) doc.getElementById("next")).click();
System.err.println("next");

                            try { Thread.sleep(2000); } catch (InterruptedException e) { e.printStackTrace(System.err); }

System.err.println(webEngine.executeScript("document.documentElement.outerHTML"));

//                            ((HTMLInputElement) doc.getElementById("password")).setValue(password);
//System.err.println("set passwd: " + password);

//                            ((HTMLInputElement) doc.getElementById("signIn")).click();
//System.err.println("signin");

                            login = true;

                        } else {
                            exception = new IllegalArgumentException("wrong email or password");
                            latch.countDown();
                        }
                    } else if (location.startsWith("https://accounts.google.com/signin/challenge/totp")) {
//System.err.println(webEngine.executeScript("document.documentElement.outerHTML"));
                        //
                        if (totpSecret != null && !totpSecret.isEmpty()) {
                            String pin = PinGenerator.computePin(totpSecret, null);
                            Document doc = webEngine.getDocument();

                            try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(System.err); }

                            ((HTMLInputElement) doc.getElementById("totpPin")).setValue(pin);
System.err.println("pin: " + pin);

                            try { Thread.sleep(50); } catch (InterruptedException e) { e.printStackTrace(System.err); }

                            ((HTMLInputElement) doc.getElementById("submit")).click();
System.err.println("2 step authentication");
                        } else {
System.err.println("no totp secret, enter by yourself");
                        }

                    } else if (location.startsWith("https://accounts.google.com/o/oauth2/auth")) {
System.err.println(webEngine.executeScript("document.documentElement.outerHTML"));

//                        Document doc = webEngine.getDocument();
//                        ((HTMLFormElement) doc.getElementById("connect-approve")).submit();
System.err.println("accept");

                    } else if (location.startsWith("https://accounts.google.com/o/oauth2/approval")) {
//System.err.println(webEngine.executeScript("document.documentElement.outerHTML"));

                        Document doc = webEngine.getDocument();
                        code = ((HTMLInputElement) doc.getElementById("code")).getAttribute("value");
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
