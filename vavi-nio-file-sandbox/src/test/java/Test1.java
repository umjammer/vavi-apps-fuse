/*
 * Copyright (c) 2016 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Test.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2016/02/27 umjammer initial version <br>
 */
public class Test1 {

    @Test
    public void test() {
        String location = "https://login.live.com/oauth20_authorize.srf?client_id=0000000040184284&scope=wl.offline_access&response_type=code&redirect_uri=https%3A%2F%2Fvast-plateau-97564.herokuapp.com%2Fonedrive_set";
        String url = "https://login.live.com/oauth20_authorize.srf";
        assertTrue(location.indexOf(url) >= 0);

        String location2 = "https://vast-plateau-97564.herokuapp.com/onedrive_set?code=M2739c1c0-460c-2ac5-8e94-f9b8fdf3dd5c";
        String redirectUrl = "https://vast-plateau-97564.herokuapp.com/onedrive_set";
        assertTrue(location2.indexOf(redirectUrl) >= 0);
    }

    @Test
    public void test2() {
        SecurityManager security = System.getSecurityManager();
        if (security == null) {
            System.err.println("no security manager");
            return;
        }
        try {
            security.checkPermission(new RuntimePermission("shutdownHooks"));
        } catch (final SecurityException e) {
            fail();
        }
    }

    @Test
    void test01() {
        System.out.printf("file: %1$s, %2$tF %2$tT.%2$tL, %3$d\n", "a", System.currentTimeMillis(), 1);
    }

    @Test
    void test04() {
        String m = "{\"@vavi\":" + 123456L + "}";
        long o = Long.parseLong(m.substring(9, m.length() - 1));
        assertEquals(123456L, o);
    }
}

/* */
