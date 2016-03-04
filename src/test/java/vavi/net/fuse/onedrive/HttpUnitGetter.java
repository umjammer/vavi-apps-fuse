/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.fuse.onedrive;

import java.io.IOException;

import org.xml.sax.SAXException;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import vavi.net.fuse.Getter;


/**
 * HttpUnitGetter. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/13 umjammer initial version <br>
 */
public class HttpUnitGetter implements Getter {

    /* @see Getter#get(java.lang.String) */
    @Override
    public String get(String url) throws IOException {
        try {
            WebConversation wc = new WebConversation();
            WebResponse wr = wc.getResponse(url);
            System.err.println(wr.getText());
        } catch (SAXException e) {
            throw new IOException(e);
        }
        
        return null;
    }
}

/* */
