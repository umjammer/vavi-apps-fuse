/*
 * Copyright (c) 2018 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.auth.oauth2;


/**
 * AuthUI.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2018/11/23 umjammer initial version <br>
 */
public interface AuthUI<T> {

    /** */
    void auth();

    /** */
    T getResult();

    /** */
    Exception getException();
}

/* */
