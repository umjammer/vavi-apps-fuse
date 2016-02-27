/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;


/**
 * Getter. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/11 umjammer initial version <br>
 */
public interface Getter {

    /**
     * @param url
     * @return code
     * @throws IOException
     */
    String get(String url) throws IOException;
}

/* */
