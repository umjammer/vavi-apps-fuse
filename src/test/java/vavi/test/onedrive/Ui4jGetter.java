/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.test.onedrive;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.HttpCookie;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.BrowserFactory;
import com.ui4j.api.browser.Page;
import com.ui4j.api.browser.PageConfiguration;
import com.ui4j.api.interceptor.Interceptor;
import com.ui4j.api.interceptor.Request;
import com.ui4j.api.interceptor.Response;

import vavi.test.Getter;


/**
 * Ui4jGetter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/11 umjammer initial version <br>
 * @see "https://github.com/webfolderio/ui4j"
 */
public class Ui4jGetter implements Getter {

    /* @see Getter#get() */
    @Override
    public String get(String url) throws IOException {
        System.setProperty("ui4j.headless", "true");

        BrowserEngine engine = BrowserFactory.getWebKit();
        CountDownLatch latch = new CountDownLatch(1);
        CookieHandler.setDefault(new com.ui4j.webkit.WebKitIsolatedCookieHandler());
        Interceptor interceptor = new Interceptor() {
            List<HttpCookie> cookies;
            @Override
            public void beforeLoad(Request request) {
                System.err.println("beforeLoad: " + request.getUrl());
                if (cookies != null) {
                    request.setCookies(cookies);
                }
                request.setCookie(new HttpCookie("name", "value"));
            }
            @Override
            public void afterLoad(Response response) {
                System.out.println(response.getHeaders());
                try {
                    cookies = response.getCookies();
                } catch (IllegalArgumentException e) {
                    System.err.println("ERROR: " + e.getMessage());
                    cookies = null;
                }
                System.err.println("afterLoad: " + response.getUrl());
                if (response.getUrl().startsWith("https://vast-plateau-97564.herokuapp.com")) {
                    latch.countDown();
                }
            }
        };
        PageConfiguration configuration = new PageConfiguration(interceptor);
        configuration.setUserAgent("Mozilla/5.0 (Windows NT 6.3; Win64; x64; rv:43.0) Gecko/20100101 Firefox/43.0");
        Page page = engine.navigate(url, configuration);
        System.err.println("---- ---- ---- ----");
        page.getDocument().queryAll("script").forEach(e -> {
            System.err.println(e.getText().get());
            page.executeScript(e.getText().get());
        });
        System.err.println("---- ---- ---- ----");
        page.getDocument().queryAll("html").forEach(e -> {
            System.err.println(e.getText().get());
        });

        try {
            System.err.println("wait until redirect");
            latch.await();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }


        page.close();
        BrowserFactory.getWebKit().shutdown();

        return "ACCESS_TOKEN";
    }
}

/* */
