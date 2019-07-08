/*
 * Copyright (c) 2018 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.oauth2.microsoft;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import vavi.net.auth.oauth2.AuthUI;
import vavi.net.auth.totp.PinGenerator;
import vavi.util.Debug;


/**
 * SeleniumAuthUI.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/06/27 umjammer initial version <br>
 */
public class SeleniumAuthUI implements AuthUI<String> {

    private String email;
    private String password;
    private String url;
    private String redirectUrl;
    private String totpSecret;

    /** */
    SeleniumAuthUI(String email, String password, String url, String redirectUrl, String totpSecret) {
        this.email = email;
        this.password = password;
        this.url = url;
        this.redirectUrl = redirectUrl;
        this.totpSecret = totpSecret;
Debug.println("totpSecret: " + totpSecret);
    }

    /** */
    private CountDownLatch latch = new CountDownLatch(1);

    /** */
    private transient String code;
    /** */
    private volatile Exception exception;

    /* @see vavi.net.auth.oauth2.AuthUI#auth() */
    @Override
    public void auth() {
        SwingUtilities.invokeLater(() -> { openUI(url); });

        try { latch.await(); } catch (InterruptedException e) { throw new IllegalStateException(e); }

        closeUI();
    }

    private WebDriver driver;

    /** Create a JFrame with a JButton and a JFXPanel containing the WebView. */
    private void openUI(String url) {
        String pwd = System.getProperty("user.dir");
        System.setProperty("webdriver.chrome.driver", pwd + "/bin/chromedriver");
        System.setProperty("webdriver.chrome.verboseLogging", "false");

        ChromeOptions chromeOptions = new ChromeOptions();
        String app = System.getProperty("com.google.chrome.app", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome");
        chromeOptions.setBinary(app);
//        chromeOptions.addArguments("--window-size=240x320");

        driver = new ChromeDriver(chromeOptions);
        driver.manage().window().setSize(new Dimension(480, 640));

        process(url);
    }

    /** */
    private void closeUI() {
        driver.quit();
    }

    /** Creates a WebView and fires up */
    private void process(String url) {
        WebDriverWait wait = new WebDriverWait(driver, 10);
        driver.get(url);

        Deque<String> tasks = new ArrayDeque<>();

        boolean login = false;
        while (!login) {
            try {
                wait.until(d -> {
                    if (d == null) {
                        throw new IllegalStateException("browser maight be closed");
                    }
                    String r = ((JavascriptExecutor) d).executeScript("return document.readyState;").toString();
//Debug.println(r);
                    return "complete".equals(r);
                });
                String location = driver.getCurrentUrl();
//Debug.println("location: " + location);
                if (location.indexOf("oauth20_authorize") > -1 ||
                    location.indexOf("microsoft") > -1 ||
                    location.indexOf("login.live.com") > -1) {
                    try {
                        WebElement element = driver.findElement(By.className("form-control"));
                        sleep();
//Debug.println("element: name = " + element.getTagName() + ", class = " + element.getAttribute("class") + ", id = " + element.getAttribute("id") + ", type = " + element.getAttribute("type"));
                        if (!tasks.contains("email") && "email".equals(element.getAttribute("type"))) {
                            element.sendKeys(email);
                            tasks.add("email");
                        } else if (!tasks.contains("password") && "password".equals(element.getAttribute("type"))) {
                            if (password != null) {
                                element.sendKeys(password);
                                tasks.add("password");
                            } else {
Debug.println("no password");
                                continue;
                            }
                        } else if (!tasks.contains("totp") && "tel".equals(element.getAttribute("type"))) {
                            if (totpSecret != null) {
Debug.println("here");
                                String pin = PinGenerator.computePin(totpSecret, null);
Debug.println("pin: " + pin);
                                element.sendKeys(pin);
                                tasks.add("totp");
                            } else {
Debug.println("no pin");
                                continue;
                            }
                        } else {
                            continue;
                        }
                        new Actions(driver).moveToElement(driver.findElement(By.className("btn-primary"))).click().build().perform();
Debug.println("set " + tasks.peekLast());
                        sleep();
                    } catch (org.openqa.selenium.NoSuchElementException e) {
                        WebElement element = driver.findElement(By.className("btn-primary"));
                        if (!tasks.contains("accept") && "ucaccept".equals(element.getAttribute("name"))) {
                            new Actions(driver).moveToElement(element).click().build().perform();
                            tasks.add("accept");
Debug.println("set " + tasks.peekLast());
                            sleep();
                        } else {
e.printStackTrace();
                            if (exception == null) {
                                exception = new IllegalStateException(e);
                            } else {
                                exception.addSuppressed(e);
                            }
                        }
                    } catch (org.openqa.selenium.StaleElementReferenceException e) {
Debug.println(Level.WARNING, e.getMessage());
                    }
                } else if (location.indexOf(redirectUrl) > -1) {
//                    code = location.substring(location.indexOf("code=") + "code=".length(), location.indexOf("&"));
                    code = location;
Debug.println("code: " + code);
                    login = true;
                }
            } catch (Exception e) {
e.printStackTrace();
                if (exception == null) {
                    exception = new IllegalStateException(e);
                } else {
                    exception.addSuppressed(e);
                }
            }
        }

        latch.countDown();
    }

    /** */
    private static void sleep() {
        try { Thread.sleep(300); } catch (InterruptedException e) {}
    }

    /* @see vavi.net.auth.oauth2.AuthUI#getResult() */
    @Override
    public String getResult() {
        return code;
    }

    /* @see vavi.net.auth.oauth2.AuthUI#getException() */
    @Override
    public Exception getException() {
        return exception;
    }

    /** */
    protected void finalize() {
        driver.quit();
    }
}

/* */
