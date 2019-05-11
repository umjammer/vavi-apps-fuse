/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.onedrive;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import vavi.net.fuse.Getter;


/**
 * PlainGetter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/11 umjammer initial version <br>
 */
public class PlainGetter implements Getter {

    /* @see Getter#get(java.lang.String) */
    @Override
    public String get(String url) throws IOException {
        HttpURLConnection conn = HttpURLConnection.class.cast(new URL(url).openConnection());
        conn.setInstanceFollowRedirects(true);
        InputStream is = conn.getInputStream();
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[1024];
        while (is.available() > 0) {
            int r = is.read(buf, 0, buf.length);
            if (r < 0) {
                break;
            }
            sb.append(new String(buf));
        }
        System.err.println(sb.toString());

        return null;
    }
}

/* */
