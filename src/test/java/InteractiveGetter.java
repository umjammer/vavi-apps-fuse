/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * InteractiveGetter. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/11 umjammer initial version <br>
 */
public class InteractiveGetter implements Getter {

    /* @see Getter#get(java.lang.String) */
    @Override
    public String get(String url) throws IOException {
        // TODO use processbuilder(chrome)
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(new URI(url));
            } catch (URISyntaxException e) {
                new IOException(e);
            }
        }
        
        System.out.println("Please open the following URL in your browser then type the autorization code:");
        System.out.println(" " + url);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String code = br.readLine();
        
        return code;
    }

}

/* */
