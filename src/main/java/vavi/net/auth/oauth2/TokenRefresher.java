/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.oauth2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;

import vavi.util.Debug;


/**
 * TokenRefresher.
 *
 * TODO out source file (dao?)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/07/03 umjammer initial version <br>
 */
public class TokenRefresher {

    /** */
    private ExecutorService refreshThread;

    /** */
    private boolean keepRefreshing;

    /** for refreshToken */
    private Path file;

    /** */
    private Supplier<Long> refresh;

    /**
     * @param refresh you should call {@link #writeRefreshToken(String)} and return new refresh delay.
     */
    public TokenRefresher(Path file, Supplier<Long> refresh) {
        this.file = file;
        this.refresh = refresh;
    }

    /** */
    public void start(String refreshToken, long refreshDelay) throws IOException {
        writeRefreshToken(refreshToken);
        keepRefreshing = true;
        startRefreshThread(refreshDelay);
    }

    /** */
    private void startRefreshThread(long refreshDelay) {
        if (this.refreshThread == null) {
            this.refreshThread = Executors.newSingleThreadExecutor();
            this.refreshThread.submit(() -> {
                // initial delay
                try { Thread.sleep(refreshDelay); } catch (InterruptedException e) {}
                // continuously refresh thread
                while (keepRefreshing) {
Debug.println("refreshing session");
                    try {
                        refresh.get(); // TODO update refresh delay
                        Thread.sleep(refreshDelay);
                    } catch (Exception e) {
Debug.println(Level.WARNING, "failed to refresh session - attempting recovery");
                        long retryTime = System.currentTimeMillis() + 1000 * 30;
                        while (System.currentTimeMillis() <= retryTime && keepRefreshing) {
                            try {
                                refresh.get();
                            } catch (Exception e1) {
Debug.println("failed to refresh session - attempting retrying");
                                try { Thread.sleep(500); } catch (InterruptedException e2) {}
                            }
                        }
Debug.println(Level.SEVERE, "could not refresh session");
                        e.printStackTrace();
                        // back off after error
                        try { Thread.sleep(10000); } catch (Exception e1) {}
                    }
                }
            });
Debug.println("starting refresh thread");
        }
    }

    /** */
    public void terminate() {
        if (this.refreshThread != null) {
            keepRefreshing = false;
            refreshThread.shutdownNow();
Debug.println("stopping refresh thread");
        }
    }

    /** */
    public void writeRefreshToken(String refreshToken) throws IOException {
        if (!Files.exists(file.getParent())) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, refreshToken.getBytes());
Debug.println("refreshToken: " + refreshToken);
    }

    /**
     * @return null when the data does not exist.
     */
    public String readRefreshToken() throws IOException {
        String refreshToken = Files.exists(file) ? new String(Files.readAllBytes(file)) : null;
        return refreshToken;
    }
}

/* */
